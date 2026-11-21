package com.diqi.demo.domain.quality

import com.diqi.demo.data.db.MissingReason
import com.diqi.demo.data.db.QualityState
import com.diqi.demo.sensor.SensorSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 质量四态判定测试（FR-08）。
 *
 * ## 这些测试守住的是"绝不显示伪造数值"
 *
 * 需求 §5.6 的红线是「无数据时显示数字」= 禁止。四态判定是这条红线的
 * 唯一执行点，所以每条补充规则都必须有对应断言：
 * - 前 30 秒不出数字（AC-15）
 * - 人脸丢失 <3s 不切状态、≥3s 判 NO_DATA（AC-16）
 * - SQI 低于门限判 INVALID 而不是 VALID
 * - 光照不足降 DEGRADED 但**仍然显示可靠项**
 */
class QualityGateTest {

    private fun snap(
        elapsedSec: Int = 40,
        hr: Float? = 72f,
        br: Float? = null,
        sqi: Float? = 0.8f,
        reason: MissingReason = MissingReason.NONE,
        lightOk: Boolean = true,
    ) = SensorSnapshot(
        state = QualityState.VALID,
        heartRateBpm = hr,
        breathingRatePerMin = br,
        sqi = sqi,
        missingReason = reason,
        elapsedSec = elapsedSec,
        faceInFrame = true,
        lightOk = lightOk,
    )

    @Test
    fun 未启用感知时为_NO_DATA_且不出数字() {
        val v = QualityGate.evaluate(SensorSnapshot.NOT_ENABLED)
        assertEquals(QualityState.NO_DATA, v.state)
        assertFalse(v.showNumbers)
        assertEquals(MissingReason.NOT_ENABLED, v.reason)
    }

    @Test
    fun 前三十秒只显示信号建立中_一个数字都不出() {
        listOf(0, 1, 15, 29).forEach { sec ->
            val v = QualityGate.evaluate(snap(elapsedSec = sec))
            assertFalse("第 $sec 秒不应显示数字（AC-15）", v.showNumbers)
            assertEquals(QualityState.NO_DATA, v.state)
        }
        // 第 30 秒开始允许
        assertTrue(QualityGate.evaluate(snap(elapsedSec = 30)).showNumbers)
    }

    @Test
    fun 演示模式缩短建立期_hrReadySec_3_时3秒后即出HR() {
        // 演示模式传 hrReadySec=3（DemoSensor.HR_READY_SEC），3s 后即可出数
        listOf(0, 1, 2).forEach { sec ->
            val v = QualityGate.evaluate(snap(elapsedSec = sec), hrReadySec = 3)
            assertFalse("第 $sec 秒不应显示数字（演示模式 3s 建立期）", v.showNumbers)
            assertEquals(QualityState.NO_DATA, v.state)
        }
        // 第 3 秒开始允许
        assertTrue(
            "演示模式 3s 建立期后应允许显示数字",
            QualityGate.evaluate(snap(elapsedSec = 3), hrReadySec = 3).showNumbers,
        )
    }

    @Test
    fun 人脸丢失不足三秒只做半透明_不切换状态() {
        val v = QualityGate.evaluate(snap(), faceLostMs = 1_200L)
        assertEquals("短暂丢失不应切状态（AC-16 反面）", QualityState.VALID, v.state)
        assertTrue("但应允许半透明提示", v.dimmed)
        assertTrue(v.showNumbers)
    }

    @Test
    fun 人脸丢失达到三秒判_NO_DATA_且不出数字() {
        val v = QualityGate.evaluate(snap(), faceLostMs = 3_000L)
        assertEquals(QualityState.NO_DATA, v.state)
        assertFalse(v.showNumbers)
        assertEquals(MissingReason.FACE_LOST, v.reason)
        assertTrue("应给出可解释的提示", v.banner != null)
    }

    @Test
    fun SQI_低于门限判_INVALID_而不是_VALID() {
        val v = QualityGate.evaluate(snap(sqi = 0.3f))
        assertEquals(QualityState.INVALID, v.state)
        assertFalse("不可信的数据不得显示为数字", v.showNumbers)
    }

    @Test
    fun SQI_恰好等于门限算通过() {
        val v = QualityGate.evaluate(snap(sqi = QualityGate.DEFAULT_SQI_THRESHOLD))
        assertEquals(QualityState.VALID, v.state)
    }

    @Test
    fun SQI_未知时不因此降级_但仍要求有数值() {
        val v = QualityGate.evaluate(snap(sqi = null))
        assertEquals(QualityState.VALID, v.state)
    }

    // ── 双门限：显示下限（beta 标定 0.30）与达标门限（0.50）分开 ──────────

    /** beta 标定口径：floor = 0.30，threshold 仍是临床默认 0.50 */
    private fun betaVerdict(sqi: Float?) = QualityGate.evaluate(
        snap(sqi = sqi),
        sqiThreshold = QualityGate.DEFAULT_SQI_THRESHOLD,
        sqiDisplayFloor = 0.30f,
    )

