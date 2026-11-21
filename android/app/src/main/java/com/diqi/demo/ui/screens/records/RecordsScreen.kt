package com.diqi.demo.ui.screens.records

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diqi.demo.AppContainer
import com.diqi.demo.data.db.TrainingSessionEntity
import com.diqi.demo.ui.UiTag
import com.diqi.demo.ui.rememberContainerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class RecordsUiState(
    val loading: Boolean = true,
    val monthLabel: String = "",
    /** 当月第一天是周几（0 = 周一），用于月历左侧留白 */
    val leadingBlanks: Int = 0,
    val daysInMonth: Int = 30,
    /** 当月已完成训练的日号（1-based） */
    val trainedDayNumbers: Set<Int> = emptySet(),
    val weekTrainedDays: Int = 0,
    val totalSec: Long = 0,
    val sessionCount: Int = 0,
    val recent: List<TrainingSessionEntity> = emptyList(),
    val simulated: List<TrainingSessionEntity> = emptyList(),
) {
    val isEmpty: Boolean get() = recent.isEmpty() && simulated.isEmpty()
}

/**
 * P06 记录与 7 天。
 *
 * ## 数据口径
 *
 * 月历的圆点只统计**真实且 ≥60 秒**的训练（数据层 SQL 已过滤 `simulated` 与短会话），
 * 因此「汇总值 = 列表逐条相加」这条 AC-12 判据天然成立。
 *
 * ## 演示记录
 *
 * 仍然单独成区并标注「不计入打卡」（D9），**不参与月历与任何汇总**。
 */
class RecordsViewModel(private val container: AppContainer) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()

    private val _state = MutableStateFlow(RecordsUiState())
    val state: StateFlow<RecordsUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            val ym = YearMonth.from(today)

            // 取近 60 天真实记录，既够本月月历，也够下面逐条列表
            val recent = container.sessionRepository.recentReal(60)
            val trainedDates = recent
                .map { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
                .filter { YearMonth.from(it) == ym }
                .map { it.dayOfMonth }
                .toSet()

            val week = container.progressRepository.lastSevenDays(now)

            _state.value = RecordsUiState(
                loading = false,
                monthLabel = "${ym.year} 年 ${ym.monthValue} 月",
                leadingBlanks = ym.atDay(1).dayOfWeek.value - 1,
                daysInMonth = ym.lengthOfMonth(),
                trainedDayNumbers = trainedDates,
                weekTrainedDays = week.trainedDays.size,
                totalSec = week.totalSec,
                sessionCount = week.sessionCount,
                recent = recent.take(10),
                simulated = container.sessionRepository.simulatedSessions(5),
            )
        }
    }

    init {
        refresh()
    }
}

