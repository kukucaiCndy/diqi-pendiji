package com.diqi.demo.ui.screens.training

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
import com.diqi.demo.sensor.SensorAdapter
import com.diqi.demo.training.TrainingSessionCoordinator
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

    private var started = false
    private var savedId: String? = null
    /** 演示模式（FR-14）。决定写库时的 sensorMode，绝不能被当成真实记录 */
    private var demoMode: Boolean = false

    /** 当前感知数据源。演示模式 → DemoSensor；否则 NullSensor（S6 第二部分接真实源）*/
    private var sensor: SensorAdapter = container.sensorFor(demoMode = false)

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
            sensor = container.sensorFor(demoMode, cameraEnabled)
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
                _metric.value = computeMetric()
                if (frame.status == TrainingStatus.COMPLETE && savedId == null) {
                    finishSession(frame)
                }
            }
        }
    }

    /**
     * 把感知快照过一遍 [QualityGate]，映射成指标区状态。
     *
     * 关键：`showNumbers` 为 false 时 UI **不会**渲染任何数字 ——
     * 这条约束由 `MetricUiState` 的类型表达，而不是靠界面记得判断。
     */
    private fun computeMetric(): MetricUiState {
        val snap = sensor.snapshot(coordinator.elapsedActiveMs().toInt() / 1000)
        if (snap.missingReason == MissingReason.NOT_ENABLED) {
            return MetricUiState.NotEnabled
        }
        // 用数据源真实累计的 faceLostMs（不再拍常量）
        val verdict = QualityGate.evaluate(snap)
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
            com.diqi.demo.data.db.QualityState.NO_DATA ->
                if (snap.elapsedSec < QualityGate.HR_READY_SEC) {
                    MetricUiState.Establishing(snap.elapsedSec)
                } else {
                    MetricUiState.NoData(verdict.reason, verdict.banner)
                }
        }
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
        // 页面离开时停投喂（需求 §5.3：页面销毁 release()）。
        // 会话本身是 app 级的，不在这里释放。
        runCatching { sensor.release() }
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
            mode = if (demoMode) SensorMode.SIMULATED else SensorMode.OFF,
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
}