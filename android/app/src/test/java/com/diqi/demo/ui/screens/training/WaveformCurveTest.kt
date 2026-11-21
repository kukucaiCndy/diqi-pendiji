package com.diqi.demo.ui.screens.training

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 训练波形趋势线的映射（JVM，无需设备）。
 *
 * 覆盖 brief P3 要求的五类输入：空 / 单点 / 常量 / 正常 / 超 60。
 * 只有这一段是纯计算 —— 曲线本身由 Canvas 画，映射对了曲线才对。
 */
class WaveformCurveTest {

    private val width = 590f
    private val height = 64f
    private val step = width / (CURVE_SLOTS - 1) // = 10f

    @Test
    fun 空历史不产生任何点() {
        assertTrue(normalizeCurvePoints(emptyList(), width, height).isEmpty())
    }

    @Test
    fun 单点落在右缘中线上() {
        val points = normalizeCurvePoints(listOf(72f), width, height)

        assertEquals(1, points.size)
        assertEquals("最新点贴右缘", width, points[0].x, 0.001f)
        assertEquals("单点无趋势，画中线", height / 2f, points[0].y, 0.001f)
    }

    @Test
    fun 常量历史全部落在中线且横向不变宽() {
        val points = normalizeCurvePoints(List(5) { 70f }, width, height)

        assertEquals(5, points.size)
        points.forEach {
            assertEquals("等值不该放大成波动", height / 2f, it.y, 0.001f)
        }
        assertEquals("最新点贴右缘", width, points.last().x, 0.001f)
        assertEquals(
            "5 点之间共 4 个间距，每格 = width/59",
            step * 4f,
            points.last().x - points[0].x,
            0.001f,
        )
    }

    @Test
    fun 正常读数按最小最大归一化到半高() {
        // 最小 60 → 半高下沿；最大 80 → 半高上沿（y 轴向下）
        val points = normalizeCurvePoints(listOf(60f, 80f), width, height)

        assertEquals(2, points.size)
        assertEquals("最小值画在下方 3/4 高", height * 0.75f, points[0].y, 0.001f)
        assertEquals("最大值画在上方 1/4 高", height * 0.25f, points[1].y, 0.001f)
        assertEquals("倒数第二点距离右缘一个间距", width - step, points[0].x, 0.001f)
        assertEquals("最新点贴右缘", width, points[1].x, 0.001f)
    }

    @Test
    fun 超过六十点仍每点一个且最新贴右缘() {
        val history = List(80) { 60f + (it % 20) }
        val points = normalizeCurvePoints(history, width, height)

        assertEquals("80 个采样都保留映射（上游负责环形截断）", 80, points.size)
        assertEquals("最新点贴右缘", width, points.last().x, 0.001f)
        assertTrue("更旧的采样落到画布左侧之外（自然裁掉）", points.first().x < 0f)
    }

    @Test
    fun 宽度为零时不崩溃() {
        assertTrue(normalizeCurvePoints(listOf(60f, 70f), 0f, height).isEmpty())
    }

    @Test
    fun 历史环形上限六十点丢最旧() {
        var history = emptyList<Float>()
        repeat(65) { history = TrainingViewModel.appendBounded(history, it.toFloat()) }

        assertEquals("最多保留 60 点", 60, history.size)
        assertEquals("最新的留下", 64f, history.last(), 0.001f)
        assertEquals("最旧的被丢弃", 5f, history.first(), 0.001f)
    }
}
