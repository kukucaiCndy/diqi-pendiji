package com.diqi.demo

import android.app.Application
import com.diqi.demo.core.time.Clock
import com.diqi.demo.core.time.MonotonicClock
import com.diqi.demo.data.db.HerRhythmDb
import com.diqi.demo.data.prefs.AppSettingsStore
import com.diqi.demo.data.repo.LeakDiaryRepository
import com.diqi.demo.data.repo.ProgressRepository
import com.diqi.demo.data.repo.SessionRepository

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
}