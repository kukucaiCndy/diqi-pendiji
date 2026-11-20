package com.diqi.demo.ui.screens.records

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diqi.demo.AppContainer
import com.diqi.demo.data.db.LeakContext
import com.diqi.demo.data.db.LeakFrequency
import com.diqi.demo.data.db.TrainingSessionEntity
import com.diqi.demo.ui.UiTag
import com.diqi.demo.ui.components.ChipTone
import com.diqi.demo.ui.components.HrBanner
import com.diqi.demo.ui.components.HrCard
import com.diqi.demo.ui.components.HrPage
import com.diqi.demo.ui.components.HrPrimaryButton
import com.diqi.demo.ui.components.HrSecondaryButton
import com.diqi.demo.ui.components.HrStatusChip
import com.diqi.demo.ui.components.VSpace
import com.diqi.demo.ui.rememberContainerViewModel
import com.diqi.demo.ui.screens.home.formatMinutes
import com.diqi.demo.ui.theme.HrColor
import com.diqi.demo.ui.theme.HrRadius
import com.diqi.demo.ui.theme.HrSize
import com.diqi.demo.ui.theme.HrSpacing
import com.diqi.demo.ui.theme.HrType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class RecordsUiState(
    val loading: Boolean = true,
    val dayIndex: Int = 1,
    val trainedDays: List<LocalDate> = emptyList(),
    val sessionCount: Int = 0,
    val totalSec: Long = 0,
    val recent: List<TrainingSessionEntity> = emptyList(),
    val simulated: List<TrainingSessionEntity> = emptyList(),
    val leakToday: LeakFrequency? = null,
    val leakSkipped: Boolean = false,
) {
    val isEmpty: Boolean get() = recent.isEmpty() && simulated.isEmpty()
}

class RecordsViewModel(private val container: AppContainer) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()

    private val _state = MutableStateFlow(RecordsUiState())
    val state: StateFlow<RecordsUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val week = container.progressRepository.lastSevenDays(now)
            val dayIndex = container.progressRepository.currentDayIndex(now)
            val leak = container.leakDiaryRepository.findByDay(now)

            _state.value = RecordsUiState(
                loading = false,
                dayIndex = dayIndex,
                trainedDays = week.trainedDays
                    .map { LocalDate.ofEpochDay(it) }
                    .sorted(),
                sessionCount = week.sessionCount,
                totalSec = week.totalSec,
                recent = container.sessionRepository.recentReal(10),
                simulated = container.sessionRepository.simulatedSessions(5),
                leakToday = leak?.leakFrequency,
                leakSkipped = leak?.skipped ?: false,
            )
        }
    }

    init {
        refresh()
    }

    fun saveLeak(frequency: LeakFrequency?, context: LeakContext?) {
        viewModelScope.launch {
            container.leakDiaryRepository.save(System.currentTimeMillis(), frequency, context)
            refresh()
        }
    }

    fun skipLeak() {
        viewModelScope.launch {
            container.leakDiaryRepository.skip(System.currentTimeMillis())
            refresh()
        }
    }
}

/**
 * P06 记录与七天。
 *
 * ## 三条约束
 *
 * 1. **空态不造假**：没有任何记录时只显示空态文案，不出现模拟数据（FR-10）
 * 2. **演示记录单独成区**：带水印展示，但**不参与任何统计**（D9 / AC-19）
 * 3. **不生成任何诊断结论**：漏尿日记只是记录，不做解读（FR-12）
 */
