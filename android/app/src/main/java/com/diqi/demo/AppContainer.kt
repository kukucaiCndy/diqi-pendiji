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
import com.diqi.demo.sensor.NullSensor
import com.diqi.demo.sensor.SensorAdapter
import com.diqi.demo.domain.training.TrainingEngine
import com.diqi.demo.domain.training.TrainingParams
import com.diqi.demo.training.TrainingSessionCoordinator
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
class AppContainer(private val app: Application) {

    val clock: Clock = MonotonicClock

    val context: Application get() = app

    val database: HerRhythmDb by lazy { HerRhythmDb.create(app) }

    val settingsStore: AppSettingsStore by lazy { AppSettingsStore(app) }

    val sessionRepository: SessionRepository by lazy { SessionRepository(database) }

    val progressRepository: ProgressRepository by lazy { ProgressRepository(database) }

    val leakDiaryRepository: LeakDiaryRepository by lazy { LeakDiaryRepository(database) }

    val snapshotStore: SnapshotStore by lazy { DataStoreSnapshotStore(app) }

    /**
     * 按当前设置选择感知数据源。
     *
     * - 演示模式 → [DemoSensor]（产出数值但带 `isSimulated` 标记，必须水印）
     * - 其他 → [NullSensor]（如实"未启用感知"）
     *
     * 真实 RppgSensor 在 S6 第二部分接入（CameraX + ML Kit + Bridge）。
     * 刻意先只有这两个实现：**没有摄像头也绝不出假数值**，
     * 这是 FR-08 与 AC-14 的底线。
     */
    fun sensorFor(demoMode: Boolean): SensorAdapter =
        if (demoMode) DemoSensor() else NullSensor()

    /**
     * 语音播放器。
     *
     * app-scoped 而不是页面级：训练中页面可能重建（旋转禁止但配置变化仍可能），
     * 播放器跟着页面重建会打断正在播的句子。播完会主动归还音频焦点，
     * 所以长期持有是安全的。
     */
    val voiceGuide: VoiceGuide by lazy { AssetVoiceGuide(app) }

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
    fun obtainTrainingSession(params: TrainingParams = TrainingParams.DEFAULT): TrainingSessionCoordinator {
        _activeTraining?.let { if (!it.isFinished()) return it }
        val session = TrainingSessionCoordinator(
            engine = TrainingEngine(clock, params),
            clock = clock,
            voice = voiceGuide,
            snapshots = snapshotStore,
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
        voice = voiceGuide,
        snapshots = snapshotStore,
        scope = scope,
        params = params,
    )
}