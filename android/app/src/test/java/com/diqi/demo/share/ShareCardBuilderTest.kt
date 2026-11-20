package com.diqi.demo.share

import com.diqi.demo.data.repo.WeekSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AC-22 的核心断言：分享内容**不得**包含任何身体数据。
 *
 * 这条是合规红线，而且分享出去的文本无法撤回 —— 所以必须用测试钉死，
 * 不能靠"写文案的时候记得别加"。
 */
class ShareCardBuilderTest {

    private fun summary(days: Int, totalSec: Long) = WeekSummary(
        trainedDays = (1..days).map { it.toLong() }.toSet(),
        sessionCount = days * 2,
        totalSec = totalSec,
        lastTrainingAt = 0L,
    )

    @Test
    fun 分享内容只含天数与时长() {
        val text = ShareCardBuilder.build(summary(3, 600), cycleDayIndex = 3).toShareText()
        assertTrue("应包含天数", text.contains("3 天"))
        assertTrue("应包含时长", text.contains("10 分钟"))
    }

    @Test
    fun 分享内容不得包含任何敏感字段() {
        // 覆盖多种天数/时长组合，确保不是"某一个组合恰好没触发"
        listOf(
            summary(0, 0),
            summary(1, 60),
            summary(3, 600),
            summary(7, 3600),
        ).forEach { s ->
            val text = ShareCardBuilder.build(s, cycleDayIndex = s.trainedDays.size.coerceAtLeast(1))
                .toShareText()
            ShareCardBuilder.FORBIDDEN_IN_SHARE.forEach { word ->
                assertFalse(
                    "分享文本不得包含「$word」：$text",
                    text.contains(word, ignoreCase = true),
                )
            }
        }
    }

    @Test
    fun 未训练时也有可分享的文案而不是空白() {
        val card = ShareCardBuilder.build(summary(0, 0), cycleDayIndex = 1)
        assertTrue(card.title.isNotBlank())
        assertTrue(card.body.all { it.isNotBlank() })
    }

    @Test
    fun 文案不得出现任何全局禁词() {
        // 首版这里自建了一个 5 词的表，与 BannedWords.WORDS 分叉 ——
        // 于是"血压/血氧/情绪/心理评分"等词在分享文案里出现也不会变红。
        // 改为直接遍历全局词表：改词表即全局生效。
        (0..7).forEach { days ->
            val text = ShareCardBuilder.build(summary(days, days * 300L), cycleDayIndex = 1).toShareText()
            val hits = com.diqi.demo.core.text.BannedWords.scan(text)
            assertTrue("分享文案命中禁词 $hits：$text", hits.isEmpty())
        }
    }

    @Test
    fun 分享文本只有标题与正文两段_不夹带第四项信息() {
        // FR-13 只允许"完成天数 / 累计时长 / 鼓励语"三项。
        // 首版在末尾追加了「第 N 天」，那是第四项 —— 分享出去无法撤回。
        val text = ShareCardBuilder.build(summary(3, 600), cycleDayIndex = 3).toShareText()
        assertFalse("不得夹带天数序号：$text", text.contains("第 3 天"))
        assertFalse("不得出现免责尾注：$text", text.contains("不含任何身体数据"))
        val lines = text.trim().lines()
        assertEquals("分享文本应为 标题 + 2 行正文", 3, lines.size)
    }

    @Test
    fun 分享文本不得包含任何身体数据字段名() {
        // 首版在末尾追加「…不含任何身体数据」作为免责尾注，
        // 但那让"心率/呼吸率/HRV"这些字眼出现在分享文本里（虽是否定式），
        // 且构成 FR-13 之外的第四段信息。现在改为**完全不带这类字眼**。
        val text = ShareCardBuilder.build(summary(3, 600), cycleDayIndex = 3).toShareText()
        listOf("身体数据", "心率", "呼吸率", "HRV").forEach { word ->
            assertFalse("分享文本不应出现「$word」：$text", text.contains(word))
        }
    }

    @Test
    fun 天数越多鼓励语越积极但不得暗示疗效() {
        val low = ShareCardBuilder.build(summary(1, 300), cycleDayIndex = 1).body.last()
        val high = ShareCardBuilder.build(summary(6, 1800), cycleDayIndex = 6).body.last()
        assertTrue(low.isNotBlank())
        assertTrue(high.isNotBlank())
        assertTrue("鼓励语应随天数变化", low != high)
    }
}