@Composable
fun RecordsScreen(
    onOpenWeekly: () -> Unit,
    onOpenLeak: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = rememberContainerViewModel { RecordsViewModel(it) }
    val state by vm.state.collectAsState()
    androidx.compose.runtime.LaunchedEffect(Unit) { vm.refresh() }

    HrPage(modifier = modifier, tag = UiTag.P06_ROOT) {
        Text("记录与七天", style = HrType.headline, color = HrColor.Ink)
        VSpace(HrSpacing.xxs)
        Text(
            text = "Day ${state.dayIndex} · 按第一次完成练习那天起算",
            style = HrType.bodySmall,
            color = HrColor.InkSecondary,
        )

        VSpace(HrSpacing.md)

        if (state.isEmpty) {
            HrCard(modifier = Modifier.testTag(UiTag.P06_EMPTY_STATE)) {
                Column(verticalArrangement = Arrangement.spacedBy(HrSpacing.xxs)) {
                    Text("还没有记录", style = HrType.titleSmall, color = HrColor.Ink)
                    Text(
                        "完成第一次练习后，这里会显示你的打卡。",
                        style = HrType.bodySmall,
                        color = HrColor.InkSecondary,
                    )
                }
            }
        } else {
            // 近七天打卡
            HrCard(modifier = Modifier.testTag(UiTag.P06_CALENDAR)) {
                Column(verticalArrangement = Arrangement.spacedBy(HrSpacing.sm)) {
                    Text("近七天", style = HrType.label, color = HrColor.Ink)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val today = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate()
                        (6 downTo 0).forEach { back ->
                            val day = today.minusDays(back.toLong())
                            val done = state.trainedDays.contains(day)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Box(
                                    Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(if (done) HrColor.Good else HrColor.Surface)
                                        .border(1.dp, if (done) HrColor.Good else HrColor.Track, CircleShape),
                                )
                                Text(
                                    "${day.dayOfMonth}",
                                    style = HrType.micro,
                                    color = HrColor.WeakText,
                                )
                            }
                        }
                    }
                }
            }

            VSpace(HrSpacing.sm)

            HrCard {
                Column(verticalArrangement = Arrangement.spacedBy(HrSpacing.xs)) {
                    StatRow("完成天数", "${state.trainedDays.size} 天", UiTag.P06_SUMMARY_DAYS)
                    StatRow("训练次数", "${state.sessionCount} 次", UiTag.P06_SUMMARY_COUNT)
                    StatRow("累计时长", formatMinutes(state.totalSec), UiTag.P06_SUMMARY_DURATION)
                }
            }

            VSpace(HrSpacing.sm)

            // 演示记录：单独成区、带水印、不参与统计（AC-19 / D9）
            if (state.simulated.isNotEmpty()) {
                HrCard(
                    color = HrColor.WarnContainer,
                    modifier = Modifier.testTag(UiTag.P06_DEMO_SECTION),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(HrSpacing.xxs)) {
                        Text(
                            "演示记录 · 不计入打卡",
                            style = HrType.label,
                            color = HrColor.Warn,
                        )
                        state.simulated.take(3).forEach { s ->
                            Text(
                                "· ${s.completedReps} 次 · ${formatMinutes(s.durationSec)}",
                                style = HrType.labelSmall,
                                color = HrColor.Warn,
                            )
                        }
                    }
                }
                VSpace(HrSpacing.sm)
            }

            // 逐条记录
            Column(
                modifier = Modifier.testTag(UiTag.P06_DAY_LIST),
                verticalArrangement = Arrangement.spacedBy(HrSpacing.xs),
            ) {
                state.recent.take(5).forEach { s ->
                    val date = Instant.ofEpochMilli(s.startedAt)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                    HrCard {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "${date.monthValue} 月 ${date.dayOfMonth} 日",
                                style = HrType.label,
                                color = HrColor.Ink,
                            )
                            Text(
                                "${s.completedReps} 次 · ${formatMinutes(s.durationSec)}",
                                style = HrType.labelSmall,
                                color = HrColor.InkSecondary,
                            )
                        }
                    }
                }
            }
        }

        VSpace(HrSpacing.md)

        HrSecondaryButton(
            text = "漏尿日记（可以跳过）",
            tag = UiTag.P06_LEAK_DIARY_ENTRY,
            onClick = onOpenLeak,
        )

        VSpace(HrSpacing.xs)

        HrPrimaryButton(
            text = "家人周报",
            tag = UiTag.P06_WEEKLY_REPORT_ENTRY,
            onClick = onOpenWeekly,
        )

        VSpace(HrSpacing.xxl)
    }
}

@Composable
private fun StatRow(label: String, value: String, tag: String?) {
    Row(
        Modifier
            .fillMaxWidth()
            .let { if (tag != null) it.then(Modifier.testTag(tag)) else it },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = HrType.label, color = HrColor.InkSecondary)
        Text(value, style = HrType.label, color = HrColor.Ink)
    }
}

