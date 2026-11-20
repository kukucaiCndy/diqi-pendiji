package com.diqi.demo.training

import com.diqi.demo.audio.VoiceGuide
import com.diqi.demo.core.time.Clock
import com.diqi.demo.data.prefs.SnapshotStore
import com.diqi.demo.domain.training.EngineSnapshot
import com.diqi.demo.domain.training.TrainingEngine
import com.diqi.demo.domain.training.TrainingFrame
import com.diqi.demo.domain.training.TrainingParams
import com.diqi.demo.domain.training.TrainingPhase
import com.diqi.demo.domain.training.TrainingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 训练会话协调器：把 **引擎 + 语音 + 快照** 接成一个可用的东西。
 *
 * ## 为什么需要这一层
 *
 * 评审指出：`TrainingEngine` 在 main 源集里**没有任何生产调用点** ——
 * 于是 AC-02/05/07/13 的单测在测一个"没人使用的库"，
 * 而快照恢复路径（AC-13）连实现都不存在。这个类的职责就是把那条链路补齐：
 *
 * - **唯一的 tick 驱动点**：100ms 刷新 `frames`，只负责重绘，不推进状态（D11）
 * - **阶段变更时播语音**：由 frame 的 stage 变化驱动，音频与状态机零耦合
 * - **快照落盘点**：暂停、结束、以及运行中每 10 秒一次
 *
 * ## 为什么 tick 周期与正确性无关
 *
 * `frames` 只是"当前该显示什么"的投影。即使 tick 被延迟 5 秒，
 * 下一次 `frame()` 也会算出正确的阶段 —— 因为它读的是 `elapsedRealtime` 差值。
 */
class TrainingSessionCoordinator(
    private val engine: TrainingEngine,
    /** 与引擎**同一个**时钟实例。传进来而不是从引擎里取，避免两处时间源不一致。 */
    private val clock: Clock,
    private val voice: VoiceGuide,
    private val snapshots: SnapshotStore,
    private val scope: CoroutineScope,
    val params: TrainingParams = engine.params,
    private val tickIntervalMs: Long = 100L,
    private val snapshotIntervalMs: Long = 10_000L,
) {

    private val _frames = MutableStateFlow(TrainingFrame.idle(params.repsPerSet))
    val frames: StateFlow<TrainingFrame> = _frames.asStateFlow()

    private var ticker: Job? = null

    /** 上一次播过的阶段，用于"阶段变更才播"（避免同一阶段反复播）*/
    private var lastSpokenPhase: TrainingPhase? = null

    private var lastSnapshotAtMs: Long = 0L

    /** 最近一次播放的语音（P04「语音状态」显示）*/
    var lastVoiceAtMs: Long = 0L
        private set

    // ── 生命周期 ──────────────────────────────────────────────────────────

    fun start(wallClockMs: Long = System.currentTimeMillis()) {
        engine.start(wallClockMs = wallClockMs)
        lastSpokenPhase = null
        voice.play(com.diqi.demo.audio.VoiceScript.START)
        lastVoiceAtMs = wallClockMs
        // 同步先出第一帧：ticker 是协程，调用方紧接着读 frames 时不能还是 IDLE
        emitFrame()
        restartTicker()
    }

    fun pause() {
        engine.pause()
        voice.stop()
        voice.play(com.diqi.demo.audio.VoiceScript.PAUSE)
        lastVoiceAtMs = clock.nowMs()
        emitFrame()
        persistSnapshotAsync()
    }

    fun resume() {
        engine.resume()
        restartTicker()
        emitFrame()
    }

    /** 用户主动结束：冻结时间（AC-07），落快照，停 tick。 */
    fun stop() {
        engine.stop()
        voice.stop()
        stopTicker()
        emitFrame()
        scope.launch { snapshots.clear() }
    }

    /**
     * 会话结束/页面销毁：停 tick、停语音、保存快照。
     *
     * **绝不能调 `voice.release()`** —— voice 是 application-scoped 单例
     * （`AppContainer.voiceGuide`），而 `AssetVoiceGuide.release()` 会把
     * `released` 置成不可逆终态。首版在这里 release 了它，导致
     * **本进程第一场训练之后，后续每一场都全程无声**，而且不崩不报错、
     * `isAvailable` 仍是 true（评审抓到的发布阻断级缺陷）。
     *
     * 语音的生命周期归容器；这里只 `stop()` 掉当前正在播的那句。
     */
    fun release() {
        stopTicker()
        voice.stop()
        persistSnapshotAsync()
    }

    fun repeatVoice() {
        voice.playPhase(currentFrame().phase)
        lastVoiceAtMs = clock.nowMs()
    }

    /** 恢复中断的训练（AC-13）。返回 false 表示参数已变，应丢弃。 */
    suspend fun restoreFrom(snapshot: EngineSnapshot): Boolean {
        val ok = engine.restore(snapshot)
        if (ok) {
            lastSpokenPhase = null
            emitFrame()
            restartTicker()
        }
        return ok
    }

    suspend fun pendingSnapshot(): EngineSnapshot? = snapshots.load()

    fun currentFrame(): TrainingFrame = engine.frame(clock.nowMs())

    fun pauseCount(): Int = engine.pauseCount

    fun pausedMs(): Long = engine.pausedMs(clock.nowMs())

    fun elapsedActiveMs(): Long = engine.elapsedActiveMs(clock.nowMs())

    fun startedAtWallMs(): Long = engine.wallClockAtStartMs ?: System.currentTimeMillis()

    fun isFinished(): Boolean = engine.isFinished(clock.nowMs())

    // ── 内部 ──────────────────────────────────────────────────────────────

    private fun restartTicker() {
        stopTicker()
        ticker = scope.launch {
            while (isActive) {
                emitFrame()
                // 运行中定期落快照：进程被系统杀掉时最多丢 10 秒
                val now = clock.nowMs()
                if (now - lastSnapshotAtMs >= snapshotIntervalMs) {
                    lastSnapshotAtMs = now
                    persistSnapshot()
                }
                if (currentFrame().status == TrainingStatus.COMPLETE) break
                delay(tickIntervalMs)
            }
            stopTicker()
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun emitFrame() {
        val frame = engine.frame(clock.nowMs())
        _frames.value = frame

        // 阶段变更才播语音：避免每 100ms 重复触发
        if (frame.phase != lastSpokenPhase && frame.status != TrainingStatus.COMPLETE) {
            lastSpokenPhase = frame.phase
            voice.playPhase(frame.phase)
            lastVoiceAtMs = clock.nowMs()
        }
        if (frame.status == TrainingStatus.COMPLETE && lastSpokenPhase != TrainingPhase.COMPLETE) {
            lastSpokenPhase = TrainingPhase.COMPLETE
            voice.playPhase(TrainingPhase.COMPLETE)
            lastVoiceAtMs = clock.nowMs()
        }
    }

    private suspend fun persistSnapshot() {
        val snap = engine.snapshot(clock.nowMs()) ?: return
        // 已跑完的快照没有恢复价值，清掉避免下次误提示
        if (engine.status == TrainingStatus.COMPLETE && engine.isFinished()) {
            snapshots.clear()
        } else {
            snapshots.save(snap)
        }
    }

    private fun persistSnapshotAsync() {
        scope.launch { persistSnapshot() }
    }
}