package com.diqi.demo.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.diqi.demo.data.db.HerRhythmDb
import com.diqi.demo.data.db.LeakContext
import com.diqi.demo.data.db.LeakFrequency
import com.diqi.demo.data.db.MissingReason
import com.diqi.demo.data.db.QualityState
import com.diqi.demo.data.db.SensorMode
import com.diqi.demo.data.repo.ProgressRepository
import com.diqi.demo.data.repo.QualitySummary
import com.diqi.demo.data.repo.SessionDraft
import com.diqi.demo.data.repo.SessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

/**
 * **真实数据库**上的 DAO / 仓储测试（in-memory Room）。
 *
 * ## 为什么必须有这一层
 *
 * 评审指出：S2 之前所有测试都是纯函数与源码静态检查，
 * DAO/Repository 的真实 SQL **一次都没被执行过**。于是三个缺陷可以长期存活：
 *   · `REPLACE` 触发外键级联，自评回填会静默删掉质量行（A1）
 *   · 覆盖率门限只做数值比较、不校验数据源（B3）
 *   · 「仅统计 ≥60s」只写在注释里（D1）
 * 静态检查拦不住这类问题 —— 它们只有真跑 SQL 才会暴露。
 *
 * 用 `inMemoryDatabaseBuilder`：不需要清理磁盘，也不会污染真机数据。
 */
@RunWith(AndroidJUnit4::class)
class HerRhythmDaoTest {

    private lateinit var db: HerRhythmDb
    private lateinit var sessions: SessionRepository
    private lateinit var progress: ProgressRepository

