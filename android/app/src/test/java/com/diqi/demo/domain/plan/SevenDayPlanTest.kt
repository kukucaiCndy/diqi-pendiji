package com.diqi.demo.domain.plan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * FR-11 / AC-20 的逻辑层验证：Day 进度必须按**首次完成日**起算，不按自然周。
 */
class SevenDayPlanTest {

    private val zone = ZoneId.of("Asia/Shanghai")

    private fun ms(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    @Test
    fun `从未训练过时显示 Day 1`() {
        assertEquals(1, SevenDayPlan.dayIndexFor(null, ms(LocalDate.of(2026, 11, 20)), zone))
    }

    @Test
    fun `首次完成当天是 Day 1`() {
        val first = LocalDate.of(2026, 11, 20)
        assertEquals(1, SevenDayPlan.dayIndexFor(ms(first), ms(first), zone))
    }

    @Test
    fun `完成后第一天是 Day 2 —— 这是 AC-20 后半的判据`() {
        val first = LocalDate.of(2026, 11, 20)
        assertEquals(
            2,
            SevenDayPlan.dayIndexFor(ms(first), ms(first.plusDays(1)), zone),
        )
    }

    @Test
    fun `第 7 天仍是 7 而不会溢出到 8`() {
        val first = LocalDate.of(2026, 11, 20)
        assertEquals(7, SevenDayPlan.dayIndexFor(ms(first), ms(first.plusDays(6)), zone))
        // 超出周期后夹在 7
        assertEquals(7, SevenDayPlan.dayIndexFor(ms(first), ms(first.plusDays(30)), zone))
    }

    @Test
    fun `按自然日算而不是按 24 小时整除`() {
        // 23:50 开始算第一天，第二天 00:10 进来应已是 Day 2
        val day1Late = LocalDate.of(2026, 11, 20).atTime(23, 50)
            .atZone(zone).toInstant().toEpochMilli()
        val day2Early = LocalDate.of(2026, 11, 21).atTime(0, 10)
            .atZone(zone).toInstant().toEpochMilli()
        assertEquals(
            "只过了 20 分钟，但跨了自然日，必须是 Day 2",
            2,
            SevenDayPlan.dayIndexFor(day1Late, day2Early, zone),
        )
    }

    @Test
    fun `七天周期走完判定`() {
        val first = LocalDate.of(2026, 11, 20)
        assertFalse(SevenDayPlan.isCycleComplete(ms(first), ms(first.plusDays(6)), zone))
        assertTrue(SevenDayPlan.isCycleComplete(ms(first), ms(first.plusDays(7)), zone))
        assertFalse(SevenDayPlan.isCycleComplete(null, ms(first), zone))
    }

    @Test
    fun `七天内容齐全且主题含 Day 编号`() {
        assertEquals(7, SevenDayPlan.DAYS.size)
        SevenDayPlan.DAYS.forEachIndexed { i, day ->
            assertEquals(i + 1, day.dayIndex)
            assertTrue("Day ${day.dayIndex} 主题应含编号", day.theme.contains("Day ${i + 1}"))
            assertTrue("Day ${day.dayIndex} 提示不能为空", day.hint.isNotBlank())
        }
    }

    @Test
    fun `Day3 必须强化呼吸提示 —— 需求要求练习不憋气`() {
        assertEquals(DayContent.Focus.BREATHING_EMPHASIS, SevenDayPlan.contentFor(3).focus)
    }

    @Test
    fun `Day1 必须有动作教学聚焦`() {
        assertEquals(DayContent.Focus.TEACHING_FIRST, SevenDayPlan.contentFor(1).focus)
    }

    @Test
    fun `七天文案不得暗示疗效 —— 用全局词表而不是测试内自建表`() {
        // 首版在这里自建了一个 listOf("治愈","改善","疗效","康复","肌力")，
        // 与 BannedWords.WORDS 分叉（「康复」全局扫描原本不设防）。
        // 现在统一读全局词表，改词表即全局生效（评审 E5）。
        SevenDayPlan.DAYS.forEach { day ->
            val hits = com.diqi.demo.core.text.BannedWords.scan(day.theme + " " + day.hint)
            assertTrue(
                "Day ${day.dayIndex} 文案命中禁词 $hits：${day.theme} / ${day.hint}",
                hits.isEmpty(),
            )
        }
    }

    @Test
    fun `dayIndex 越界时夹取而不抛异常`() {
        assertEquals(1, SevenDayPlan.contentFor(0).dayIndex)
        assertEquals(7, SevenDayPlan.contentFor(99).dayIndex)
    }

    @Test
    fun `epochDay 换算按本地时区`() {
        val t = LocalDate.of(2026, 11, 20).atTime(0, 30)
            .atZone(zone).toInstant().toEpochMilli()
        assertEquals(LocalDate.of(2026, 11, 20).toEpochDay(), SevenDayPlan.epochDayOf(t, zone))
    }
}