@Composable
fun RecordsScreen(
    onOpenWeekly: () -> Unit,
    onOpenLeak: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = rememberContainerViewModel { RecordsViewModel(it) }
    val state by vm.state.collectAsState()
    androidx.compose.runtime.LaunchedEffect(Unit) { vm.refresh() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HrCanvas)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 10.dp)
            .testTag(UiTag.P06_ROOT),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("训练记录", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = HrInk)

        // 三个汇总卡 h82 r14
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard(
                label = "7 天完成",
                value = "${state.weekTrainedDays} 天",
                tag = UiTag.P06_SUMMARY_DAYS,
                modifier = Modifier.weight(1f),
            )
            SummaryCard(
                label = "累计时长",
                value = mmss(state.totalSec),
                tag = UiTag.P06_SUMMARY_DURATION,
                modifier = Modifier.weight(1f),
            )
            SummaryCard(
                label = "训练次数",
                value = "${state.sessionCount} 次",
                tag = UiTag.P06_SUMMARY_COUNT,
                modifier = Modifier.weight(1f),
            )
        }

        // 月历卡
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(HrWhite)
                .border(BorderStroke(1.dp, HrLine), RoundedCornerShape(16.dp))
                .padding(16.dp)
                .testTag(UiTag.P06_CALENDAR),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(state.monthLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HrInk)
                Text("›", fontSize = 16.sp, color = HrInkTertiary)
            }
            // 星期表头
            Row(modifier = Modifier.fillMaxWidth()) {
                WEEK_HEADERS.forEach { w ->
                    Text(
                        text = w,
                        fontSize = 10.sp,
                        color = HrInkTertiary,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            MonthGrid(
                leadingBlanks = state.leadingBlanks,
                daysInMonth = state.daysInMonth,
                trained = state.trainedDayNumbers,
            )
        }

        if (state.isEmpty) {
            Text(
                "还没有记录，完成第一次练习后会出现在这里。",
                fontSize = 13.sp,
                color = HrInkMuted,
                modifier = Modifier.testTag(UiTag.P06_EMPTY_STATE),
            )
        }

        // 演示记录：单独成区、水印样式、不计入打卡
        if (state.simulated.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(HrWarnContainer)
                    .padding(14.dp)
                    .testTag(UiTag.P06_DEMO_SECTION),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("演示记录 · 不计入打卡", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = HrWarn)
                state.simulated.take(3).forEach { s ->
                    Text(
                        "· ${s.completedReps} 次 · ${mmss(s.durationSec)}",
                        fontSize = 11.sp,
                        color = HrWarn,
                    )
                }
            }
        }

        // 逐条记录
        Column(
            modifier = Modifier.testTag(UiTag.P06_DAY_LIST),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.recent.take(6).forEach { s ->
                val d = Instant.ofEpochMilli(s.startedAt).atZone(zone0()).toLocalDate()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(HrWhite)
                        .border(BorderStroke(1.dp, HrLine), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "${d.monthValue} 月 ${d.dayOfMonth} 日",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HrInk,
                    )
                    Text(
                        "${s.completedReps} 次 · ${mmss(s.durationSec)}",
                        fontSize = 12.sp,
                        color = HrInkMuted,
                    )
                }
            }
        }

        // 入口
        EntryRow("家人周报", UiTag.P06_WEEKLY_REPORT_ENTRY, onOpenWeekly)
        EntryRow("漏尿日记（可跳过）", UiTag.P06_LEAK_DIARY_ENTRY, onOpenLeak)

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun MonthGrid(leadingBlanks: Int, daysInMonth: Int, trained: Set<Int>) {
    val cells = leadingBlanks + daysInMonth
    val rows = (cells + 6) / 7
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(rows) { r ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { c ->
                    val idx = r * 7 + c
                    val dayNo = idx - leadingBlanks + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        if (dayNo in 1..daysInMonth) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text(
                                    dayNo.toString(),
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569),
                                )
                                if (trained.contains(dayNo)) {
                                    Box(
                                        Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(HrGood),
                                    )
                                } else {
                                    Spacer(Modifier.height(5.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, tag: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(HrWhite)
            .border(BorderStroke(1.dp, HrLine), RoundedCornerShape(14.dp))
            .padding(start = 12.dp, top = 12.dp)
            .testTag(tag),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = HrInkTertiary)
        Text(
            value,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = HrInk,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        )
    }
}

@Composable
private fun EntryRow(label: String, tag: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(HrWhite)
            .border(BorderStroke(1.dp, HrTrack), RoundedCornerShape(26.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(tag)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = HrInk)
        Text("›", fontSize = 16.sp, color = HrInkTertiary)
    }
}

private fun zone0(): ZoneId = ZoneId.systemDefault()

/** 「756 秒」→「12:36」，与设计稿汇总卡格式一致。 */
private fun mmss(totalSec: Long): String = "%d:%02d".format(totalSec / 60, totalSec % 60)

private val WEEK_HEADERS = listOf("一", "二", "三", "四", "五", "六", "日")

private val HrWhite = Color(0xFFFFFFFF)
private val HrCanvas = Color(0xFFF4F6F8)
private val HrInk = Color(0xFF0F1A24)
private val HrInkMuted = Color(0xFF64748B)
private val HrInkTertiary = Color(0xFF94A3B8)
private val HrLine = Color(0xFFE2E8F0)
private val HrTrack = Color(0xFFCBD5E1)
private val HrGood = Color(0xFF047857)
private val HrWarn = Color(0xFFB45309)
private val HrWarnContainer = Color(0xFFFEF3C7)