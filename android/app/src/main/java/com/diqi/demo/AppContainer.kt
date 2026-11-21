package com.diqi.demo

import android.app.Application
import com.diqi.demo.core.time.Clock
import com.diqi.demo.core.time.MonotonicClock
import com.diqi.demo.data.db.HerRhythmDb
import com.diqi.demo.data.prefs.AppSettingsStore
import com.diqi.demo.data.repo.LeakDiaryRepository
import com.diqi.demo.data.repo.ProgressRepository
import com.diqi.demo.audio.AssetVoiceGuide
import com.diqi.demo.audio.VoiceGuide
import com.diqi.demo.data.prefs.DataStoreSnapshotStore
import com.diqi.demo.data.prefs.SnapshotStore
import com.diqi.demo.data.repo.SessionRepository
import com.diqi.demo.sensor.DemoSensor
import com.diqi.demo.sensor.FacePhysBridge
import com.diqi.demo.sensor.NullSensor
import com.diqi.demo.sensor.RppgSensor
import com.diqi.demo.sensor.SensorAdapter
import com.diqi.demo.domain.training.TrainingEngine
import com.diqi.demo.domain.training.TrainingParams
import com.diqi.demo.training.RppgSensorAssembly
import com.diqi.demo.training.TrainingSessionCoordinator
import com.facephys.rppg.RealRppgEngine
import com.facephys.rppg.RppgEngine
import com.facephys.rppg.StubRppgEngine
import com.diqi.demo.training.TrainingDeps
import com.diqi.demo.ui.screens.settings.SettingsDeps
import com.diqi.demo.work.ReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * 手写依赖容器（决策 D7：不引入 Hilt）。
 *
 * ## 生命周期归属（这是本容器存在的核心价值 —— 明确"谁活多久"）
 *
 * **application-scoped（与进程同生共死，绝不持有 Activity）**
 *  · [clock] 时钟
 *  · [database] Room —— 每进程一个实例，重复 open 会导致 WAL 争用
 *  · [settingsStore] DataStore —— **一个文件只能一个实例**，同名二次
 *    `preferencesDataStore` 会抛 `IllegalStateException`
 *  · [sessionRepository] / [progressRepository] / [leakDiaryRepository]
 *  · 真实 rPPG 组装（`RppgSensor` + `FacePhysBridge`，见 `sharedRppgAssembly`）——
 *    **必须共享**：SDK 窗口是引擎内部状态，新建实例等于清空窗口，
 *    校准攒满 30s 后进入训练页又要重等（问题 2 的根因）
 *  · 后续：`SensorCoordinator`（**必须** app-scoped，否则锁屏时 Activity 销毁
 *    会连带释放 SDK 窗口，AC-05「锁屏 30s 返回指标恢复」失败 —— 决策 D12）、
 *    `VoiceGuide`
 *
 * **screen-scoped（必须随页面销毁）**
 *  · CameraX 的 `ProcessCameraProvider` 绑定、`ImageAnalysis` 用例、ML Kit detector
 *    —— 页面销毁必须 unbind + close，否则 AC-18「连续进出 5 次」会泄漏相机
 *
 * **process-scoped but restartable**
 *  · `TrainingEngine` 可随 ViewModel 重建，状态从 DataStore 快照恢复
 *
 * ## 为什么全部 lazy
 *
 * Room 与 DataStore 的初始化都不该发生在 `Application.onCreate`：
 * 冷启动目标是 < 2s（需求 §7），而首个数据库访问往往要等几百毫秒。
 * 用 `by lazy` 让它们在真正需要时才初始化。CameraX **绝不能**在这里初始化 ——
 * 它需要 Activity/Lifecycle 上下文。
 */
class AppContainer(private val app: Application) : TrainingDeps, SettingsDeps {

    override val clock: Clock = MonotonicClock

    val context: Application get() = app

    val database: HerRhythmDb by lazy { HerRhythmDb.create(app) }

    val settingsStore: AppSettingsStore by lazy { AppSettingsStore(app) }

    val sessionRepository: SessionRepository by lazy { SessionRepository(database) }

    val progressRepository: ProgressRepository by lazy { ProgressRepository(database) }

    val leakDiaryRepository: LeakDiaryRepository by lazy { LeakDiaryRepository(database) }

    override val snapshots: SnapshotStore by lazy { DataStoreSnapshotStore(app) }