    @Test
    fun 双门限_中间带给数字并标注仅供参考() {
        val v = betaVerdict(0.4f)
        assertEquals("0.30 ≤ sqi < 0.50 应判 DEGRADED", QualityState.DEGRADED, v.state)
        assertTrue("中间带必须给数字（否则演示/联调永远看不到值）", v.showNumbers)
        assertEquals("信号一般，估算值仅供参考", v.banner)
        assertEquals(MissingReason.NONE, v.reason)
    }

    @Test
    fun 双门限_恰好等于显示下限算进中间带() {
        // floor 是闭区间下界：sqi == 0.30 不再是 INVALID
        val v = betaVerdict(0.30f)
        assertEquals(QualityState.DEGRADED, v.state)
        assertTrue(v.showNumbers)
    }

    @Test
    fun 双门限_低于显示下限仍然_INVALID_且不出数字() {
        val v = betaVerdict(0.2f)
        assertEquals("低于 floor 连估算值都不给", QualityState.INVALID, v.state)
        assertFalse(v.showNumbers)
    }

    @Test
    fun 双门限_达到达标门限才是_VALID() {
        val v = betaVerdict(0.6f)
        assertEquals("≥ 0.50 才算达标", QualityState.VALID, v.state)
        assertTrue(v.showNumbers)
    }

    @Test
    fun 双门限_默认_floor_等于门限时中间带不存在() {
        // 不传 sqiDisplayFloor ⇒ floor == threshold ⇒ 与旧行为一致：0.4 直接 INVALID
        val v = QualityGate.evaluate(snap(sqi = 0.4f))
        assertEquals(QualityState.INVALID, v.state)
        assertFalse(v.showNumbers)
    }

    @Test
    fun 光照不足降为_DEGRADED_但仍显示可靠项() {
        val v = QualityGate.evaluate(snap(), lightOk = false)
        assertEquals(QualityState.DEGRADED, v.state)
        assertTrue("DEGRADED 仍应显示仍可靠的项", v.showNumbers)
        assertEquals(MissingReason.LOW_LIGHT, v.reason)
        assertTrue("应给出光照提示", v.banner != null)
    }

    // ── 建立期认窗口地面真相（windowReady）────────────────────────────────

    /** windowReady 语义：true=窗口已满 / false=建立中 / null=没有 SDK 窗口（旧代理）*/
    private fun windowVerdict(windowReady: Boolean?, elapsedSec: Int) =
        QualityGate.evaluate(snap(elapsedSec = elapsedSec).copy(windowReady = windowReady))

    @Test
    fun 窗口已满时即使会话只有几秒也立刻出数() {
        // 校准页已攒满窗口 → 训练页第 2 秒就该出数，不再空等 30s（问题 2）
        val v = windowVerdict(windowReady = true, elapsedSec = 2)
        assertEquals(QualityState.VALID, v.state)
        assertTrue("窗口已满就必须给数字", v.showNumbers)
    }

    @Test
    fun 窗口未满时即使会话跑过三十秒仍判建立中() {
        val v = windowVerdict(windowReady = false, elapsedSec = 60)
        assertEquals(QualityState.NO_DATA, v.state)
        assertFalse("窗口没攒够，跑再久也不出数字", v.showNumbers)
        assertEquals(MissingReason.NONE, v.reason)
    }

    @Test
    fun windowReady_为空时沿用_elapsedSec_旧代理() {
        // Null/Demo 源没有 SDK 窗口：行为必须与首版一致
        assertFalse(windowVerdict(windowReady = null, elapsedSec = 0).showNumbers)
        assertFalse(windowVerdict(windowReady = null, elapsedSec = 29).showNumbers)
        assertTrue(windowVerdict(windowReady = null, elapsedSec = 30).showNumbers)
    }

    // ── 数值与曲线同门控（hideBelowFloor = false，beta 演示取向）───────────

    /** hideBelowFloor=false：SQI 不再隐藏数字，只决定质量标签 */
    private fun sameGateVerdict(sqi: Float?, hr: Float? = 72f) = QualityGate.evaluate(
        snap(sqi = sqi, hr = hr),
        sqiThreshold = QualityGate.DEFAULT_SQI_THRESHOLD,
        sqiDisplayFloor = 0.30f,
        hideBelowFloor = false,
    )

    @Test
    fun 同门控_达到达标门限仍是_VALID_正常显示() {
        val v = sameGateVerdict(0.6f)
        assertEquals(QualityState.VALID, v.state)
        assertTrue(v.showNumbers)
        assertNull("VALID 不应带质量横幅", v.banner)
    }

    @Test
    fun 同门控_中间带给估值并标信号一般() {
        val v = sameGateVerdict(0.4f)
        assertEquals(QualityState.DEGRADED, v.state)
        assertTrue("中间带必须给数字", v.showNumbers)
        assertEquals("信号一般，估算值仅供参考", v.banner)
    }