    private val zone = ZoneId.systemDefault()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HerRhythmDb::class.java,
        ).allowMainThreadQueries().build()
        sessions = SessionRepository(db)
        progress = ProgressRepository(db, zone)
    }

    @After
    fun tearDown() = db.close()

    // ── 工具 ──────────────────────────────────────────────────────────────

    private fun nowMs() = System.currentTimeMillis()

    private fun draft(
        id: String = "s-1",
        startedAt: Long = nowMs() - 120_000,
        durationMs: Long = 120_000,
        mode: SensorMode = SensorMode.OFF,
        coverage: Double? = null,
        avgHr: Double? = null,
        quality: QualitySummary? = null,
    ) = SessionDraft(
        id = id,
        startedAt = startedAt,
        endedAt = startedAt + durationMs,
        durationMs = durationMs,
        planVersion = "deadbeef",
        plannedReps = 8,
        completedReps = 8,
        pauseCount = 0,
        pausedMs = 0,
        mode = mode,
        signalCoverage = coverage,
        avgHr = avgHr,
        avgBreathingRate = if (coverage != null) 16.0 else null,
        avgSqi = if (coverage != null) 0.8 else null,
        quality = quality,
    )

    private fun quality(valid: Double = 0.8) = QualitySummary(
        lastState = QualityState.VALID,
        validRatio = valid,
        degradedRatio = 0.0,
        invalidRatio = 1.0 - valid,
        missingReason = MissingReason.NONE,
    )

    // ── A1：REPLACE 级联删除 ──────────────────────────────────────────────

    @Test
    fun 自评回填不会删掉质量汇总行() = runTest {
        sessions.save(draft(quality = quality()))
        assertNotNull("质量行应已写入", db.sensorQualityDao().findBySession("s-1"))

        sessions.attachSelfAssessment("s-1", difficulty = 2, discomfort = false)

        val after = db.sensorQualityDao().findBySession("s-1")
        assertNotNull(
            "自评回填后质量行消失了 —— 说明 upsert 用了 REPLACE，" +
                "触发了 sensor_quality 的 ON DELETE CASCADE（评审 A1）",
            after,
        )
        assertEquals(0.8, after!!.validRatio, 0.001)
        assertEquals(2, sessions.findById("s-1")?.difficulty)
    }

    @Test
    fun 自评回填不会改变其他字段() = runTest {
        sessions.save(draft(durationMs = 180_000, coverage = 0.9, avgHr = 71.0, mode = SensorMode.OFFLINE_SDK))
        val before = sessions.findById("s-1")!!
        sessions.attachSelfAssessment("s-1", difficulty = 3, discomfort = true)
        val after = sessions.findById("s-1")!!
        assertEquals(before.startedAt, after.startedAt)
        assertEquals(before.durationMs, after.durationMs)
        assertEquals(before.planVersion, after.planVersion)
        assertEquals(1, after.discomfort)
    }

    // ── B3 / FR-09：覆盖率门限与数据源 ───────────────────────────────────

    @Test
    fun 覆盖率不足时指标必须置空() = runTest {
        val saved = sessions.save(
            draft(coverage = 0.5, avgHr = 78.0, mode = SensorMode.OFFLINE_SDK),
        )
        assertNull("覆盖率 0.5 < 0.6，avgHr 必须为空", saved.avgHr)
        assertNull(saved.avgBreathingRate)
        assertNull(saved.avgSqi)
        // 但覆盖率本身要保留（用于记录页展示"本次信号覆盖 50%"）
        assertEquals(0.5, saved.signalCoverage!!, 0.001)
    }

    @Test
    fun 覆盖率达标时指标保留() = runTest {
        val saved = sessions.save(
            draft(coverage = 0.85, avgHr = 72.0, mode = SensorMode.OFFLINE_SDK),
        )
        assertEquals(72.0, saved.avgHr!!, 0.001)
    }

    @Test
    fun 非真实感知模式下即使覆盖率达标也不允许带指标() = runTest {
        // 演示模式/未启用都不该出现心率 —— 这是 FR-08 与 AC-19 的交叉红线
        listOf(SensorMode.SIMULATED, SensorMode.OFF).forEach { mode ->
            val saved = sessions.save(
                draft(id = "m-$mode", coverage = 0.95, avgHr = 80.0, mode = mode),
            )
            assertNull("mode=$mode 不应带 avgHr", saved.avgHr)
        }
    }

    @Test
    fun 覆盖率越界会被夹到0到1之间() = runTest {
        val saved = sessions.save(
            draft(coverage = 60.0, avgHr = 70.0, mode = SensorMode.OFFLINE_SDK),
        )
        assertEquals("传百分比 60 应被夹到 1.0", 1.0, saved.signalCoverage!!, 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun startedAt_必须是墙钟毫秒_单调时钟塞进来会直接报错() = runTest {
        sessions.save(draft(startedAt = 12_345L)) // 形如 elapsedRealtime 的值
    }

    @Test(expected = IllegalArgumentException::class)
    fun 质量占比之和不为1时拒绝写入() = runTest {
        sessions.save(
            draft(quality = quality().copy(validRatio = 0.5, invalidRatio = 0.2)),
        )
    }

    // ── AC-08：幂等 ───────────────────────────────────────────────────────

    @Test
    fun 同一id重复保存只有一行() = runTest {
        val d = draft(id = "dup")
        sessions.save(d)
        sessions.save(d)
        sessions.save(d)
        assertEquals(1, db.trainingSessionDao().countAll())
    }

    // ── AC-19：演示隔离 ──────────────────────────────────────────────────

    @Test
    fun 演示记录不进列表但可单独读取() = runTest {
        sessions.save(draft(id = "real-1"))
        sessions.save(draft(id = "sim-1", mode = SensorMode.SIMULATED))

        val real = sessions.recentReal(10).map { it.id }
        assertEquals(listOf("real-1"), real)

        val sim = sessions.simulatedSessions(10).map { it.id }
        assertEquals(listOf("sim-1"), sim)

        assertEquals(2, db.trainingSessionDao().countAll())
    }

    @Test
    fun 演示记录不计入打卡与周报统计() = runTest {
        val now = nowMs()
        sessions.save(draft(id = "real-1", startedAt = now - 60_000, durationMs = 600_000))
        sessions.save(
            draft(id = "sim-1", startedAt = now - 120_000, durationMs = 600_000, mode = SensorMode.SIMULATED),
        )

        val week = progress.lastSevenDays(now)
        assertEquals("打卡只应有 1 次真实训练", 1, week.sessionCount)
        assertEquals(600L, week.totalSec)
    }

    // ── D1：60 秒时长门槛 ────────────────────────────────────────────────

    @Test
    fun 短于60秒的误触不计入统计() = runTest {
        val now = nowMs()
        sessions.save(draft(id = "short", startedAt = now - 30_000, durationMs = 30_000))
        sessions.save(draft(id = "long", startedAt = now - 200_000, durationMs = 120_000))

        val week = progress.lastSevenDays(now)
        assertEquals("误触不应计入", 1, week.sessionCount)
        assertEquals(120L, week.totalSec)
        assertTrue(progress.hasTrainedToday(now))
    }

    @Test
    fun 只有误触时_今天等同于未训练_且Day进度不起算() = runTest {
        val now = nowMs()
        sessions.save(draft(id = "short", startedAt = now - 10_000, durationMs = 10_000))

        assertTrue("仅误触不应让今日变成已完成", !progress.hasTrainedToday(now))
        assertNull("仅误触不应让 Day 起算", sessions.firstRealStartAt())
        assertEquals(1, progress.currentDayIndex(now))
    }

    // ── FR-11 / AC-20：Day 进度链路 ──────────────────────────────────────

    @Test
    fun Day进度按首次有效完成日起算() = runTest {
        val zoneLocal = zone
        val today = LocalDate.now(zoneLocal)
        val firstDayStart = today.minusDays(1).atTime(20, 0)
            .atZone(zoneLocal).toInstant().toEpochMilli()

        sessions.save(draft(id = "d1", startedAt = firstDayStart, durationMs = 200_000))

        assertEquals(
            "昨天首次完成 → 今天应为 Day 2",
            2,
            progress.currentDayIndex(System.currentTimeMillis()),
        )
        assertTrue(!progress.isCycleComplete(System.currentTimeMillis()))
    }

    // ── FR-12 / AC-23：漏尿日记三态 ──────────────────────────────────────

    @Test
    fun 跳过不写频次且可区分于未填写() = runTest {
        val now = nowMs()
        val repo = com.diqi.demo.data.repo.LeakDiaryRepository(db, zone)
        assertEquals("初始应没有任何行（= 用户没接触过）", 0, repo.countAll())

        repo.skip(now)
        assertEquals(1, repo.countAll())
        assertEquals(0, repo.countAnswered())
        assertEquals(1, repo.countSkipped())

        val row = repo.findByDay(now)!!
        assertNull("跳过时不得编造频次", row.frequency)
        assertTrue(row.skipped)
    }

    @Test
    fun 作答写出频次与情境且重复提交覆盖同一天() = runTest {
        val now = nowMs()
        val repo = com.diqi.demo.data.repo.LeakDiaryRepository(db, zone)
        repo.save(now, LeakFrequency.ONCE, LeakContext.COUGH)
        repo.save(now, LeakFrequency.TWO_TO_THREE, LeakContext.SPORT)

        assertEquals("一天一条", 1, repo.countAll())
        val row = repo.findByDay(now)!!
        assertEquals(LeakFrequency.TWO_TO_THREE, row.leakFrequency)
        assertEquals(LeakContext.SPORT, row.leakContext)
    }

    @Test(expected = IllegalArgumentException::class)
    fun 跳过却带频次会被不变式拦住() {
        com.diqi.demo.data.db.LeakDiaryEntity(
            id = "x",
            date = 1L,
            frequency = "once",
            context = null,
            skipped = true,
            createdAt = 0L,
        )
    }

    // ── 质量聚合的 JOIN 过滤 ──────────────────────────────────────────────

    @Test
    fun 质量均值只统计真实会话() = runTest {
        val now = nowMs()
        sessions.save(
            draft(id = "r1", startedAt = now - 60_000, quality = quality(valid = 0.8), mode = SensorMode.OFFLINE_SDK),
        )
        sessions.save(
            draft(id = "s1", startedAt = now - 90_000, quality = quality(valid = 0.1), mode = SensorMode.SIMULATED),
        )

        val avg = progress.averageValidRatio(now)
        assertEquals("演示会话的 0.1 不得参与均值", 0.8, avg!!, 0.001)
    }

    // ── 清理 ──────────────────────────────────────────────────────────────

    @Test
    fun 清空训练数据同时清掉质量行() = runTest {
        sessions.save(draft(id = "a", quality = quality()))
        sessions.clearAll()
        assertEquals(0, db.trainingSessionDao().countAll())
        assertNull(db.sensorQualityDao().findBySession("a"))
    }

    @Test
    fun 清除种子不影响真实记录() = runTest {
        sessions.save(draft(id = "seed-day-x"))
        sessions.save(draft(id = "real-1"))
        sessions.clearSeeds()
        val remaining = sessions.recentReal(10).map { it.id }
        assertEquals(listOf("real-1"), remaining)
    }
}