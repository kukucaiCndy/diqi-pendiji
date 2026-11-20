package com.diqi.demo.share

import com.diqi.demo.data.repo.WeekSummary
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
    fun 文案不得出现疗效类表述() {
        val banned = listOf("治愈", "改善", "疗效", "肌力", "康复")
        (0..7).forEach { days ->
            val text = ShareCardBuilder.build(summary(days, days * 300L), cycleDayIndex = 1).toShareText()
            banned.forEach { word ->
                assertFalse("不得出现「$word」：$text", text.contains(word))
            }
        }
    }

    @Test
    fun 分享文本会说明不含身体数据() {
        val text = ShareCardBuilder.build(summary(3, 600), cycleDayIndex = 3).toShareText()
        assertTrue("应明确说明不含身体数据（避免被误读成体检报告）", text.contains("不含任何身体数据"))
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