    /**
     * 按当前设置选择感知数据源（供 Null/Demo 路径）。
     *
     * - 演示模式 → [DemoSensor]（产出数值但带 `isSimulated` 标记，必须水印）
     * - 其他 → [NullSensor]（如实"未启用感知"）
     *
     * 真实路径（[com.diqi.demo.sensor.RppgSensor]）由 [TrainingViewModel] 自行组装：
     * 它需要 FacePhysBridge + FaceAnalyzer + CameraController 的完整链路，
     * 不适合在这里的单一工厂方法里构造。三态选择的纯函数见
     * [com.diqi.demo.sensor.chooseSensor]。
     *
     * **没有摄像头也绝不出假数值**，这是 FR-08 与 AC-14 的底线。
     */
    override fun sensorFor(demoMode: Boolean, cameraEnabled: Boolean): SensorAdapter =
        if (demoMode) DemoSensor() else NullSensor()

    /**
     * rPPG 引擎工厂：BuildConfig.FACEPHYS_APP_KEY 非空 → RealRppgEngine（真实 SDK），
     * 否则 StubRppgEngine（契约桩，NullSensor/演示路径仍可跑）。
     *
     * beta 期 AppKey 通过 BuildConfig 注入（local.properties），控制台「建议服务端下发」，
     * 正式上线前切换为服务端动态拉取。
     */
    fun createRppgEngine(): RppgEngine =
        if (BuildConfig.FACEPHYS_APP_KEY.isNotBlank()) RealRppgEngine() else StubRppgEngine()

    /**
     * **application-scoped 的真实 rPPG 组装**（engine + bridge + sensor，进程内唯一）。
     *
     * ## 为什么必须在容器里共享（而不是每次训练新建）
     *
     * 首版每次 [createRppgSensor] 都新建 engine + bridge，于是**校准页攒满的 SDK
     * 窗口在进入训练页时被丢掉**，训练页从 0 帧重新建立，用户要再干等约 30 秒 ——
     * 窗口是 SDK 引擎内部状态，跟实例同生共死，新建实例等于清空窗口。
     *
     * 共享之后：校准页（[com.diqi.demo.ui.screens.calibrate.CalibrateViewModel]）
     * 与训练页（[com.diqi.demo.ui.screens.training.TrainingViewModel]）投喂同一个
     * bridge，窗口自然延续 —— 校准攒满 → 训练第二秒就有真实读数。
     *
     * ## 生命周期
     *
     * 与进程同生共死，**页面不得释放它**（`RppgSensor.release()` 以
     * `ownsBridge=false` 构造，只停投喂；`FacePhysBridge.init` 已幂等，
     * 训练 start 不会重置窗口）。SDK 自身 5 分钟上限（-16）由
     * [RealRppgEngine] 的自动重连兜底，不靠页面重建开新实例。
     *
     * 无 AppKey 时为 null —— VM 回退到 NullSensor（无 key 绝不出假数，FR-08/AC-14）。
     */
    private val sharedRppgAssembly: RppgSensorAssembly? by lazy {
        val appKey = BuildConfig.FACEPHYS_APP_KEY
        if (appKey.isBlank()) {
            null
        } else {
            val engine = createRppgEngine()
            val bridge = FacePhysBridge.create(app, engine)
            RppgSensorAssembly(
                sensor = RppgSensor(
                    bridge = bridge,
                    appKey = appKey,
                    encAssetName = "step_mini.tflite.enc",
                    // 容器持有 bridge 生命周期，sensor.release() 不得连带释放引擎
                    ownsBridge = false,
                ),
                bridge = bridge,
            )
        }
    }

    /**
     * 容器级共享 bridge（仅在有 AppKey 时为非 null）。
     *
     * 校准页用它攒窗口，训练页用同一个继续 —— 这是「免再等 30s」的全部机制。
     * 无 AppKey 时校准页自建桩 bridge（StubRppgEngine 无真实窗口，行为与旧版一致）。
     */
    val sharedBridge: FacePhysBridge? get() = sharedRppgAssembly?.bridge

    /**
     * 真实 rPPG 传感器组装。**返回容器级共享实例**，不再每次新建。
     *
     * 无 AppKey 时返回 null —— VM 回退到 NullSensor（无 key 绝不出假数，FR-08/AC-14）。
     */
    override fun createRppgSensor(): RppgSensorAssembly? = sharedRppgAssembly