    @Test
    fun 同门控_低于显示下限也给估值_只把标签换成信号弱() {
        // 旧行为这里是 INVALID + 隐藏数字（数字闪断的根因）；同门控下改为 DEGRADED 给值
        val v = sameGateVerdict(0.2f)
        assertEquals("低 SQI 不再判 INVALID", QualityState.DEGRADED, v.state)
        assertTrue("SQI 抖动不得让数字消失", v.showNumbers)
        assertEquals("信号弱，估算值仅供参考", v.banner)
        // 兜底：低 SQI 但没有估值时仍是 NO_DATA，不能靠"降级"掩盖"没采到值"
        val noValues = sameGateVerdict(0.2f, hr = null)
        assertEquals(QualityState.NO_DATA, noValues.state)
        assertFalse(noValues.showNumbers)
    }

    @Test
    fun 默认口径仍隐藏低于显示下限的数字() {
        // 不传 hideBelowFloor ⇒ true ⇒ 临床行为不变
        val v = QualityGate.evaluate(
            snap(sqi = 0.2f),
            sqiDisplayFloor = 0.30f,
        )
        assertEquals(QualityState.INVALID, v.state)
        assertFalse(v.showNumbers)
    }

    @Test
    fun 完全无数值时为_NO_DATA_而不是_INVALID() {
        // INVALID 意味着"有数据但不可信"；完全没数据应落 NO_DATA
        val v = QualityGate.evaluate(snap(hr = null, br = null, sqi = 0.9f))
        assertEquals(QualityState.NO_DATA, v.state)
        assertFalse(v.showNumbers)
    }

    @Test
    fun 只有呼吸率时也算有数据() {
        val v = QualityGate.evaluate(snap(hr = null, br = 16f))
        assertEquals(QualityState.VALID, v.state)
        assertTrue(v.showNumbers)
    }

    @Test
    fun 判定顺序_人脸全丢优先于_SQI() {
        // 同时满足"离框 3s"与"SQI 低"时，应报 NO_DATA（更严重的那个）
        val v = QualityGate.evaluate(snap(sqi = 0.1f), faceLostMs = 5_000L)
        assertEquals(QualityState.NO_DATA, v.state)
        assertEquals(MissingReason.FACE_LOST, v.reason)
    }

    @Test
    fun 判定顺序_建立中优先于人脸丢失提示() {
        val v = QualityGate.evaluate(snap(elapsedSec = 10), faceLostMs = 500L)
        assertFalse(v.showNumbers)
    }

    // ── faceLostMs 必须来自快照的真实累计 ────────────────────────────────

    @Test
    fun 快照里的真实丢失时长会被采纳_不足三秒不切状态() {
        val v = QualityGate.evaluate(
            snap().copy(faceInFrame = false, faceLostMs = 2_000L),
        )
        assertEquals(
            "2 秒丢失不应切 NO_DATA（AC-16 的分界就在 3 秒）",
            QualityState.VALID,
            v.state,
        )
        assertTrue(v.dimmed)
    }

    @Test
    fun 快照里的真实丢失时长达到三秒判_NO_DATA() {
        val v = QualityGate.evaluate(
            snap().copy(faceInFrame = false, faceLostMs = 3_050L),
        )
        assertEquals(QualityState.NO_DATA, v.state)
        assertEquals(MissingReason.FACE_LOST, v.reason)
    }

    @Test
    fun 显式传入的丢失时长优先于快照字段() {
        // 便于测试与将来"上层另行计时"的场景
        val v = QualityGate.evaluate(
            snap().copy(faceLostMs = 0L),
            faceLostMs = 5_000L,
        )
        assertEquals(QualityState.NO_DATA, v.state)
    }

    @Test
    fun 质量三点指示与四态对应() {
        assertEquals(3, QualityGate.qualityDots(QualityState.VALID))
        assertEquals(2, QualityGate.qualityDots(QualityState.DEGRADED))
        assertEquals(1, QualityGate.qualityDots(QualityState.INVALID))
        assertEquals(0, QualityGate.qualityDots(QualityState.NO_DATA))
    }

    @Test
    fun 任何不可显示数字的状态都必须显式标记() {
        // 一条兜底断言：把"允许显示数字"的状态穷举出来，
        // 将来新增状态却忘了归类时，这里会提醒补全。
        val showStates = listOf(QualityState.VALID, QualityState.DEGRADED)
        val hideStates = listOf(QualityState.INVALID, QualityState.NO_DATA)
        showStates.forEach { assertTrue(it.name, it == QualityState.VALID || it == QualityState.DEGRADED) }
        hideStates.forEach { assertTrue(it.name, it == QualityState.INVALID || it == QualityState.NO_DATA) }
        assertEquals(
            "四态必须被全覆盖",
            QualityState.entries.toSet(),
            (showStates + hideStates).toSet(),
        )
        assertNull(null)
    }
}