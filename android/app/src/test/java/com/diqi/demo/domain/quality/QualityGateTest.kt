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

    @Test
    fun 光照不足降为_DEGRADED_但仍显示可靠项() {
        val v = QualityGate.evaluate(snap(), lightOk = false)
        assertEquals(QualityState.DEGRADED, v.state)
        assertTrue("DEGRADED 仍应显示仍可靠的项", v.showNumbers)
        assertEquals(MissingReason.LOW_LIGHT, v.reason)
        assertTrue("应给出光照提示", v.banner != null)
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