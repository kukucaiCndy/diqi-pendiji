package com.diqi.demo.domain.training

import com.diqi.demo.core.time.TestClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 训练引擎测试（AC-02 暂停冻结/恢复无跳变、AC-07 不补造、AC-13 中断恢复）。
 *
 * ## 这些测试专门针对评审指出的三条必然失败路径
 *
 * 评审（架构 reviewer）指出「tick 推进状态」在以下场景必然出错：
 *   1. tick 累加漂移（`delay(100)` 实际 100–150ms）
 *   2. 暂停边界（`lastTickAt` 未重置 → 一次 tick 跨多个阶段）
 *   3. 锁屏休眠（`delay` 晚醒，clamp 与不 clamp 都错）
 *
 * 纯函数时间线（D11）下这三条都不存在，但**必须有测试证明**。
 * 所以这里刻意用"单次大跳跃"来模拟锁屏 30s，而不是循环 tick 100 次。
 */
class TrainingEngineTest {

    private val params = TrainingParams.DEFAULT // prep 5s + 8×(5+5+5)s = 125s
    private lateinit var clock: TestClock
    private lateinit var engine: TrainingEngine

    private fun newEngine(p: TrainingParams = params): TrainingEngine {
        clock = TestClock(1_000_000L)
        return TrainingEngine(clock, p).also { engine = it }
    }

    // 便利常量（毫秒）
    private val prep = 5_000L
    private val cycle = 15_000L
    private val contract = 5_000L
    private val hold = 5_000L

    // ── 阶段推进 ──────────────────────────────────────────────────────────

    @Test
    fun 未开始时为_IDLE() {
        val e = newEngine()
        assertEquals(TrainingStatus.IDLE, e.status)
        assertEquals(0L, e.elapsedActiveMs())
        assertEquals(TrainingPhase.IDLE, e.frame().phase)
    }

    @Test
    fun 开始后进入准备阶段且剩余等于准备时长() {
        val e = newEngine()
        e.start()
        val f = e.frame()
        assertEquals(TrainingStatus.RUNNING, f.status)
        assertEquals(TrainingPhase.PREPARATION, f.phase)
        assertEquals(prep, f.phaseRemainingMs)
        assertEquals(prep, f.phaseTotalMs)
        assertEquals(0, f.completedReps)
    }

    @Test
    fun 四个阶段按时长依次推进() {
        val e = newEngine()
        e.start()

        fun at(ms: Long) = e.frame(clock.current + ms)

        assertEquals(TrainingPhase.PREPARATION, at(0).phase)
        assertEquals(TrainingPhase.CONTRACT, at(prep).phase)
        assertEquals(TrainingPhase.HOLD, at(prep + contract).phase)
        assertEquals(TrainingPhase.RELAX, at(prep + contract + hold).phase)
        assertEquals(TrainingPhase.CONTRACT, at(prep + cycle).phase)
        assertEquals(2, at(prep + cycle).repIndex)
    }

    @Test
    fun 倒计时文本为两位分秒() {
        val e = newEngine()
        e.start()
        assertEquals("00:05", e.frame().countdownText)
        assertEquals("00:01", e.frame(clock.current + 4_000L).countdownText)
    }

    // ── AC-02：暂停冻结与恢复无跳变 ──────────────────────────────────────

    @Test
    fun 暂停期间时间冻结() {
        val e = newEngine()
        e.start()
        clock.advance(prep + 1_000L) // 收缩第 1 秒
        val before = e.frame()

        e.pause()
        clock.advance(10_000L) // 暂停 10 秒

        val during = e.frame()
        assertEquals(TrainingStatus.PAUSED, during.status)
        assertEquals("暂停中剩余时间不得变化", before.phaseRemainingMs, during.phaseRemainingMs)
        assertEquals("暂停中已用时长不得增长", prep + 1_000L, e.elapsedActiveMs())
    }

    @Test
    fun 恢复后从暂停点继续而不是跳跃() {
        val e = newEngine()
        e.start()
        clock.advance(prep + 1_000L)
        val before = e.frame()

        e.pause()
        clock.advance(10_000L)
        e.resume()

        val after = e.frame()
        assertEquals(TrainingStatus.RUNNING, after.status)
        assertEquals("恢复后阶段必须保持不变（AC-02 无跳变）", before.phase, after.phase)
        assertEquals("恢复后剩余时间必须保持不变", before.phaseRemainingMs, after.phaseRemainingMs)
        assertEquals("已用时长不应因暂停而增加", prep + 1_000L, e.elapsedActiveMs())

        // 再走 1 秒，应正常推进且只推进 1 秒
        clock.advance(1_000L)
        assertEquals(before.phaseRemainingMs - 1_000L, e.frame().phaseRemainingMs)
    }

    @Test
    fun 暂停次数只记一次_重复暂停幂等() {
        val e = newEngine()
        e.start()
        e.pause()
        e.pause()
        e.pause()
        assertEquals(1, e.pauseCount)
        e.resume()
        e.resume()
        assertEquals(TrainingStatus.RUNNING, e.status)
    }

    // ── 锁屏：单次大跳跃 ────────────────────────────────────────────────

