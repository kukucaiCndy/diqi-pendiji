package com.diqi.demo.ui.screens.complete

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diqi.demo.AppContainer
import com.diqi.demo.data.repo.SessionRepository
import com.diqi.demo.ui.UiTag
import com.diqi.demo.ui.components.ChipTone
import com.diqi.demo.ui.components.HrBanner
import com.diqi.demo.ui.components.HrCard
import com.diqi.demo.ui.components.HrPageWithAction
import com.diqi.demo.ui.components.HrPrimaryButton
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

/** 自评难度（1 轻松 / 2 合适 / 3 困难）*/
enum class Difficulty(val value: Int, val label: String) {
    EASY(1, "轻松"),
    OK(2, "合适"),
    HARD(3, "困难"),
}

data class CompleteUiState(
    val loading: Boolean = true,
    val durationSec: Long = 0,
    val completedReps: Int = 0,
    val plannedReps: Int = 0,
    val pauseCount: Int = 0,
    /** 仅当覆盖率 ≥ 60% 才有值（FR-09）*/
    val signalCoverage: Double? = null,
    val avgHr: Double? = null,
    val difficulty: Difficulty? = null,
    val discomfort: Boolean? = null,
    val saved: Boolean = false,
    /** FR-01 分流：不给「已学会」结论 */
    val mustAvoidLearnedConclusion: Boolean = true,
    val sessionId: String? = null,
)

/**
 * P05 完成与自评。
 *
 * ## 三条硬约束
 *
 * 1. **覆盖率不足时不显示指标**：`signalCoverage < 0.6` 时整块不出现，
 *    连「--」都不显示（FR-09 原文：不显示 0、不显示「--」以外的占位）。
 * 2. **选「有不适」→ 停止并寻求专业评估，无加量建议**（FR-09 / AC-11）。
 * 3. **不展示动作正确率、肌力变化、疗效数字**（§5.6 红线）。
 */
class CompleteViewModel(private val container: AppContainer, sessionId: String?) :
    ViewModel() {

    private val _state = MutableStateFlow(CompleteUiState(sessionId = sessionId))
    val state: StateFlow<CompleteUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = container.settingsStore.current()
            val entity = sessionId?.let { container.sessionRepository.findById(it) }
            _state.value = _state.value.copy(
                loading = false,
                durationSec = entity?.durationSec ?: 0,
                completedReps = entity?.completedReps ?: 0,
                plannedReps = entity?.plannedReps ?: 0,
                pauseCount = entity?.pauseCount ?: 0,
                // 数据层已在保存时把不达标的指标置空，这里再判一次门限，
                // 双重保证"覆盖率不足绝不显示数字"
                signalCoverage = entity?.signalCoverage
                    ?.takeIf { it >= SessionRepository.MIN_COVERAGE_FOR_METRICS },
                avgHr = entity?.avgHr,
                mustAvoidLearnedConclusion = settings.screening.mustAvoidLearnedConclusion,
            )
        }
    }

    fun selectDifficulty(value: Difficulty) {
        _state.value = _state.value.copy(difficulty = value)
        persist()
    }

    fun selectDiscomfort(value: Boolean) {
        _state.value = _state.value.copy(discomfort = value)
        persist()
    }

    private fun persist() {
        val s = _state.value
        val id = s.sessionId ?: return
        viewModelScope.launch {
            runCatching {
                container.sessionRepository.attachSelfAssessment(
                    sessionId = id,
                    difficulty = s.difficulty?.value,
                    discomfort = s.discomfort,
                )
            }
        }
    }
}