/**
 * 漏尿简易日记（FR-12）。
 *
 * 全部可跳过；仅本地存储；不生成任何诊断结论；不进周报（AC-22/AC-23）。
 */
@Composable
fun LeakDiaryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = rememberContainerViewModel { RecordsViewModel(it) }
    val state by vm.state.collectAsState()

    var frequency by remember { mutableStateOf<LeakFrequency?>(null) }
    var context by remember { mutableStateOf<LeakContext?>(null) }

    HrPage(modifier = modifier, tag = UiTag.LEAK_ROOT) {
        Text("今天的情况", style = HrType.headline, color = HrColor.Ink)
        VSpace(HrSpacing.xxs)
        Text(
            "这一步可以跳过，只记在你自己的手机里。",
            style = HrType.bodySmall,
            color = HrColor.InkSecondary,
        )

        VSpace(HrSpacing.md)

        Text("今天发生漏尿了吗？", style = HrType.titleSmall, color = HrColor.Ink)
        VSpace(HrSpacing.xs)
        LeakFrequency.entries.forEach { f ->
            RadioRow(
                label = f.label,
                selected = frequency == f,
                tag = when (f) {
                    LeakFrequency.NONE -> UiTag.LEAK_FREQ_NONE
                    LeakFrequency.ONCE -> UiTag.LEAK_FREQ_ONCE
                    LeakFrequency.TWO_TO_THREE -> UiTag.LEAK_FREQ_2_3
                    LeakFrequency.FOUR_PLUS -> UiTag.LEAK_FREQ_4_PLUS
                    LeakFrequency.UNSURE -> UiTag.LEAK_FREQ_UNSURE
                },
                onClick = { frequency = f },
            )
            VSpace(HrSpacing.xxs)
        }

        if (frequency != null && frequency != LeakFrequency.NONE) {
            VSpace(HrSpacing.md)
            Text("当时在做什么？（可不选）", style = HrType.titleSmall, color = HrColor.Ink)
            VSpace(HrSpacing.xs)
            LeakContext.entries.forEach { c ->
                RadioRow(
                    label = c.label,
                    selected = context == c,
                    tag = when (c) {
                        LeakContext.COUGH -> UiTag.LEAK_CONTEXT_COUGH
                        LeakContext.SPORT -> UiTag.LEAK_CONTEXT_SPORT
                        LeakContext.URGE -> UiTag.LEAK_CONTEXT_URGE
                        LeakContext.OTHER -> UiTag.LEAK_CONTEXT_OTHER
                        LeakContext.UNSURE -> UiTag.LEAK_CONTEXT_OTHER
                    },
                    onClick = { context = c },
                )
                VSpace(HrSpacing.xxs)
            }
        }

        VSpace(HrSpacing.md)

        HrPrimaryButton(
            text = "保存",
            tag = UiTag.LEAK_SAVE_BUTTON,
            enabled = frequency != null,
            onClick = {
                vm.saveLeak(frequency, context)
                onBack()
            },
        )

        VSpace(HrSpacing.xs)

        HrSecondaryButton(
            text = "跳过这一步",
            tag = UiTag.LEAK_SKIP_BUTTON,
            onClick = {
                // 跳过写一行 skipped=true，与"从未接触"可区分（AC-23）
                vm.skipLeak()
                onBack()
            },
        )

        VSpace(HrSpacing.xxl)
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = HrSize.primaryButton)
            .clip(HrRadius.controlShape)
            .background(if (selected) HrColor.GoodContainer else HrColor.Surface)
            .border(1.dp, if (selected) HrColor.Good else HrColor.Line, HrRadius.controlShape)
            .clickable(role = Role.RadioButton) { onClick() }
            .testTag(tag)
            .padding(horizontal = HrSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HrSpacing.xs),
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (selected) HrColor.Good else HrColor.Surface)
                .border(2.dp, if (selected) HrColor.Good else HrColor.Track, CircleShape),
        )
        Text(
            label,
            style = if (selected) HrType.bodyMediumBold else HrType.bodyMedium,
            color = if (selected) HrColor.GoodDeep else HrColor.Slate800,
        )
    }
}