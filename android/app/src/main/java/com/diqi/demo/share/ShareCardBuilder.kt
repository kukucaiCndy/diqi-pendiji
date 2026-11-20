package com.diqi.demo.share

import com.diqi.demo.data.repo.WeekSummary

/**
 * 家人周报文案构建器（FR-13）。
 *
 * ## 这个类为什么值得单独存在并且有单测
 *
 * FR-13 / AC-22 的红线是**分享内容里绝不能出现心率、呼吸率、HRV、漏尿记录、原始视频**。
 * 如果文案在 Composable 里用字符串拼出来，这条约束就只能靠人肉 review；
 * 抽成纯函数后，`ShareCardBuilderTest` 可以逐条断言"敏感字段一个都不在"。
 *
 * 内容只允许三项（需求原文）：
 * 1. 本周完成天数
 * 2. 累计训练时长
 * 3. 鼓励文案
 */
object ShareCardBuilder {

    /** 需求允许出现在分享内容里的最大信息量 —— 就是这三项，没有第四项。 */
    fun build(summary: WeekSummary, cycleDayIndex: Int): ShareCard {
        val days = summary.trainedDays.size
        val minutes = (summary.totalSec / 60).toInt()
        val header = if (days > 0) {
            "这周我完成了 $days 天练习"
        } else {
            "这周还没开始练习"
        }
        val detail = if (days > 0) {
            "累计 $minutes 分钟"
        } else {
            "下周一起加油"
        }
        val encouragement = when {
            days >= 6 -> "几乎每天都在坚持，很棒。"
            days >= 3 -> "坚持得不错，继续保持。"
            days > 0 -> "开始了就是好的开始。"
            else -> "还没开始也没关系，明天试试看。"
        }
        return ShareCard(
            title = header,
            body = listOf(detail, encouragement),
            dayIndex = cycleDayIndex,
        )
    }

    /**
     * 敏感词黑名单：用于自检与测试。
     *
     * 这些词一旦出现在分享文本里就是合规事故（FR-13 / AC-22）。
     */
    val FORBIDDEN_IN_SHARE: List<String> = listOf(
        "心率", "bpm", "呼吸", "HRV", "漏尿", "覆盖", "SQI", "信号", "视频", "画面", // banned-ok: 黑名单定义本身
    )
}

data class ShareCard(
    val title: String,
    val body: List<String>,
    /** 七天周期里的第几天。**只用于页面展示，不进分享文本**（FR-13 只允许三项）。 */
    val dayIndex: Int,
) {
    /**
     * 最终分享出去的纯文本。
     *
     * **只允许三项**：完成天数、累计时长、鼓励语（FR-13 原文）。
     * 首版在这里追加了「第 $dayIndex 天」，那成了第四项信息 ——
     * 分享出去的东西无法撤回，所以宁可少说一句，也不要多带一个字段。
     */
    fun toShareText(): String = buildString {
        appendLine(title)
        body.forEach { appendLine(it) }
    }
}