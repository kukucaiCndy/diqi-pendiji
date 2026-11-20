package com.diqi.demo.training

import com.diqi.demo.audio.VoiceGuide
import com.diqi.demo.core.time.Clock
import com.diqi.demo.data.prefs.AppSettings
import com.diqi.demo.data.prefs.SnapshotStore
import com.diqi.demo.data.repo.SessionDraft
import com.diqi.demo.domain.training.TrainingParams
import com.diqi.demo.sensor.SensorAdapter

/**
 * `TrainingViewModel` 需要的最小依赖面。
 *
 * ## 为什么要抽这一层
 *
 * 复审发现：`TrainingViewModel`（326 行）是整个应用最关键的接入点，
 * 却在 `test/` 与 `androidTest/` 里被引用 **0 次**。于是这些行为全无自动化覆盖：
 *
 * - AC-08「连点两次完成只产生一条记录」（`savedId` 闸门）
 * - AC-19「演示模式写的记录必须是 simulated」（`mode` 归属）
 * - AC-13「进入时有未完成快照就弹继续/丢弃」
 * - 「保存失败不能让用户卡住也不能静默」（`saveFailed` 分支）
 *
 * 根因是它直接依赖具体的 `AppContainer`（里面是 Room + DataStore，都要 Android 环境）。
 * 抽成接口后，JVM 单测可以用假实现跑通这些分支 —— 不需要设备，也能进 CI。
 *
 * 刻意保持**窄**：只放 VM 真正用到的 8 个成员，不把整个容器搬过来。
 */
interface TrainingDeps {
    val clock: Clock
    val voice: VoiceGuide
    val snapshots: SnapshotStore

    suspend fun currentSettings(): AppSettings

    suspend fun saveSession(draft: SessionDraft)

    fun sensorFor(demoMode: Boolean): SensorAdapter

    /** 取得（或新建）当前训练会话 */
    fun obtainSession(params: TrainingParams): TrainingSessionCoordinator

    fun releaseSession()
}