    /**
     * 语音播放器。
     *
     * app-scoped 而不是页面级：训练中页面可能重建（旋转禁止但配置变化仍可能），
     * 播放器跟着页面重建会打断正在播的句子。播完会主动归还音频焦点，
     * 所以长期持有是安全的。
     */
    override val voice: VoiceGuide by lazy { AssetVoiceGuide(app) }

    /**
     * **训练会话必须 application-scoped**（这条是 AC-05 的根因修复）。
     *
     * 首版把协调器放在 ViewModel 里，锁屏时 Activity 重建 → ViewModel 重建 →
     * 引擎的 `startedAtElapsedMs` 丢失 → 用户看到的进度退回 0，
     * 只能靠快照"继续"（而快照最多落后 10 秒）。
     * 实测 AC-05「锁屏 30s 返回，进度误差 ≤1s」直接不成立。
     *
     * 放到容器里之后：Activity 重建不影响引擎，回来时同一个引擎继续算，
     * 因为时间是从 `elapsedRealtime` 现算的纯函数（D11）。
     */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var _activeTraining: TrainingSessionCoordinator? = null

    /** 当前进行中的训练（可能为 null）。UI 重建时用它认领会话而不是新建。 */
    val activeTraining: TrainingSessionCoordinator?
        get() = _activeTraining?.takeIf { !it.isFinished() }

    /**
     * 取得**当前这一场**训练：有未结束的就认领，没有才新建。
     *
     * 注意引擎是 app-scoped 的，所以这里不再需要外部传入 viewModelScope。
     */
    override fun obtainSession(params: TrainingParams): com.diqi.demo.training.TrainingSession =
        obtainTrainingSession(params)

    override suspend fun currentSettings(): com.diqi.demo.data.prefs.AppSettings =
        settingsStore.current()

    override suspend fun saveSession(draft: com.diqi.demo.data.repo.SessionDraft) {
        sessionRepository.save(draft)
    }

    override fun releaseSession() = releaseTrainingSession()

    fun obtainTrainingSession(params: TrainingParams = TrainingParams.DEFAULT): TrainingSessionCoordinator {
        _activeTraining?.let { if (!it.isFinished()) return it }
        val session = TrainingSessionCoordinator(
            engine = TrainingEngine(clock, params),
            clock = clock,
            voice = voice,
            snapshots = snapshots,
            scope = appScope,
            params = params,
        )
        _activeTraining = session
        return session
    }

    /** 结束并丢弃当前会话（用于"丢弃"与测试清理）。 */
    fun releaseTrainingSession() {
        _activeTraining?.release()
        _activeTraining = null
    }

    /**
     * 为一次训练新建协调器。
     *
     * **每次训练必须是新实例**：引擎内部持有本场的起点与暂停累计，
     * 复用会把上一场的时间带进来。
     *
     * @param scope 生命周期归属方（训练页 ViewModel 或前台服务）的 scope
     */
    fun newTrainingSession(
        scope: CoroutineScope,
        params: TrainingParams = TrainingParams.DEFAULT,
    ): TrainingSessionCoordinator = TrainingSessionCoordinator(
        engine = TrainingEngine(clock, params),
        clock = clock,
        voice = voice,
        snapshots = snapshots,
        scope = scope,
        params = params,
    )

    // ── SettingsDeps ───────────────────────────────────────────────────────
    // currentSettings() 由 TrainingDeps 的 override 已实现，两者签名相同

    override val settingsFlow: kotlinx.coroutines.flow.Flow<com.diqi.demo.data.prefs.AppSettings>
        get() = settingsStore.settings

    override suspend fun setReminderEnabled(enabled: Boolean) =
        settingsStore.setReminderEnabled(enabled)

    override suspend fun setReminderTime(hhmm: String) =
        settingsStore.setReminderTime(hhmm)

    override suspend fun setAudioSpeed(speed: Float) =
        settingsStore.setAudioSpeed(speed)

    override suspend fun setDemoMode(enabled: Boolean) =
        settingsStore.setDemoMode(enabled)

    override suspend fun clearBusinessSettings() =
        settingsStore.clearBusinessSettings()

    override suspend fun clearAllSessions() =
        sessionRepository.clearAll()

    override suspend fun clearSimulatedSessions() =
        sessionRepository.clearSimulated()

    override suspend fun clearAllLeakDiary() =
        leakDiaryRepository.clearAll()

    override fun scheduleReminder(timeHHmm: String) {
        ReminderWorker.schedule(app, timeHHmm)
    }

    override fun cancelReminder() {
        ReminderWorker.cancel(app)
    }
}