package com.diqi.demo.domain.training

import com.diqi.demo.core.time.Clock

/**
 * 训练引擎：持有运行状态，对外只暴露「当前这一帧」。
 *
 * ## 设计要点
 *
 * 1. **时间不进状态**（D11）：唯一被记住的是 `startedAtElapsedMs` 与暂停累计量，
 *    当前阶段完全由 [TrainingTimeline] 从 `elapsedActiveMs` 现算。
 * 2. **时钟注入**：引擎不自己读系统时间，便于用 `TestClock` 制造
 *    "暂停 10 秒""锁屏 30 秒"这类场景。
 * 3. **恢复用相对量**：进程重启后 `elapsedRealtime` 归零，恢复旧的绝对起点
 *    会算出负数。故 [restore] 接的是"快照时的已用时长"，用 `now - 该值` 倒推起点。
 * 4. **结束必须冻结时间**：`stop()` 若只置标志位，之后每次 `frame()` 都重算
 *    `elapsedActiveMs`，一越过总时长就变成"满组" —— 正是 AC-07 要防的补造。
 *    因此 `stop()` 记下 `frozenElapsedMs`，此后所有时间读取都用冻结值。
 */
class TrainingEngine(
    private val clock: Clock,
    val params: TrainingParams,
) {

    private var startedAtElapsedMs: Long? = null
    private var pausedAtElapsedMs: Long? = null
    private var accumulatedPausedMs: Long = 0L

    /** 快照恢复带回的历史暂停时长（与 accumulatedPausedMs 分开，避免被重复扣减）*/
    private var restoredPausedMs: Long = 0L

    /** 主动结束后冻结的已用时长。null 表示未冻结。 */
    private var frozenElapsedMs: Long? = null

    var pauseCount: Int = 0
        private set

    /** 训练开始的墙钟时间，仅用于写库的 startedAt */
    var wallClockAtStartMs: Long? = null
        private set

    val status: TrainingStatus
        get() = when {
            frozenElapsedMs != null -> TrainingStatus.COMPLETE
            startedAtElapsedMs == null -> TrainingStatus.IDLE
            // 暂停优先于"自然完成"：暂停后再看状态应是 PAUSED
            pausedAtElapsedMs != null -> TrainingStatus.PAUSED
            // 时间线走完也算完成 —— 否则 status 与 frame().status 会不一致，
            // 调用方按 status 判断会漏掉自然结束
            elapsedActiveMs() >= params.totalActiveMs -> TrainingStatus.COMPLETE
            else -> TrainingStatus.RUNNING
        }

    /** 已用活动时长（已扣暂停）。IDLE 为 0；已结束后为冻结值。 */
    fun elapsedActiveMs(nowElapsedMs: Long = clock.nowMs()): Long {
        // 结束后必须返回冻结值：否则越过总时长后会变成"满组"，违反 AC-07
        frozenElapsedMs?.let { return it }

        val start = startedAtElapsedMs ?: return 0L
        val pausedAt = pausedAtElapsedMs
        val elapsed = if (pausedAt != null) {
            pausedAt - start - accumulatedPausedMs
        } else {
            nowElapsedMs - start - accumulatedPausedMs
        }
        return elapsed.coerceAtLeast(0L)
    }

    /** 累计暂停时长（含恢复前历史）。供 `SessionDraft.pausedMs` 使用。 */
    fun pausedMs(nowElapsedMs: Long = clock.nowMs()): Long {
        val current = pausedAtElapsedMs?.let { (nowElapsedMs - it).coerceAtLeast(0L) } ?: 0L
        return restoredPausedMs + accumulatedPausedMs + current
    }

    fun start(
        nowElapsedMs: Long = clock.nowMs(),
        wallClockMs: Long = System.currentTimeMillis(),
    ) {
        startedAtElapsedMs = nowElapsedMs
        pausedAtElapsedMs = null
        accumulatedPausedMs = 0L
        restoredPausedMs = 0L
        frozenElapsedMs = null
        pauseCount = 0
        wallClockAtStartMs = wallClockMs
    }

    /**
     * 暂停。幂等。
     *
     * 已结束或时间线已走完时不允许暂停 —— 否则状态会从 COMPLETE 变回 PAUSED，
     * 而需求 §5.3 的状态机里 Complete 没有"暂停"这条去向（评审 E3）。
     */
    fun pause(nowElapsedMs: Long = clock.nowMs()) {
        if (startedAtElapsedMs == null) return
        if (frozenElapsedMs != null) return
        if (pausedAtElapsedMs != null) return
        if (elapsedActiveMs(nowElapsedMs) >= params.totalActiveMs) return

        pausedAtElapsedMs = nowElapsedMs
        pauseCount++
    }

    /** 恢复。幂等；未暂停或已结束时安全返回。 */
    fun resume(nowElapsedMs: Long = clock.nowMs()) {
        if (frozenElapsedMs != null) return
        val pausedAt = pausedAtElapsedMs ?: return
        accumulatedPausedMs += (nowElapsedMs - pausedAt).coerceAtLeast(0L)
        pausedAtElapsedMs = null
    }

    /** 用户主动结束：**冻结时间**，保留已完成次数（AC-07）。 */
    fun stop(nowElapsedMs: Long = clock.nowMs()) {
        if (frozenElapsedMs != null) return
        frozenElapsedMs = elapsedActiveMs(nowElapsedMs)
        pausedAtElapsedMs = null
    }

    /** 当前帧。 */
    fun frame(nowElapsedMs: Long = clock.nowMs()): TrainingFrame {
        // 还没开始：直接给 IDLE 帧，不要算出"准备阶段"
        if (startedAtElapsedMs == null) return TrainingFrame.idle(params.repsPerSet)

        val active = elapsedActiveMs(nowElapsedMs)
        val computed = TrainingTimeline.frameAt(active, params)
        val displayStatus = when {
            // 主动结束（时间已冻结）优先显示完成
            frozenElapsedMs != null -> TrainingStatus.COMPLETE
            // 时间线自然走完
            computed.status == TrainingStatus.COMPLETE -> TrainingStatus.COMPLETE
            pausedAtElapsedMs != null -> TrainingStatus.PAUSED
            else -> TrainingStatus.RUNNING
        }
        return computed.copy(status = displayStatus)
    }

    /** 是否已结束（自然或主动）。 */
    fun isFinished(nowElapsedMs: Long = clock.nowMs()): Boolean =
        frozenElapsedMs != null ||
            TrainingTimeline.frameAt(elapsedActiveMs(nowElapsedMs), params).status ==
            TrainingStatus.COMPLETE

    // ── 快照与恢复（AC-13）──────────────────────────────────────────────

    /** 生成中断恢复快照。只存相对量，不存单调时钟绝对值（重启后无效）。 */
    fun snapshot(nowElapsedMs: Long = clock.nowMs()): EngineSnapshot? {
        if (startedAtElapsedMs == null) return null
        return EngineSnapshot(
            startedAtWallMs = wallClockAtStartMs ?: 0L,
            accumulatedPausedMs = pausedMs(nowElapsedMs),
            pauseCount = pauseCount,
            planVersion = params.hash(),
            plannedReps = params.repsPerSet,
            elapsedActiveMs = elapsedActiveMs(nowElapsedMs),
            wasPaused = pausedAtElapsedMs != null,
        )
    }

    /**
     * 从快照恢复。
     *
     * @return 参数版本不匹配时返回 false（调用方应提示"丢弃"而不是"继续"）
     */
    fun restore(snapshot: EngineSnapshot, nowElapsedMs: Long = clock.nowMs()): Boolean {
        if (snapshot.planVersion != params.hash()) return false

        startedAtElapsedMs = nowElapsedMs - snapshot.elapsedActiveMs
        accumulatedPausedMs = 0L
        // 历史暂停时长单独保存，不能塞进 accumulatedPausedMs —— 那会被再扣一遍
        restoredPausedMs = snapshot.accumulatedPausedMs
        pauseCount = snapshot.pauseCount
        frozenElapsedMs = null
        // 墙钟必须回填：否则再次 snapshot() 拿到 0，
        // 写库时命中 require(startedAt >= MIN_EPOCH_MS) 直接崩（评审 E2）
        wallClockAtStartMs = snapshot.startedAtWallMs
        pausedAtElapsedMs = if (snapshot.wasPaused) nowElapsedMs else null
        return true
    }
}