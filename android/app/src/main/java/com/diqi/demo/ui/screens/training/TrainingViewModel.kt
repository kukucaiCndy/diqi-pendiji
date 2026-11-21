package com.diqi.demo.ui.screens.training

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diqi.demo.training.TrainingDeps
import com.diqi.demo.data.db.SensorMode
import com.diqi.demo.data.repo.SessionDraft
import com.diqi.demo.domain.training.EngineSnapshot
import com.diqi.demo.domain.training.TrainingEngine
import com.diqi.demo.domain.training.TrainingFrame
import com.diqi.demo.domain.training.TrainingParams
import com.diqi.demo.data.db.MissingReason
import com.diqi.demo.domain.quality.QualityGate
import com.diqi.demo.domain.training.TrainingStatus
import com.diqi.demo.sensor.DemoSensor
import com.diqi.demo.sensor.NullSensor
import com.diqi.demo.sensor.RppgSensor
import com.diqi.demo.sensor.SensorAdapter
import com.diqi.demo.sensor.SensorKind
import com.diqi.demo.sensor.camera.FaceAnalyzer
import com.diqi.demo.sensor.camera.FaceFrame
import com.diqi.demo.sensor.chooseSensorKind
import com.diqi.demo.training.RppgSensorAssembly
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/** 训练页 UI 状态。 */
/**
 * 训练页的**低频**状态。
 *
 * 刻意**不含** `frame` 与 `metric` —— 它们每秒变一次，若放在同一个 StateFlow 里，
 * 整个页面每秒重组一次。实测：运行中 40 帧/20s、22 帧慢（p90 61ms），
 * 而暂停后不推帧时 **0 帧 0 慢**，说明掉帧 100% 来自"推帧导致整页重组"。
 *
 * 所以拆成三条流：
 * - [TrainingViewModel.meta]  低频（进出对话框、开关变化）
 * - [TrainingViewModel.frames] 每秒（只由 [TickingSection] 订阅）
 * - [TrainingViewModel.metric] 每秒（只由指标区订阅）
 */
data class TrainingUiState(
    val params: TrainingParams = TrainingParams.DEFAULT,
    val paused: Boolean = false,
    /** 进入页面时发现的未完成训练（AC-13）*/
    val pendingRecovery: EngineSnapshot? = null,
    val showStopConfirm: Boolean = false,
    val finished: Boolean = false,
    val savedSessionId: String? = null,
    /** FR-01 分流：训练页常驻就医提示条 */
    val clinicalNotice: Boolean = false,
    val voiceAvailable: Boolean = true,
    /** 保存失败（例如磁盘满）。UI 必须提示，不能静默。 */
    val saveFailed: Boolean = false,
    /** 演示模式（FR-14）：为 true 时指标区必须渲染全屏水印 */
    val demoMode: Boolean = false,
    /** 真实传感器是否就绪（cameraEnabled + appKey + bridge 初始化成功）。
     * 为 true 时 Screen 渲染真实相机预览；为 false 时维持占位渲染。 */
    val isRealSensor: Boolean = false,
    /** 相机错误文案（照 CalibrateViewModel.cameraError）；null 表示无错误 */
    val cameraError: String? = null,
    /** 当前是否检测到人脸（用于触发 AE/AWB 锁定）。低频：仅在出现/消失时变化。 */
    val faceFound: Boolean = false,
)

/**
 * 训练页 ViewModel：**协调器唯一的 UI 接入点**。
 *
 * ## 三个容易出错的地方，都在这里守住
 *
 * 1. **会话 id 在开始时生成并持有**（评审 D4）：`SessionDraft.id` 故意没有默认值，
 *    所以"连点两次完成只产生一条记录"（AC-08）在数据层成立的前提是
 *    这里只生成一次 id。若每次点保存都 new 一个 UUID，AC-08 直接失效。
 * 2. **保存只做一次**：自然完成与用户点结束都会走到 [finishSession]，
 *    用 `savedSessionId` 做闸门，避免两条记录。
 * 3. **中断恢复**：进入页面先查快照；有就弹「继续 / 丢弃」，
 *    用户选继续则用快照恢复，选丢弃则清掉快照从零开始（AC-13）。
 */
