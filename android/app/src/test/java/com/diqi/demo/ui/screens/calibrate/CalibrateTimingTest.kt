package com.diqi.demo.ui.screens.calibrate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 校准页计时/就绪判定（JVM，无需设备）。
 *
 * 回归的是用户实测的 bug：**对着天花板也计时** —— 原实现用
 * `elapsedRealtime() - initAt` 墙钟算 `signalSec`，人脸离开不重置，
 * 于是"入镜后不足 30s 真实采集"就显示就绪，而按钮其实不可用。
 *
 * 现在：
 * - `sec` 只统计**人脸在镜**的时间（0..30）；
 * - 离镜连续 ≥2 帧归零并 `shouldReset`（调用方据此 `bridge.reset()`）；
 * - `ready` 只认 `windowFull`（= `bridge.hasEnoughWindow` 地面真相）。
 */
class CalibrateTimingTest {

    private val max = CalibrateViewModel.HR_READY_SEC

    @Test
    fun `天花板 无人脸 不计时`() {
        var sec = 0
        var absent = 0
        // 连续 10 秒无人脸：秒数必须一直是 0，就绪必须是 false。
        repeat(10) {
            val tick = nextCalibrationTick(
                faceFound = false,
                prevSec = sec,
                absentTicks = absent,
                windowFull = false,
            )
            sec = tick.sec
            absent = tick.absentTicks
            assertEquals("无人脸不应累计秒数", 0, sec)
            assertFalse(tick.ready)
        }
        assertTrue("无人脸帧数应持续累加", absent >= ABSENT_FRAMES_TO_RESET)
    }

    @Test
    fun `人脸在镜逐秒累计`() {
        var sec = 0
        var absent = 7 // 之前离镜过，有脸第一帧应清零
        val seen = mutableListOf<Int>()
        repeat(max) {
            val tick = nextCalibrationTick(
                faceFound = true,
                prevSec = sec,
                absentTicks = absent,
                windowFull = false,
            )
            sec = tick.sec
            absent = tick.absentTicks
            seen += tick.sec
        }
        assertEquals("有脸第一帧必须从 0 开始计（不是跳变到墙钟值）", 1, seen.first())
        assertEquals("逐秒 +1", (1..max).toList(), seen)
        assertEquals("有脸应清零无人脸帧计数", 0, absent)
    }

    @Test
    fun `入镜后从零开始 不继承天花板时间`() {
        // 先无人脸 10 秒（debounce 已触发），再入镜。
        var sec = 0
        var absent = 0
        repeat(10) {
            val t = nextCalibrationTick(false, sec, absent, windowFull = false)
            sec = t.sec
            absent = t.absentTicks
        }
        assertEquals(0, sec)
        val first = nextCalibrationTick(faceFound = true, prevSec = sec, absentTicks = absent, windowFull = false)
        assertEquals("入镜第一秒 = 1s，绝不跳变到 10s+", 1, first.sec)
    }

    @Test
    fun `离镜两帧重置并置 shouldReset`() {
        // 已累计 12 秒。
        var sec = 12
        var absent = 0
        val t1 = nextCalibrationTick(faceFound = false, prevSec = sec, absentTicks = absent, windowFull = false)
        sec = t1.sec
        absent = t1.absentTicks
        assertFalse("单帧漏检不重置", t1.shouldReset)
        assertEquals("单帧漏检保留原累计", 12, t1.sec)

        val t2 = nextCalibrationTick(faceFound = false, prevSec = sec, absentTicks = absent, windowFull = false)
        assertTrue("连续 2 帧无人脸应重置（含 bridge.reset()）", t2.shouldReset)
        assertEquals("重置后累计归零", 0, t2.sec)
        assertEquals(ABSENT_FRAMES_TO_RESET, t2.absentTicks)

        val t3 = nextCalibrationTick(faceFound = false, prevSec = t2.sec, absentTicks = t2.absentTicks, windowFull = false)
        assertFalse("只在跨过门限那一帧 reset，避免每帧重复 reset", t3.shouldReset)
        assertEquals(0, t3.sec)
    }

    @Test
    fun `重置后重入镜从 1 秒重新累计`() {
        val reset = nextCalibrationTick(faceFound = false, prevSec = 20, absentTicks = 1, windowFull = false)
        assertTrue(reset.shouldReset)
        val back = nextCalibrationTick(faceFound = true, prevSec = reset.sec, absentTicks = reset.absentTicks, windowFull = false)
        assertEquals(1, back.sec)
    }

    @Test
    fun `ready 只认 windowFull 不看秒数`() {
        // 秒数攒满但 SDK 窗口未满 → 不就绪（原 bug：墙钟 30s 就说就绪）。
        val fullSecButWindowNotFull = nextCalibrationTick(
            faceFound = true,
            prevSec = max - 1,
            absentTicks = 0,
            windowFull = false,
        )
        assertEquals(max, fullSecButWindowNotFull.sec)
        assertFalse("秒数到了但窗口没满，不能算就绪", fullSecButWindowNotFull.ready)

        // 秒数没攒满但 SDK 窗口已满 → 就绪（地面真相优先）。
        val windowFullEarly = nextCalibrationTick(
            faceFound = true,
            prevSec = 3,
            absentTicks = 0,
            windowFull = true,
        )
        assertEquals(4, windowFullEarly.sec)
        assertTrue("就绪 = SDK 窗口地面真相", windowFullEarly.ready)

        // 重置那一帧即便读到窗口满也必须不就绪（reset 会清窗口）。
        val resetFrame = nextCalibrationTick(
            faceFound = false,
            prevSec = max,
            absentTicks = 1,
            windowFull = true,
        )
        assertTrue(resetFrame.shouldReset)
        assertFalse("reset 帧不就绪", resetFrame.ready)
    }

    @Test
    fun `累计上限 30 秒`() {
        var sec = 0
        var absent = 0
        repeat(max + 15) { i ->
            val tick = nextCalibrationTick(
                faceFound = true,
                prevSec = sec,
                absentTicks = absent,
                windowFull = i >= max,
            )
            sec = tick.sec
            absent = tick.absentTicks
        }
        assertEquals("人脸在镜累计封顶 30s", max, sec)
    }

    @Test
    fun `进度与秒数一致 0到1`() {
        var sec = 0
        repeat(max) {
            val tick = nextCalibrationTick(faceFound = true, prevSec = sec, absentTicks = 0, windowFull = false)
            sec = tick.sec
            val progress = (tick.sec.toFloat() / CalibrateViewModel.HR_READY_SEC).coerceIn(0f, 1f)
            assertEquals(tick.sec / max.toFloat(), progress, 1e-6f)
        }
        assertEquals(1f, max / CalibrateViewModel.HR_READY_SEC.toFloat(), 1e-6f)
    }
}