@Composable
fun CompleteScreen(
    sessionId: String?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = rememberContainerViewModel { CompleteViewModel(it, sessionId) }
    val state by vm.state.collectAsState()

    HrPageWithAction(
        modifier = modifier,
        tag = UiTag.P05_ROOT,
        action = {
            HrPrimaryButton(
                text = "完成",
                tag = UiTag.P05_DONE_BUTTON,
                onClick = onDone,
            )
        },
    ) {
        Text("完成今天的练习", style = HrType.headline, color = HrColor.Ink)
        VSpace(HrSpacing.xxs)
        Text(
            text = "做得很好，明天再练一次就够了。",
            style = HrType.bodySmall,
            color = HrColor.InkSecondary,
        )

        VSpace(HrSpacing.md)

        // 只展示时长/次数/暂停次数 —— 不展示任何"效果"指标
        HrCard {
            Column(verticalArrangement = Arrangement.spacedBy(HrSpacing.xs)) {
                StatRow("引导时长", formatMinutes(state.durationSec), UiTag.P05_DURATION)
                StatRow("完成次数", "${state.completedReps} / ${state.plannedReps} 次", UiTag.P05_REPS)
                StatRow("中途暂停", "${state.pauseCount} 次", UiTag.P05_PAUSE_COUNT)
            }
        }

        // 覆盖率门限：不达标整块不出现（FR-09）
        val coverage = state.signalCoverage
        if (coverage != null) {
            VSpace(HrSpacing.sm)
            HrCard(color = HrColor.SubtleContainer) {
                Column(verticalArrangement = Arrangement.spacedBy(HrSpacing.xs)) {
                    Text("本次感知", style = HrType.label, color = HrColor.Ink)
                    StatRow(
                        "信号覆盖",
                        "${(coverage * 100).toInt()}%",
                        UiTag.P05_COVERAGE,
                    )
                    state.avgHr?.let {
                        StatRow("平均心率", "%.0f bpm".format(it), UiTag.P05_AVG_HR)
                    }
                    Text(
                        "反映身体状态，不代表盆底肌收缩质量。",
                        style = HrType.disclaimer,
                        color = HrColor.InkSecondary,
                    )
                }
            }
        }

        VSpace(HrSpacing.md)

        Text("这次练得怎么样？", style = HrType.titleSmall, color = HrColor.Ink)
        VSpace(HrSpacing.xs)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HrSpacing.xs),
        ) {
            Difficulty.entries.forEach { d ->
                ChoicePill(
                    label = d.label,
                    selected = state.difficulty == d,
                    tag = when (d) {
                        Difficulty.EASY -> UiTag.P05_DIFFICULTY_EASY
                        Difficulty.OK -> UiTag.P05_DIFFICULTY_OK
                        Difficulty.HARD -> UiTag.P05_DIFFICULTY_HARD
                    },
                    onClick = { vm.selectDifficulty(d) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        VSpace(HrSpacing.md)

        Text("练习时有没有哪里不舒服？", style = HrType.titleSmall, color = HrColor.Ink)
        VSpace(HrSpacing.xs)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HrSpacing.xs),
        ) {
            ChoicePill(
                label = "没有",
                selected = state.discomfort == false,
                tag = UiTag.P05_DISCOMFORT_TOGGLE,
                onClick = { vm.selectDiscomfort(false) },
                modifier = Modifier.weight(1f),
            )
            ChoicePill(
                label = "有不适",
                selected = state.discomfort == true,
                tag = UiTag.P05_DISCOMFORT_NOTICE,
                onClick = { vm.selectDiscomfort(true) },
                modifier = Modifier.weight(1f),
            )
        }

        // AC-11：选「有不适」→ 停止并寻求专业评估，**无加量建议**
        if (state.discomfort == true) {
            VSpace(HrSpacing.sm)
            HrBanner(
                text = "先暂停练习，并尽快咨询医生或专业人员。等情况明确后再决定是否继续。",
                tone = ChipTone.WARN,
                tag = UiTag.P05_DISCOMFORT_NOTICE,
            )
        }

        VSpace(HrSpacing.lg)
    }
}

@Composable
private fun StatRow(label: String, value: String, tag: String?) {
    Row(
        Modifier
            .fillMaxWidth()
            .let { if (tag != null) it.then(Modifier.testTag(tag)) else it },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = HrType.label, color = HrColor.InkSecondary)
        Text(value, style = HrType.monoMedium, color = HrColor.Ink)
    }
}

@Composable
private fun ChoicePill(
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .heightIn(min = HrSize.primaryButton)
            .clip(HrRadius.controlShape)
            .background(if (selected) HrColor.GoodContainer else HrColor.Surface)
            .border(
                1.dp,
                if (selected) HrColor.Good else HrColor.Line,
                HrRadius.controlShape,
            )
            .clickable(role = Role.RadioButton) { onClick() }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = if (selected) HrType.bodyMediumBold else HrType.bodyMedium,
            color = if (selected) HrColor.GoodDeep else HrColor.Slate800,
        )
    }
}