    @Test
    fun 一次跳跃三十秒后状态与已用时长都正确() {
        // 模拟锁屏 30s 后回来：不循环 tick，直接跳跃 30s
        val e = newEngine()
        e.start()
        clock.advance(30_000L)
        val f = e.frame()

        assertEquals("已用时长必须精确等于 30s（elapsedRealtime 含深睡）", 30_000L, e.elapsedActiveMs())
        // 30s = prep(5s) + 第 1 组(15s) + 第 2 组的前 10s（收缩 5s + 保持 5s）
        // 所以 30s 恰好是第 2 组「放松」的起点（边界属于后一个阶段）
        assertEquals(TrainingPhase.RELAX, f.phase)
        assertEquals(1, f.completedReps)
        assertEquals(2, f.repIndex)

        // 再验证边界前一刻确实是「保持」
        assertEquals(TrainingPhase.HOLD, e.frame(clock.current - 1L).phase)
        // 且已完成次数在边界处正确翻转
        assertEquals(1, e.frame(clock.current).completedReps)
    }

    @Test
    fun 跳跃超过总时长直接完成() {
        val e = newEngine()
        e.start()
        clock.advance(params.totalActiveMs + 60_000L)
        val f = e.frame()
        assertEquals(TrainingStatus.COMPLETE, f.status)
        assertEquals(params.repsPerSet, f.completedReps)
        assertTrue(e.isFinished())
    }

    // ── AC-07：只计整组，不补造 ──────────────────────────────────────────

    @Test
    fun 第三次收缩中结束时完成次数为二() {
        val e = newEngine()
        e.start()
        // 第 3 次收缩进行中：prep + 2 个完整循环 + 2 秒
        clock.advance(prep + cycle * 2 + 2_000L)
        assertEquals(3, e.frame().repIndex)
        assertEquals("第 3 次还没走完，完成次数应为 2", 2, e.frame().completedReps)

        e.stop()
        val f = e.frame()
        assertEquals(TrainingStatus.COMPLETE, f.status)
        assertEquals("结束后不得补造成 3", 2, f.completedReps)
    }

    @Test
    fun 阶段边界上完成次数按整组计算() {
        // 刚好走完第 2 组（relax 结束处）
        assertEquals(0, TrainingTimeline.completedRepsAt(prep - 1, params))
        assertEquals(0, TrainingTimeline.completedRepsAt(prep, params))
        assertEquals(0, TrainingTimeline.completedRepsAt(prep + cycle - 1, params))
        assertEquals(1, TrainingTimeline.completedRepsAt(prep + cycle, params))
        assertEquals(2, TrainingTimeline.completedRepsAt(prep + cycle * 2, params))
        assertEquals(
            params.repsPerSet,
            TrainingTimeline.completedRepsAt(params.totalActiveMs, params),
        )
    }

    // ── AC-13：中断恢复（跨进程）─────────────────────────────────────────

    @Test
    fun 快照只保存相对量_不保存单调时钟绝对值() {
        val e = newEngine()
        e.start()
        clock.advance(prep + 3_000L)
        val snap = e.snapshot()!!

        assertEquals(prep + 3_000L, snap.elapsedActiveMs)
        assertEquals(params.hash(), snap.planVersion)
        assertFalse(snap.wasPaused)
    }

    @Test
    fun 进程重启后恢复_剩余时间不丢也不为负() {
        val e = newEngine()
        e.start()
        clock.advance(prep + 3_000L)
        val beforeRemaining = e.frame().phaseRemainingMs
        val snap = e.snapshot()!!

        // 模拟进程重启：单调时钟归零
        val freshClock = TestClock(0L)
        val restored = TrainingEngine(freshClock, params)
        assertTrue(restored.restore(snap, freshClock.nowMs()))

        val f = restored.frame()
        assertEquals(
            "恢复后阶段与剩余时间必须与快照时一致",
            beforeRemaining,
            f.phaseRemainingMs,
        )
        assertEquals(prep + 3_000L, restored.elapsedActiveMs())
        assertTrue("已用时长不得为负", restored.elapsedActiveMs() > 0)
    }

    @Test
    fun 暂停中快照恢复后仍为暂停() {
        val e = newEngine()
        e.start()
        clock.advance(prep)
        e.pause()
        val snap = e.snapshot()!!
        assertTrue(snap.wasPaused)

        val freshClock = TestClock(0L)
        val restored = TrainingEngine(freshClock, params)
        assertTrue(restored.restore(snap, 0L))
        assertEquals(TrainingStatus.PAUSED, restored.status)
    }

    @Test
    fun 参数版本不一致时拒绝恢复() {
        val e = newEngine()
        e.start()
        clock.advance(prep)
        val snap = e.snapshot()!!

        // 换了一套参数（例如临床调整了收缩时长）
        val other = TrainingEngine(TestClock(0L), params.copy(contractSec = 8))
        assertFalse("参数版本不同必须拒绝恢复", other.restore(snap, 0L))
    }

    @Test
    fun 未开始时没有快照() {
        assertNull(newEngine().snapshot())
    }

