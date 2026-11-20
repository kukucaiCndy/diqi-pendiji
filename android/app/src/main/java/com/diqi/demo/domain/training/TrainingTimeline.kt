package com.diqi.demo.domain.training

/**
 * 训练时间线：把「已用活动时长」映射成「当前该显示什么」的**纯函数**。
 *
 * ## 为什么是纯函数（决策 D11）
 *
 * 首版计划是「每 100ms tick 一次、在 tick 里推进状态」，评审指出三条必然失败路径：
 *
 * 1. **累加漂移**：`delay(100)` 实际 100–150ms，10 分钟累计偏差 10–30s → AC-05 挂
 * 2. **暂停边界**：`lastTickAt` 未重置时，恢复后首帧 delta = 整个暂停时长，
 *    一次 tick 跨 3–4 个阶段 → AC-02 挂
 * 3. **锁屏休眠**：前台服务不持 wakelock 时 CPU 会 suspend，`delay` 晚醒数秒。
 *    clamp 则进度落后，不 clamp 则跳阶段 —— **两个都错，说明模型本身无解**
 *
 * 纯函数模型下：`elapsedActiveMs` 由调用方用 `SystemClock.elapsedRealtime()`
 * 现算（含深睡、单调），本函数只做除法与取模。
 *
 * ## 阶段划分
 *
 * ```
 * |<-- PREPARATION -->|<-- CYCLE 1 -->|<-- CYCLE 2 -->| ... |<-- CYCLE N -->|
 *                     |C | H | R      |C | H | R      |     |C | H | R      |
 * ```
 * 一个 CYCLE = CONTRACT + HOLD + RELAX。
 *
 * ## 关于「锁屏很久后回来，已完成次数很大」
 *
 * 这是**刻意保留**的行为：训练期间有前台服务在跑，语音与倒计时确实在走，
 * 每一组都在按计划进行，计入是正确的。
 * 与 AC-07「不补造整组」不冲突 —— AC-07 说的是**用户主动提前结束**时
 * 不得把没做的补齐，那条由 `TrainingEngine.stop()` 冻结时间实现。
 * 真被杀进程的情况走快照恢复，`elapsedActiveMs` 冻结在快照值，不会膨胀。
 */
object TrainingTimeline {

    /**
     * 已完成整组的次数。
     *
     * AC-07「第 3 次收缩中结束 → completedReps=2」就以这里为准。
     */
    fun completedRepsAt(elapsedActiveMs: Long, params: TrainingParams): Int {
        if (elapsedActiveMs <= params.prepMs) return 0
        val inCycles = elapsedActiveMs - params.prepMs
        val fullCycles = inCycles / params.cycleMs
        return fullCycles.toInt().coerceIn(0, params.repsPerSet)
    }

    /**
     * 求帧。
     *
     * 签名里**没有** `status` 形参：阶段与已完成次数完全由已用时长决定，
     * 运行/暂停/结束这些"状态词"由 [TrainingEngine] 回填。
     * 首版保留了 `status` 却只在 IDLE 分支用，且 COMPLETE 分支硬编码满组次数 ——
     * 任何调用方按字面传 COMPLETE 都会拿到"状态=完成但阶段还在收缩"的
     * 自相矛盾帧（评审 E4）。
     */
    fun frameAt(elapsedActiveMs: Long, params: TrainingParams): TrainingFrame {
        val clamped = elapsedActiveMs.coerceAtLeast(0)
        val completed = completedRepsAt(clamped, params)

        // 准备阶段
        if (clamped < params.prepMs) {
            return TrainingFrame(
                status = TrainingStatus.RUNNING,
                phase = TrainingPhase.PREPARATION,
                phaseRemainingMs = params.prepMs - clamped,
                phaseTotalMs = params.prepMs,
                repIndex = 1,
                totalReps = params.repsPerSet,
                overallProgress = progress(clamped, params),
                completedReps = 0,
            )
        }

        val inCycles = clamped - params.prepMs
        val cycleIndex = inCycles / params.cycleMs

        // 时间线走完（自然完成）
        if (cycleIndex >= params.repsPerSet) {
            return TrainingFrame(
                status = TrainingStatus.COMPLETE,
                phase = TrainingPhase.COMPLETE,
                phaseRemainingMs = 0,
                phaseTotalMs = 0,
                repIndex = params.repsPerSet,
                totalReps = params.repsPerSet,
                overallProgress = 1f,
                completedReps = params.repsPerSet,
            )
        }

        val within = inCycles % params.cycleMs
        val (phase, phaseTotal, phaseRemaining) = when {
            within < params.contractMs ->
                Triple(TrainingPhase.CONTRACT, params.contractMs, params.contractMs - within)

            within < params.contractMs + params.holdMs ->
                Triple(
                    TrainingPhase.HOLD,
                    params.holdMs,
                    params.contractMs + params.holdMs - within,
                )

            else ->
                Triple(TrainingPhase.RELAX, params.relaxMs, params.cycleMs - within)
        }

        return TrainingFrame(
            status = TrainingStatus.RUNNING,
            phase = phase,
            phaseRemainingMs = phaseRemaining.coerceAtLeast(0),
            phaseTotalMs = phaseTotal,
            repIndex = (cycleIndex + 1).toInt(),
            totalReps = params.repsPerSet,
            overallProgress = progress(clamped, params),
            // 只计已完整走完的组，不补造
            completedReps = completed,
        )
    }

    private fun progress(elapsedActiveMs: Long, params: TrainingParams): Float {
        if (params.totalActiveMs <= 0L) return 1f
        return (elapsedActiveMs.toFloat() / params.totalActiveMs).coerceIn(0f, 1f)
    }
}