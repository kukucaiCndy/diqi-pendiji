package com.diqi.demo.sensor

import com.diqi.demo.data.db.MissingReason
import com.diqi.demo.data.db.QualityState
import com.facephys.rppg.MetricsRead
import com.facephys.rppg.RppgMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RppgSensor 纯函数映射的 JVM 单测。
 *
 * 覆盖要点：
 *  · [mapSnapshot] 三态映射（Ok→VALID / NoSignal→NO_DATA / NotReady→NO_DATA）
 *  · faceLostMs / lightOk 透传
 *  · isSimulated 恒为 false（真实源不带水印）
 *  · [chooseSensorKind] 四分支（demo / camera+key / camera 无 key / 无 camera）
 */
class RppgSensorTest {

    // ── mapSnapshot 三态映射 ─────────────────────────────────────────────

    @Test
    fun `Ok 映射为 VALID 且透传 hr_rr_sqi`() {
        val read = MetricsRead.Ok(
            RppgMetrics(
                heartRateBpm = 72f,
                breathingRatePerMin = 16f,
                sqi = 0.85f,
                timestampMs = 1000L,
            ),
        )
        val snap = mapSnapshot(read, nowElapsedSec = 35, faceLostMs = 0L, lightOk = true)
        assertEquals(QualityState.VALID, snap.state)
        assertEquals(72f, snap.heartRateBpm)
        assertEquals(16f, snap.breathingRatePerMin)
        assertEquals(0.85f, snap.sqi)
        assertEquals(MissingReason.NONE, snap.missingReason)
    }

    @Test
    fun `Ok 时 isSimulated 恒为 false`() {
        val read = MetricsRead.Ok(
            RppgMetrics(72f, 16f, 0.85f, 1000L),
        )
        val snap = mapSnapshot(read, 35, 0L, true)
        assertFalse("真实源绝不带 isSimulated 标记", snap.isSimulated)
    }

    @Test
    fun `NoSignal 映射为 NO_DATA`() {
        val snap = mapSnapshot(MetricsRead.NoSignal, 35, 0L, true)
        assertEquals(QualityState.NO_DATA, snap.state)
        assertNull("NoSignal 不应有心率", snap.heartRateBpm)
        assertNull("NoSignal 不应有呼吸率", snap.breathingRatePerMin)
    }

    @Test
    fun `NotReady 映射为 NO_DATA 且 elapsedSec 透传`() {
        val snap = mapSnapshot(MetricsRead.NotReady, 10, 0L, true)
        assertEquals(QualityState.NO_DATA, snap.state)
        assertEquals("建立中秒数必须透传给 QualityGate 判信号建立", 10, snap.elapsedSec)
    }

    // ── faceLostMs / lightOk 透传 ────────────────────────────────────────

    @Test
    fun `faceLostMs 透传到快照`() {
        val snap = mapSnapshot(MetricsRead.NoSignal, 35, faceLostMs = 4000L, lightOk = true)
        assertEquals("人脸丢失毫秒必须由数据源真实累计", 4000L, snap.faceLostMs)
        assertFalse("faceLostMs>0 时 faceInFrame 应为 false", snap.faceInFrame)
    }

    @Test
    fun `faceLostMs 为 0 时 faceInFrame 为 true`() {
        val snap = mapSnapshot(MetricsRead.Ok(
            RppgMetrics(72f, 16f, 0.85f, 1000L),
        ), 35, faceLostMs = 0L, lightOk = true)
        assertTrue(snap.faceInFrame)
    }

    @Test
    fun `lightOk 透传到快照`() {
        val snap = mapSnapshot(MetricsRead.NoSignal, 35, 0L, lightOk = false)
        assertFalse("光照不足必须透传", snap.lightOk)
    }

    @Test
    fun `三态都透传 faceLostMs 和 lightOk`() {
        val ok = MetricsRead.Ok(RppgMetrics(72f, 16f, 0.85f, 1000L))
        listOf(ok, MetricsRead.NoSignal, MetricsRead.NotReady).forEach { read ->
            val snap = mapSnapshot(read, 35, faceLostMs = 2000L, lightOk = false)
            assertEquals(2000L, snap.faceLostMs)
            assertFalse(snap.lightOk)
        }
    }

    // ── isSimulated 恒 false（三态）──────────────────────────────────────

    @Test
    fun `三态都 isSimulated=false`() {
        val ok = MetricsRead.Ok(RppgMetrics(72f, 16f, 0.85f, 1000L))
        listOf(ok, MetricsRead.NoSignal, MetricsRead.NotReady).forEach { read ->
            val snap = mapSnapshot(read, 35, 0L, true)
            assertFalse("真实源三态都不带水印标记", snap.isSimulated)
        }
    }

    // ── chooseSensorKind 四分支 ──────────────────────────────────────────

    @Test
    fun `demoMode 选择 DEMO`() {
        assertEquals(SensorKind.DEMO, chooseSensorKind(demoMode = true, cameraEnabled = true, appKey = "key"))
        assertEquals(SensorKind.DEMO, chooseSensorKind(demoMode = true, cameraEnabled = false, appKey = ""))
    }

    @Test
    fun `cameraEnabled 且 appKey 非空选择 REAL`() {
        assertEquals(
            "有相机且有 key → 真实源",
            SensorKind.REAL,
            chooseSensorKind(demoMode = false, cameraEnabled = true, appKey = "real-key"),
        )
    }

    @Test
    fun `cameraEnabled 但 appKey 为空选择 NULL（无 key 不造假）`() {
        assertEquals(
            "有相机但无 key → 绝不出假数（FR-08/AC-14）",
            SensorKind.NULL,
            chooseSensorKind(demoMode = false, cameraEnabled = true, appKey = ""),
        )
    }

    @Test
    fun `无 camera 选择 NULL`() {
        assertEquals(
            "无相机 → NullSensor",
            SensorKind.NULL,
            chooseSensorKind(demoMode = false, cameraEnabled = false, appKey = "key"),
        )
        assertEquals(
            "无相机无 key → NullSensor",
            SensorKind.NULL,
            chooseSensorKind(demoMode = false, cameraEnabled = false, appKey = ""),
        )
    }

    @Test
    fun `demoMode 优先级最高（即使有相机有 key 也选 DEMO）`() {
        assertEquals(
            "演示模式优先于真实路径",
            SensorKind.DEMO,
            chooseSensorKind(demoMode = true, cameraEnabled = true, appKey = "key"),
        )
    }
}