    @Test
    fun 快照可恢复性判定() {
        val e = newEngine()
        e.start()
        clock.advance(1_000L)
        assertFalse("只练了 1 秒不值得提示继续", e.snapshot()!!.isResumable(params))

        clock.advance(5_000L)
        assertTrue(e.snapshot()!!.isResumable(params))

        clock.advance(params.totalActiveMs)
        assertFalse("已跑完不需要恢复", e.snapshot()!!.isResumable(params))
    }

    // ── 参数化 ────────────────────────────────────────────────────────────

    @Test
    fun 演示参数三次一组的总时长正确() {
        val demo = params.forDemo()
        assertEquals(3, demo.repsPerSet)
        assertEquals(prep + demo.cycleMs * 3, demo.totalActiveMs)
    }

    // ── 评审 E1：stop() 必须冻结时间，否则会被补造成满组 ────────────────

    @Test
    fun 结束后继续走时间_已完成次数不得被补成满组() {
        val e = newEngine()
        e.start()
        clock.advance(prep + cycle * 2 + 2_000L) // 第 3 次收缩中
        e.stop()
        assertEquals(2, e.frame().completedReps)

        // 关键：结束后时间仍在流逝（页面还在轮询 frame）
        clock.advance(300_000L)
        assertEquals(
            "结束后 elapsedActiveMs 必须冻结，否则越过总时长会变成满组（违反 AC-07）",
            prep + cycle * 2 + 2_000L,
            e.elapsedActiveMs(),
        )
        assertEquals(2, e.frame().completedReps)
        assertEquals(TrainingStatus.COMPLETE, e.frame().status)
    }

    @Test
    fun 结束后再暂停或恢复都无效() {
        val e = newEngine()
        e.start()
        clock.advance(prep + 1_000L)
        e.stop()
        val frozen = e.elapsedActiveMs()
        e.pause()
        e.resume()
        assertEquals(frozen, e.elapsedActiveMs())
        assertEquals(0, e.pauseCount)
    }

    // ── 评审 E3：自然完成后不能再被暂停成 PAUSED ─────────────────────────

    @Test
    fun 自然完成后点击暂停无效_状态保持完成() {
        val e = newEngine()
        e.start()
        clock.advance(params.totalActiveMs + 1_000L)
        assertEquals(TrainingStatus.COMPLETE, e.status)

        e.pause()
        assertEquals("完成后不应变成 PAUSED", TrainingStatus.COMPLETE, e.status)
        assertEquals("不应多记一次暂停", 0, e.pauseCount)
    }

    @Test
    fun 自然完成后暂停次数不会被污染() {
        val e = newEngine()
        e.start()
        clock.advance(prep)
        e.pause()
        clock.advance(2_000L)
        e.resume()
        clock.advance(params.totalActiveMs)
        e.pause() // 此时已完成
        assertEquals(1, e.pauseCount)
    }

    // ── 评审 E2：恢复后墙钟与历史暂停时长不能丢 ─────────────────────────

    @Test
    fun 恢复后再次取快照_墙钟时间不为零() {
        val e = newEngine()
        val wall = 1_700_000_000_000L
        e.start(wallClockMs = wall)
        clock.advance(prep)
        val snap = e.snapshot()!!

        val freshClock = TestClock(0L)
        val restored = TrainingEngine(freshClock, params)
        assertTrue(restored.restore(snap, 0L))

        val snap2 = restored.snapshot(0L)!!
        assertEquals(
            "恢复后墙钟必须回填，否则写库会因 startedAt 过小而崩",
            wall,
            snap2.startedAtWallMs,
        )
    }

    @Test
    fun 恢复后历史暂停时长不丢也不被重复扣除() {
        val e = newEngine()
        e.start()
        clock.advance(prep)
        e.pause()
        clock.advance(4_000L) // 暂停 4 秒
        e.resume()
        clock.advance(2_000L)
        assertEquals(4_000L, e.pausedMs())
        val snap = e.snapshot()!!

        val freshClock = TestClock(0L)
        val restored = TrainingEngine(freshClock, params)
        assertTrue(restored.restore(snap, 0L))

        assertEquals("历史暂停时长必须带回", 4_000L, restored.pausedMs(0L))
        assertEquals("已用活动时长不含暂停", prep + 2_000L, restored.elapsedActiveMs(0L))
    }

    @Test
    fun 暂停中查询暂停时长为累计值() {
        val e = newEngine()
        e.start()
        clock.advance(prep)
        e.pause()
        clock.advance(3_000L)
        assertEquals(3_000L, e.pausedMs())
    }

    @Test
    fun 单组参数下总时长与阶段划分一致() {
        val p = TrainingParams(prepSec = 5, contractSec = 5, holdSec = 5, relaxSec = 5, repsPerSet = 3)
        val e = TrainingEngine(TestClock(0L), p)
        e.start(0L)
        // 走完 3 组
        val f = e.frame(p.totalActiveMs)
        assertEquals(TrainingStatus.COMPLETE, f.status)
        assertEquals(3, f.completedReps)
        assertEquals(1f, f.overallProgress, 0.0001f)
    }
}