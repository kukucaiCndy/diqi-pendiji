package com.diqi.demo.training

import com.diqi.demo.domain.training.EngineSnapshot
import com.diqi.demo.domain.training.TrainingFrame
import kotlinx.coroutines.flow.StateFlow

/**
 * 一场训练会话的对外契约。
 *
 * ## 为什么把它从具体类里抽出来
 *
 * 上一轮写了 `TrainingViewModelTest` 却跑不起来：VM 直接依赖
 * `TrainingSessionCoordinator`，而后者持有一个**永续 ticker**
 * （`while (isActive) { … delay(100) }`，只有训练完成才 break）。
 * 在测试调度器里，虚拟时间会无限推进 —— 测试挂死，不是断言失败。
 *
 * 抽成接口后，单测注一个**不带 ticker 的最小桩**即可：
 * 只把 `frames` 手动 emit 到需要的值，其余方法记录调用次数。
 * 这既符合"单元测试只测一个单元"，也避免和协程调度器斗。
 *
 * 成员刻意与 `TrainingSessionCoordinator` 的现有方法**同名同签名**，
 * 这样生产实现只需加 `override`，调用点无需改动。
 */
interface TrainingSession {
    val frames: StateFlow<TrainingFrame>

    fun start(wallClockMs: Long)

    fun pause()

    fun resume()

    /** 用户主动结束：冻结时间、保留已完成次数（AC-07） */
    fun stop()

    /** 页面/会话销毁：停 tick、停语音、落快照 */
    fun release()

    fun repeatVoice()

    suspend fun restoreFrom(snapshot: EngineSnapshot): Boolean

    suspend fun pendingSnapshot(): EngineSnapshot?

    fun currentFrame(): TrainingFrame

    fun pauseCount(): Int

    fun pausedMs(): Long

    fun elapsedActiveMs(): Long

    fun startedAtWallMs(): Long

    fun isFinished(): Boolean
}
