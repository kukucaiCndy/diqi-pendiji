package com.diqi.demo.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diqi.demo.AppContainer
import com.diqi.demo.domain.plan.SevenDayPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 首页感知状态行三态（FR-02）。 */
enum class SensorStatusRow(val label: String, val actionLabel: String) {
    /**
     * 文案说明。
     *
     * 设计稿 P02-a 原文是「训练前需开启摄像头 / 去开启」，但那是"摄像头作为主路径"
     * 的旧 IA 下的说法。首页与训练合并后，**摄像头的开启/不开启由用户在主按钮上
     * 直接选择**（见 INTERACTION-DESIGN.md §2），所以这一行不能再写"必须开启"，
     * 否则与下面「不用摄像头，直接训练」自相矛盾。
     *
     * 改为陈述**当前状态**：未开启时不催促，只说明"不影响练习"。
     */
    OFF("摄像头未开启 · 不影响练习", ""),
    READY("摄像头已就绪", ""),
    WEAK("信号偏弱 · 可点这里调整", "调整"),
}

data class HomeUiState(
    val loading: Boolean = true,
    val greeting: String = "",
    val dayIndex: Int = 1,
    val dayTheme: String = "",
    val dayHint: String = "",
    val trainedToday: Boolean = false,
    /** 本周已训练的星期下标（0 = 周一）*/
    val trainedWeekdays: Set<Int> = emptySet(),
    val weekSessionCount: Int = 0,
    val weekTotalSec: Long = 0,
    val lastTrainingText: String = "还没有训练记录",
    /**
     * P02-b 已完成卡的数据行，对应设计稿「8 次　·　3 分 12 秒　·　平均心率 71」。
     *
     * 心率只在信号覆盖率 ≥60% 时才有值（数据层已把不达标的 avgHr 置空），
     * 所以这里拼接是安全的 —— 不达标时这一段自然消失（FR-09）。
     */
    val todaySummaryText: String = "",
    val sensor: SensorStatusRow = SensorStatusRow.OFF,
    /** FR-01 分流：需要常驻就医提示 */
    val clinicalNoticeRequired: Boolean = false,
    val todayWeekday: Int = 0,
    val cycleComplete: Boolean = false,
)

/**
 * 首页（P02）。
 *
 * ## 数据来源与"绝不造假"原则
 *
 * 所有数字都来自 `ProgressRepository` 的真实查询 —— 它已经在 SQL 层
 * 过滤掉演示数据并加了 60 秒时长门槛。首页**不**做任何本地累计，
 * 否则一旦出现"首页说练了 3 天、记录页说 2 天"就再也解释不清。
 *
 * ## 今日主题
 *
 * 来自七天计划，按**首次完成日**起算（FR-11），不是自然周。
 * 未完成过任何训练时是 Day 1，符合 AC-20 前半。
 */
class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val zone: ZoneId = ZoneId.systemDefault()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val settings = container.settingsStore.current()
            val progress = container.progressRepository

            val week = progress.lastSevenDays(now)
            val dayIndex = progress.currentDayIndex(now)
            val content = SevenDayPlan.contentFor(dayIndex)
            val trainedToday = progress.hasTrainedToday(now)
            val last = container.sessionRepository.latestReal()

            val weekdays = week.trainedDays.map { epochDay ->
                // LocalDate.dayOfWeek: 周一=1 … 周日=7 → 转成 0..6
                LocalDate.ofEpochDay(epochDay).dayOfWeek.value - 1
            }.toSet()

            _state.value = HomeUiState(
                loading = false,
                greeting = greetingFor(now),
                dayIndex = dayIndex,
                dayTheme = content.theme,
                dayHint = content.hint,
                trainedToday = trainedToday,
                trainedWeekdays = weekdays,
                weekSessionCount = week.sessionCount,
                weekTotalSec = week.totalSec,
                lastTrainingText = lastTrainingText(last?.startedAt, last?.durationSec),
                // P02-b 数据行：8 次　·　3 分 12 秒[　·　平均心率 71]
                // 心率段只在覆盖率达标时存在（数据层已把不达标的 avgHr 置空），
                // 所以这里的拼接天然满足 FR-09，不会出现"覆盖率不足还显示心率"。
                todaySummaryText = if (trainedToday && last != null) {
                    buildString {
                        append("${last.completedReps} 次")
                        append("　·　")
                        append(formatMinutes(last.durationSec))
                        last.avgHr?.let { append("　·　平均心率 ${it.toInt()}") }
                    }
                } else {
                    ""
                },
                // 感知状态只能由真实的 SensorCoordinator 驱动（S6）。
                // 首版把"演示模式"映射成"信号弱"是**编造状态**，与本文件
                // 头声明的"绝不造假"自相矛盾，已删除。
                // 在感知层接入前，如实显示"未开启感知"。
                sensor = SensorStatusRow.OFF,
                // 只有真的答过并触发了风险项才显示就医提示。
                // 未完成筛查（或直接跳过）不该被误报成"你之前回答过需要留意的情况"。
                clinicalNoticeRequired = settings.screening.hasRiskAnswers,
                todayWeekday = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
                    .dayOfWeek.value - 1,
                cycleComplete = progress.isCycleComplete(now),
            )
        }
    }

    private fun greetingFor(nowMs: Long): String {
        val hour = Instant.ofEpochMilli(nowMs).atZone(zone).hour
        return when (hour) {
            in 5..10 -> "早上好"
            in 11..13 -> "中午好"
            in 14..17 -> "下午好"
            in 18..22 -> "晚上好"
            else -> "夜深了"
        }
    }

    /**
     * 「上次训练：昨天 · 3 分钟」——格式取自设计稿 P02-a 的末行。
     *
     * 首版只输出「上次训练：今天」，丢了时长；设计稿带时长，且时长对用户有信息量
     * （能判断上次练了多久）。
     */
    private fun lastTrainingText(lastStartedAt: Long?, durationSec: Long?): String {
        if (lastStartedAt == null) return "还没有训练记录"
        val date = Instant.ofEpochMilli(lastStartedAt).atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        val whenText = when (date) {
            today -> "今天"
            today.minusDays(1) -> "昨天"
            else -> date.format(DateTimeFormatter.ofPattern("M 月 d 日"))
        }
        val durText = durationSec?.takeIf { it > 0 }?.let { formatMinutes(it) }
        return if (durText != null) "上次训练：$whenText · $durText" else "上次训练：$whenText"
    }
}