package com.diqi.demo.domain.plan

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 七天体验周期的一天（需求 FR-11）。
 *
 * 红线（FR-11 末条 / §5.6）：**七天是体验周期，界面不得暗示疗效**。
 * 因此这里的文案只说"做什么"，不说"改善了什么"。
 */
data class DayContent(
    val dayIndex: Int,
    /** 首页主题行，形如「Day 3 · 练习不憋气」 */
    val theme: String,
    /** 首页一句提示 */
    val hint: String,
    /** 训练页额外强调的提示（本期只影响文案侧重，不改变状态机） */
    val focus: Focus,
) {
    enum class Focus {
        /** 无额外侧重 */
        NONE,
        /** 加强呼吸提示（Day 3 不憋气） */
        BREATHING_EMPHASIS,
        /** 动作教学置顶（Day 1） */
        TEACHING_FIRST,
        /** 一键进入（Day 5 独立训练） */
        QUICK_START,
    }
}

/**
 * 七天计划（需求 FR-11）。
 *
 * ## 进度起算规则（必须按首次完成训练算，不按自然周）
 *
 * `dayIndex = 首次完成日 到 今天 的天数差 + 1`，夹在 1..7。
 * 之所以不用自然周：用户可能周三才开始，按自然周算会让她第一天就"错过"四天，
 * 这对 55+ 用户是明确的劝退设计。
 *
 * 跨天边界用**设备本地时区**的日历天，而不是 24 小时整除 ——
 * 否则晚上 23:50 练完、00:10 再进来会还是 Day 1。
 */
object SevenDayPlan {

    val DAYS: List<DayContent> = listOf(
        DayContent(
            dayIndex = 1,
            theme = "Day 1 · 认识盆底肌",
            hint = "先感受一下：轻轻提起，再慢慢放松，不用用力。",
            focus = DayContent.Focus.TEACHING_FIRST,
        ),
        DayContent(
            dayIndex = 2,
            theme = "Day 2 · 完成一次引导训练",
            hint = "跟着语音走完一组，找到自己的节奏就够了。",
            focus = DayContent.Focus.NONE,
        ),
        DayContent(
            dayIndex = 3,
            theme = "Day 3 · 练习不憋气",
            hint = "今天最重要的一件事：全程自然呼吸。",
            focus = DayContent.Focus.BREATHING_EMPHASIS,
        ),
        DayContent(
            dayIndex = 4,
            theme = "Day 4 · 熟悉收缩与放松",
            hint = "一收一放，像呼吸一样有来有回。",
            focus = DayContent.Focus.NONE,
        ),
        DayContent(
            dayIndex = 5,
            theme = "Day 5 · 独立进入训练",
            hint = "不用再看说明，点一下就能开始。",
            focus = DayContent.Focus.QUICK_START,
        ),
        DayContent(
            dayIndex = 6,
            theme = "Day 6 · 看看自己的坚持",
            hint = "翻一翻这几天的记录，你已经做到了。",
            focus = DayContent.Focus.NONE,
        ),
        DayContent(
            dayIndex = 7,
            theme = "Day 7 · 回顾与决定",
            hint = "这七天走完了。要不要继续，由你自己决定。",
            focus = DayContent.Focus.NONE,
        ),
    )

    const val TOTAL_DAYS = 7

    fun contentFor(dayIndex: Int): DayContent =
        DAYS[(dayIndex - 1).coerceIn(0, DAYS.lastIndex)]

    /**
     * 计算今天属于第几天。
     *
     * @param firstCompletionAtMs 首次完成训练的 epoch millis；null 表示还没完成过
     * @param nowMs 当前时间 epoch millis
     * @return 1..7；未开始过返回 1
     */
    fun dayIndexFor(
        firstCompletionAtMs: Long?,
        nowMs: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Int {
        if (firstCompletionAtMs == null) return 1
        val first: LocalDate = Instant.ofEpochMilli(firstCompletionAtMs).atZone(zone).toLocalDate()
        val today: LocalDate = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        val elapsedDays = java.time.temporal.ChronoUnit.DAYS.between(first, today)
        return (elapsedDays + 1).toInt().coerceIn(1, TOTAL_DAYS)
    }

    /** 七天是否已走完（首页据此切换文案） */
    fun isCycleComplete(firstCompletionAtMs: Long?, nowMs: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        if (firstCompletionAtMs == null) return false
        val first = Instant.ofEpochMilli(firstCompletionAtMs).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        return java.time.temporal.ChronoUnit.DAYS.between(first, today) >= TOTAL_DAYS
    }

    /** epoch millis → 本地 epoch day，供漏尿日记按天归档 */
    fun epochDayOf(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate().toEpochDay()
}