class TrainingViewModel(
    private val container: TrainingDeps,
    private val cameraEnabled: Boolean = false,
    private val params: TrainingParams = TrainingParams.DEFAULT,
) : ViewModel() {

    private val sessionId: String = UUID.randomUUID().toString()

    /**
     * 训练会话来自**容器**（application-scoped），不是 ViewModel 自有。
     *
     * 这样锁屏/重建时引擎状态不丢，AC-05「进度误差 ≤1s」才成立。
     */
    private val coordinator: com.diqi.demo.training.TrainingSession = container.obtainSession(params)

    private val _state = MutableStateFlow(TrainingUiState(params = params))
    val state: StateFlow<TrainingUiState> = _state.asStateFlow()

    /** 每秒变化的帧。只应被"倒计时/圆环/时间轴"这类叶子订阅。 */
    private val _frames = MutableStateFlow(TrainingFrame.idle(params.repsPerSet))
    val frames: StateFlow<TrainingFrame> = _frames.asStateFlow()

    /** 每秒变化的指标区状态。只应被指标区叶子订阅。 */
    private val _metric = MutableStateFlow<MetricUiState>(MetricUiState.NotEnabled)
    val metric: StateFlow<MetricUiState> = _metric.asStateFlow()

    /**
     * **波形窗口是否已攒够**（1Hz 采样自 SDK 的 `hasEnoughWindow`）。
     *
     * ## 为什么（真实路径）不能从 [metric] 推导
     *
     * 「窗口攒够了」说的是**有没有真实数据**，「SQI ≥ 0.50」说的是**这批数据可不可信**。
     * 两者正交：真机实测出现过 SQI 判 INVALID（sqi < 0.50）而窗口早已满、HR 有真实值的
     * 情形 —— 那时如果用 `metric is Valid || Degraded` 当波形门控，卡片会永远停在
     * 「信号建立中」骨架，把"有数据只是质量没达标"误报成"还没开始采集"。
     *
     * 所以**真实路径的波形门控只认这条流**（= `bridge.hasEnoughWindow`）；
     * 数值显示仍由 [QualityGate]/SQI 门控（0.50 门限不动）。
     *
     * ## 演示路径的「窗口等价信号」
     *
     * 演示模式（[rppgAssembly] == null）没有 SDK，压根没有 `hasEnoughWindow` 可问。
     * 那一侧用**窗口等价信号** = `metric is Valid || Degraded`，即"这一秒已经有
     * 可信/可展示的指标"≈"数据已攒够能出值"。这样演示模式也会累积 [hrHistory] /
     * [rrHistory] 并画出曲线，而不是一条空虚线。
     *
     * 这条等价只在演示路径成立，且演示数据本身带全屏水印 + 写库标 simulated
     * （AC-19），不会被当成真实趋势 —— 口径是诚实的，不是给假图开绿灯。
     */
    private val _windowReady = MutableStateFlow(false)
    val windowReady: StateFlow<Boolean> = _windowReady.asStateFlow()

    /**
     * 心率 / 呼吸率的 **1Hz 历史**（最多 [HISTORY_MAX] 点，环形截断最旧的）。
     *
     * ## 为什么是"每秒采一次"而不是"从 SDK 拿波形"
     *
     * SDK 的 65 个公开方法/字段里**没有** PPG 波形接口（已核实），能拿到的
     * 只有每秒刷新一次的 [MetricUiState]。所以训练页的真实趋势线 =
     * **每秒采样的真实读数序列**，不是伪造的平滑曲线：
     * SDK 指标约 10s 才刷新一次，于是曲线呈台阶／平台 —— 那是真实读数窗口，
     * 保留它，不插值、不做平滑（插值就是造假）。
     *
     * 只在 [MetricUiState.Valid] / [MetricUiState.Degraded] 且对应值非空时追加；
     * NotEnabled / Establishing / NoData / Invalid 一律**不追加**（宁缺毋滥）。
     * 暂停后 frames 停发，历史自然冻结（设计稿 P04-b）。
     */
    private val _hrHistory = MutableStateFlow<List<Float>>(emptyList())
    val hrHistory: StateFlow<List<Float>> = _hrHistory.asStateFlow()

    private val _rrHistory = MutableStateFlow<List<Float>>(emptyList())
    val rrHistory: StateFlow<List<Float>> = _rrHistory.asStateFlow()

    private var started = false
    private var savedId: String? = null
    /** 演示模式（FR-14）。决定写库时的 sensorMode，绝不能被当成真实记录 */
    private var demoMode: Boolean = false

    /** 真实 rPPG 组装（sensor + bridge）。非真实路径时为 null。 */
    private var rppgAssembly: RppgSensorAssembly? = null

    /** 真实传感器是否就绪（cameraEnabled + appKey + bridge 初始化成功）。 */
    @Volatile
    private var realSensorReady: Boolean = false

    /**
     * 供页面持有的帧分析器（lazy：仅在真实路径需要时初始化 ML Kit）。
     *
     * 真实路径：onFeed → bridge.feed，onFrame → 帧状态跟踪（照 CalibrateViewModel:95/158 范本）。
     * 非真实路径：不会访问此属性（Screen 仅在 isRealSensor 时用它建 CameraController）。
     *
     * 刻意 lazy：FaceAnalyzer 构造时初始化 ML Kit FaceDetector，
     * 在纯 JVM 测试环境会抛 MlKitContext 未初始化。lazy 让测试不触碰它。
     */
    private val analyzerLazy = lazy {
        FaceAnalyzer(
            onFeed = { roi, dtSec ->
                val bridge = rppgAssembly?.bridge
                if (bridge != null) bridge.feed(roi, dtSec)
                else com.facephys.rppg.FrameResult.ERR_NOT_INITIALIZED
            },
            onFrame = { frame -> onFaceFrame(frame) },
        )
    }
    val analyzer: FaceAnalyzer get() = analyzerLazy.value

    /**
     * 当前感知数据源。
     *
     * 三态选择（纯函数 [chooseSensor]，集中可测）：
     *  · demoMode → DemoSensor
     *  · cameraEnabled + appKey 非空 → RppgSensor（真实源）
     *  · 否则 → NullSensor（无 key/无相机绝不出假数，FR-08/AC-14）
     */
    private var sensor: SensorAdapter = NullSensor()

    private var sessionStartedAtMs: Long = 0L

    init {
        // 进页面先查有没有未完成的训练（AC-13），没有就立刻开始新的一场。
        // 首版漏了"立刻开始"这一步，页面永远停在「等待开始」——
        // 真机验证时才发现（倒计时一直是 00:00）。
        viewModelScope.launch {
            val settings = container.currentSettings()
            val clinical = settings.screening.hasRiskAnswers
            demoMode = settings.isDemoMode
            _state.value = _state.value.copy(demoMode = settings.isDemoMode)
            // 语音速度真正生效（FR-05）：进入训练页时用当前设置值初始化播放器速度
            container.voice.setSpeed(settings.audioSpeed)
            // 三态选择（纯函数 chooseSensorKind，集中可测）：
            // 真实路径组装 bridge + RppgSensor；非真实路径回退 NullSensor/DemoSensor。
            rppgAssembly = if (!demoMode && cameraEnabled) container.createRppgSensor() else null
            // appKey 判定：rppgAssembly != null 说明 container 认为可以创建真实源
            val appKey = if (rppgAssembly != null) "real-key-present" else ""
            val kind = chooseSensorKind(demoMode, cameraEnabled, appKey)
            sensor = when (kind) {
                SensorKind.DEMO -> DemoSensor()
                SensorKind.REAL -> rppgAssembly?.sensor ?: NullSensor()
                SensorKind.NULL -> NullSensor()
            }
            realSensorReady = sensor is RppgSensor
            if (realSensorReady) {
                _state.value = _state.value.copy(isRealSensor = true)
            }
            // 先看内存里是否已有**正在进行**的会话（锁屏/重建后重新进入）
            val running = coordinator.currentFrame()
            val hasRunning = running.status == TrainingStatus.RUNNING ||
                running.status == TrainingStatus.PAUSED
            if (hasRunning) {
                // 认领进行中的会话：不弹恢复对话框、不重置进度
                started = true
                sessionStartedAtMs = System.currentTimeMillis() - coordinator.elapsedActiveMs()
                viewModelScope.launch { sensor.start() }
                // 关键：把 paused 对齐到会话真实状态。
                // 首版这里只置 started，于是"上次是暂停状态"被认领后，
                // 按钮显示"暂停"、圆环显示活动色，点一次还被引擎的幂等挡住——
                // 用户要点两次才能真的恢复。
                _state.value = _state.value.copy(
                    clinicalNotice = clinical,
                    paused = running.status == TrainingStatus.PAUSED,
                )
                observeFrames()
                return@launch
            }
            val pending = coordinator.pendingSnapshot()?.takeIf { it.isResumable(params) }
            _state.value = _state.value.copy(
                pendingRecovery = pending,
                clinicalNotice = clinical,
                voiceAvailable = container.voice.isAvailable,
            )
            if (pending == null) start()
        }
    }

    // ── 启动与恢复 ────────────────────────────────────────────────────────

    /** 用户选择「继续上次」*/
    fun continueFromSnapshot() {
        val snap = _state.value.pendingRecovery ?: return
        viewModelScope.launch {
            val ok = coordinator.restoreFrom(snap)
            started = true
            if (!ok) {
                // 参数变了：不能继续，只能丢弃
                container.snapshots.clear()
                coordinator.start(System.currentTimeMillis())
            }
            _state.value = _state.value.copy(pendingRecovery = null)
            observeFrames()
        }
    }

    /** 用户选择「丢弃」：清掉快照后立刻重开一场。 */
    fun discardSnapshot() {
        viewModelScope.launch {
            container.snapshots.clear()
            _state.value = _state.value.copy(pendingRecovery = null)
            start()
        }
    }

    /** 正常开始一场新训练。 */
    fun start() {
        if (started) return
        started = true
        sessionStartedAtMs = System.currentTimeMillis()
        viewModelScope.launch { sensor.start() }
        coordinator.start(System.currentTimeMillis())
        observeFrames()
    }

    private var observing = false

    private fun observeFrames() {
        if (observing) return
        observing = true
        viewModelScope.launch {
            coordinator.frames.collect { frame ->
                _frames.value = frame
                // 快照只取一次：metric（数值门控）与 history（波形采点）吃同一份读数，
                // 两条口径不会因为各自重新采一次而对不上同一秒的值。
                val snap = sensor.snapshot(coordinator.elapsedActiveMs().toInt() / 1000)
                val metric = computeMetric(snap)
                // 波形窗口门控，与 metric 同一个 1Hz 节拍（必须先算 metric：
                // 演示路径的"窗口等价信号"就读它）。
                //  · 真实路径：问 SDK —— bridge.hasEnoughWindow
                //  · 演示路径：没有 bridge ⇒ 用窗口等价信号（Valid/Degraded 即"数据够出值"）
                _windowReady.value = rppgAssembly?.bridge?.hasEnoughWindow
                    ?: (metric is MetricUiState.Valid || metric is MetricUiState.Degraded)
                _metric.value = metric
                recordHistory(snap)
                if (frame.status == TrainingStatus.COMPLETE && savedId == null) {
                    finishSession(frame)
                }
            }
        }
    }

    /**
     * 把这一秒的读数追加进历史（1Hz 采样，见 [hrHistory] 的说明）。
     *
     * ## 采点口径：只看窗口满没满，与 SQI 判定解耦
     *
     * 设计稿对波形的门控只有一条 —— P04-a:326「波形仅在 hasEnoughWindow 后绘」，
     * **没有 SQI 条件**。所以这里也只问两件事：窗口攒够了真实数据
     * （[windowReady] / `bridge.hasEnoughWindow`），且这一路的值不是 null。
     * hr 非 null 就采 hr，rr 非 null 就采 rr，互不牵连。
     *
     * 因此「窗口满但 SQI 低于门限被判 INVALID」的那些秒**照样采点**：
     * 曲线画的是真实读数趋势，不能因为数值门控而留一段空白。
     * 「数字能不能显示」是另一件事，仍由 QualityGate/SQI 门控
     * （[MetricUiState.showsNumbers]，INVALID 时显示「--」，不参与统计）。
     * 两条口径分开：**波形门控 = 窗口满，数值门控 = SQI**。
     *
     * 环形上限 [HISTORY_MAX]：超出丢最旧的，内存与绘制量恒定。
     */
    private fun recordHistory(snap: com.diqi.demo.sensor.SensorSnapshot) {
        val windowFull = windowReady.value || rppgAssembly?.bridge?.hasEnoughWindow == true
        if (!windowFull) return
        snap.heartRateBpm?.let { _hrHistory.value = appendBounded(_hrHistory.value, it) }
        snap.breathingRatePerMin?.let { _rrHistory.value = appendBounded(_rrHistory.value, it) }
    }

    /**
     * FaceAnalyzer.onFrame 回调：更新 RppgSensor 的帧状态。
     *
     * 照 CalibrateViewModel:158 范本：faceLostMs / lightOk 由 FaceAnalyzer 逐帧真实累计，
     * 这里只透传给 RppgSensor（非真实路径时 rppgAssembly 为 null，忽略）。
     * AC-16 遮脸 3s→NO_DATA 自然成立：faceLostMs 经 RppgSensor 传到 SensorSnapshot，
     * QualityGate 据此判 FACE_LOST_NO_DATA_MS。
     */
    /**
     * 最近一帧的人脸占画面比（[FaceFrame.faceRatio]），只用于 MetricTrace 日志。
     *
     * 真机标定时要能把「距离（faceRatio）」和「质量（SQI）」对到同一秒上，
     * 否则收紧距离门限就成了拍脑袋（问题 1）。
     */
    @Volatile
    private var lastFaceRatio: Float = 0f

    private fun onFaceFrame(frame: FaceFrame) {
        lastFaceRatio = frame.faceRatio
        val rppg = rppgAssembly?.sensor
        val lightOk = frame.brightness >= MIN_BRIGHTNESS
        if (rppg != null) {
            rppg.updateFrameState(faceLostMs = frame.faceLostMs, lightOk = lightOk)
        }
        // 把 faceFound 推到 meta（低频流），供 Screen 触发首次 AE/AWB 锁定
        _state.value = _state.value.copy(faceFound = frame.faceFound)
    }

    /**
     * 把感知快照过一遍 [QualityGate]，映射成指标区状态。
     *
     * 关键：`showNumbers` 为 false 时 UI **不会**渲染任何数字 ——
     * 这条约束由 `MetricUiState` 的类型表达，而不是靠界面记得判断。
     */
    private fun computeMetric(snap: com.diqi.demo.sensor.SensorSnapshot): MetricUiState {
        val metric = when {
            snap.missingReason == MissingReason.NOT_ENABLED -> MetricUiState.NotEnabled
            else -> evaluateMetric(snap)
        }
        logMetricTrace(snap, metric)
        return metric
    }

    /** 快照 → 指标区状态（[computeMetric] 的纯映射部分）。 */
    private fun evaluateMetric(snap: com.diqi.demo.sensor.SensorSnapshot): MetricUiState {
        // 演示模式缩短信号建立期（DemoSensor.HR_READY_SEC=3），真实源保持 30s
        val hrReadySec = if (demoMode) DemoSensor.HR_READY_SEC else QualityGate.HR_READY_SEC
        // 用数据源真实累计的 faceLostMs（不再拍常量）
        // SQI 双门限（见 QualityGate 类注释）：
        //  · 达标门限用临床默认 0.50 —— 不放松，VALID 的门槛不变；
        //  · 显示下限用 [BETA_SQI_DISPLAY_FLOOR] = 0.30（beta 演示标定，用户裁决），
        //    于是真实链路上 SQI 落在 0.30–0.50 时给数字但标「估算值仅供参考」。
        // 演示路径同样走这里（DemoSensor SQI=0.82 本就 ≥ 0.50，行为不变）。
        val verdict = QualityGate.evaluate(
            snap,
            hrReadySec = hrReadySec,
            sqiThreshold = QualityGate.DEFAULT_SQI_THRESHOLD,
            sqiDisplayFloor = BETA_SQI_DISPLAY_FLOOR,
            // **beta 演示取向：数值与曲线同门控**（问题 3）。
            // 曲线只认窗口（`hasEnoughWindow`），数值若还额外要求 SQI ≥ 0.30，
            // SQI 在门限附近抖动时就会数字闪断、曲线却连续 —— 两条口径必须统一。
            // 所以这里传 false：SQI 不再隐藏数字，只决定质量标签是哪一档
            // （≥0.50 正常 / 0.30–0.50「一般」/ <0.30「弱」），showNumbers 恒 true。
            // **临床发版改回 true**（默认值），恢复"低 SQI 隐藏数字"的严格口径。
            hideBelowFloor = false,
        )
        return when (verdict.state) {
            com.diqi.demo.data.db.QualityState.VALID -> MetricUiState.Valid(
                heartRateBpm = snap.heartRateBpm,
                breathingRatePerMin = snap.breathingRatePerMin,
                sqi = snap.sqi,
                dimmed = verdict.dimmed,
            )
            com.diqi.demo.data.db.QualityState.DEGRADED -> MetricUiState.Degraded(
                heartRateBpm = snap.heartRateBpm,
                breathingRatePerMin = snap.breathingRatePerMin,
                sqi = snap.sqi,
                banner = verdict.banner,
            )
            com.diqi.demo.data.db.QualityState.INVALID -> MetricUiState.Invalid(snap.sqi)
            com.diqi.demo.data.db.QualityState.NO_DATA -> when {
                // 窗口地面真相优先：窗口满却 NO_DATA，说明是别的原因（离框 / 无值），
                // 不能再报"信号建立中"——校准已攒满窗口就进训练时，会话秒数只有个位数，
                // 旧代理会把「看不到脸了」误报成「建立中 5s / 30s」。
                snap.windowReady == true -> MetricUiState.NoData(verdict.reason, verdict.banner)
                // 窗口未满 ⇒ 确实在建立（哪怕会话已跑过 30 秒）
                snap.windowReady == false -> MetricUiState.Establishing(snap.elapsedSec, hrReadySec)
                // null（Null/Demo 源）：沿用 elapsedSec 旧代理
                snap.elapsedSec < hrReadySec -> MetricUiState.Establishing(snap.elapsedSec, hrReadySec)
                else -> MetricUiState.NoData(verdict.reason, verdict.banner)
            }
        }
    }

    /**
     * 1Hz 诊断日志（真机排障用，保留为 debug 日志）。
     *
     * 用来区分三种「卡在信号建立中」的根因：
     *  ① 门控 bug —— window=true 但界面仍画骨架；
     *  ② 信号弱   —— window=true、sqi < 0.50（窗口有真实数据但质量不达标）；
     *  ③ 没投喂   —— window=false 且 elapsed 在增长（analyzer→bridge.feed 断了）。
     *
     * 抓法：`adb logcat -c && adb logcat -s MetricTrace RppgEngineImpl`。
     */
    private fun logMetricTrace(snap: com.diqi.demo.sensor.SensorSnapshot, metric: MetricUiState) {
        val bridge = rppgAssembly?.bridge
        Log.d(
            "MetricTrace",
            "state=${snap.state} hr=${snap.heartRateBpm} rr=${snap.breathingRatePerMin}" +
                " sqi=${snap.sqi} elapsed=${snap.elapsedSec} faceLost=${snap.faceLostMs}" +
                " faceRatio=${"%.3f".format(lastFaceRatio)}" +
                " lightOk=${snap.lightOk} window=${bridge?.hasEnoughWindow}" +
                " windowSnap=${snap.windowReady}" +
                " bridgeState=${bridge?.state} ui=${metric::class.simpleName}",
        )
    }

    // ── 交互 ──────────────────────────────────────────────────────────────

    /**
     * 暂停/恢复。
     *
     * **不能乐观取反**：`engine.pause()` 在"已结束"时是空操作，
     * `resume()` 在"未暂停"时也是空操作。乐观取反会让界面说"已暂停"
     * 而引擎其实还在跑（或已结束）。所以调完以后从引擎回读真实状态。
     */
    fun togglePause() {
        if (_state.value.paused) coordinator.resume() else coordinator.pause()
        syncPausedFromEngine()
    }

    /** 把 UI 的 paused 对齐到引擎真实状态。 */
    private fun syncPausedFromEngine() {
        val realPaused = coordinator.currentFrame().status == TrainingStatus.PAUSED
        if (_state.value.paused != realPaused) {
            _state.value = _state.value.copy(paused = realPaused)
        }
    }

    /** 给"结束确认"弹窗用的即时完成次数（弹窗是低频场景，直接取一次即可） */
    fun completedRepsNow(): Int = coordinator.currentFrame().completedReps

    fun requestStop() {
        _state.value = _state.value.copy(showStopConfirm = true)
    }

    fun cancelStop() {
        _state.value = _state.value.copy(showStopConfirm = false)
    }

    /** 二次确认后结束：保存已完成次数，不补造整组（AC-07）。 */
    fun confirmStop() {
        _state.value = _state.value.copy(showStopConfirm = false)
        coordinator.stop()
        if (savedId == null) {
            finishSession(coordinator.currentFrame())
        }
    }

    /** 保存失败后重试。 */
    fun retrySave() {
        _state.value = _state.value.copy(saveFailed = false)
        savedId = null
        finishSession(coordinator.currentFrame())
    }

    /** 保存失败后放弃：清会话并允许离开（用户已知情）。 */
    fun abandonOnSaveFailure() {
        _state.value = _state.value.copy(saveFailed = false, finished = true, savedSessionId = null)
        container.releaseSession()
    }

    fun repeatVoice() {
        coordinator.repeatVoice()
        _state.value = _state.value.copy(voiceAvailable = container.voice.isAvailable)
    }

    /** 当前帧（非响应式）：供低频场景取值 */
    fun frameNow(): TrainingFrame = coordinator.currentFrame()

    /**
     * 离开页面。
     *
     * **不能 release 会话**：会话是 application-scoped 的，训练中锁屏或重建
     * 后还要继续用它。真正的释放发生在训练结束（[confirmStop] / 自然完成）
     * 与容器销毁时。
     */
    fun onClearedComposition() {
        // 页面离开时停掉帧分析（需求 §5.3：页面销毁 release 相机链路）。
        // 会话本身是 app 级的，不在这里释放。
        //
        // **刻意不调 sensor.release()**：真实路径的 sensor/bridge 是
        // `AppContainer` 的**进程级共享实例**（校准页攒的窗口就在里面）。
        // 页面一释放，SDK 引擎窗口被清空，下次进训练又要重攒约 30 秒 ——
        // 那正是问题 2 要修的症状。相机链路一停，就没有帧再投喂，
        // 引擎侧自然空转（-16 超时由 RealRppgEngine 自动重连兜底）。
        // analyzer 是 lazy：仅在已初始化时才 release（避免强制初始化 ML Kit）
        if (analyzerLazy.isInitialized()) runCatching { analyzer.release() }
    }

    override fun onCleared() {
        super.onCleared()
        // 同上：不在这里 release。若用户杀掉进程，快照会兜住（AC-13）。
    }

    // ── 保存（唯一入口）──────────────────────────────────────────────────

    private fun finishSession(frame: TrainingFrame) {
        if (savedId != null) return
        savedId = sessionId

        val nowWall = System.currentTimeMillis()
        val startedWall = coordinator.startedAtWallMs()
        val elapsed = coordinator.elapsedActiveMs()
        val draft = SessionDraft(
            // 关键：用 init 里生成并一直持有的 id，不在保存时新生成（AC-08）
            id = sessionId,
            startedAt = startedWall,
            endedAt = nowWall,
            // 时长读单调时钟，不用墙钟差（训练中校时会污染）
            durationMs = elapsed,
            planVersion = params.hash(),
            plannedReps = params.repsPerSet,
            completedReps = frame.completedReps,
            pauseCount = coordinator.pauseCount(),
            pausedMs = coordinator.pausedMs(),
            // FR-14：演示模式产生的记录必须标 simulated，
            // 否则 `countsTowardProgress = !mode.isSimulated` 会把它
            // 计入打卡与周报，等于演示数据污染真实统计（AC-19）。
            // 真实路径标 OFFLINE_SDK（countsTowardProgress=true），否则 OFF。
            mode = when {
                demoMode -> SensorMode.SIMULATED
                realSensorReady -> SensorMode.OFFLINE_SDK
                else -> SensorMode.OFF
            },
            // 未接真实感知，覆盖率无从谈起 —— 如实为 null，
            // 由 SessionRepository 的 60% 门限保证不会带出假指标。
            signalCoverage = null,
        )

        viewModelScope.launch {
            runCatching { container.saveSession(draft) }
                .onSuccess {
                    _state.value = _state.value.copy(finished = true, savedSessionId = sessionId)
                    // 训练已结束，可以释放会话了
                    container.releaseSession()
                }
                .onFailure {
                    // 保存失败**不能**置 finished —— 置了会立刻导航到完成页，
                    // 失败弹窗只被组合一帧就消失（等于没提示）。
                    // 也不释放会话：留着重试的机会。
                    _state.value = _state.value.copy(saveFailed = true)
                }
        }
    }

    companion object {
        /** 亮度门限（与 CalibrateViewModel.MIN_BRIGHTNESS 对齐 = 60）*/
        private const val MIN_BRIGHTNESS = 60

        /** 波形历史上限：60 点 × 1Hz ≈ 1 分钟，够画满卡片宽度。 */
        const val HISTORY_MAX = 60

        /**
         * SQI **显示下限**（不是达标门限）。
         *
         * 用户裁决 2026-11-21：beta 演示/联调阶段标定 **0.30** —— 端侧 rPPG 在正常
         * 室内光下实测 SQI 常落在 0.3–0.5，沿用 0.50 单门限会让界面长期停在「信号弱」
         * 而不出数字，演示与联调都看不出链路是否真的在出值。中间带给数字，但常驻
         * 「信号一般，估算值仅供参考」，且不判 VALID、不进可信统计。
         *
         * 正式发版：临床签字后改回 [QualityGate.DEFAULT_SQI_THRESHOLD]（0.50），
         * 即 floor == threshold，中间带消失，回到严格单门限口径。
         */
        const val BETA_SQI_DISPLAY_FLOOR = 0.30f

        /** 环形追加：超出上限丢最旧的。纯函数，便于单测。 */
        internal fun appendBounded(history: List<Float>, value: Float): List<Float> =
            if (history.size >= HISTORY_MAX) {
                history.drop(history.size - HISTORY_MAX + 1) + value
            } else {
                history + value
            }
    }
}