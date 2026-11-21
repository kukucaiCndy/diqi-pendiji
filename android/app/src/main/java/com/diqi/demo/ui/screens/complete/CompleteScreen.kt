package com.diqi.demo.ui.screens.complete

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
import com.diqi.demo.ui.UiTag
import com.diqi.demo.ui.rememberContainerViewModel
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
    /**
     * 信号覆盖率。**< 60% 时整块覆盖率卡不出现**（设计稿 P05-a 说明：
     * 「覆盖率<60% 时平均心率整行不占位」，FR-09）。
     */
    val signalCoverage: Double? = null,
    val avgHr: Double? = null,
    val difficulty: Difficulty? = null,
    val discomfort: Boolean? = null,
    val saved: Boolean = false,
    val sessionId: String? = null,
)

/**
 * P05 完成与自评。
 *
 * ## 三条硬约束
 *
 * 1. **覆盖率不足时整块不占位**（不是显示「--」，是不出现）——设计稿明确写了。
 * 2. **选「有点不适」→ 停止并建议就医，无加量建议**（FR-09 / AC-11）。
 * 3. **不展示动作正确率、肌力变化、疗效数字**（§5.6 红线）。
 */
class CompleteViewModel(private val container: AppContainer, sessionId: String?) : ViewModel() {

    private val _state = MutableStateFlow(CompleteUiState(sessionId = sessionId))
    val state: StateFlow<CompleteUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val entity = sessionId?.let { container.sessionRepository.findById(it) }
            _state.value = _state.value.copy(
                loading = false,
                durationSec = entity?.durationSec ?: 0,
                completedReps = entity?.completedReps ?: 0,
                plannedReps = entity?.plannedReps ?: 0,
                pauseCount = entity?.pauseCount ?: 0,
                // 双重门限：数据层保存时已把不达标的指标置空，这里再判一次
                signalCoverage = entity?.signalCoverage
                    ?.takeIf { it >= com.diqi.demo.data.repo.SessionRepository.MIN_COVERAGE_FOR_METRICS },
                avgHr = entity?.avgHr,
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HrWhite)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag(UiTag.P05_ROOT),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 22.dp, end = 22.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 56 圆底 + 标题 26/700 + 副文 14/400
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(HrGoodContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text("✓", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = HrGood)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "完成今天的练习",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = HrInk,
                )
                Text(
                    "坚持下来就是进步，明天同一时间再来一次。",
                    fontSize = 14.sp,
                    color = HrInkMuted,
                )
            }

            // 三个数据卡 h86 r14 #F8FAFC
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    label = "引导时长",
                    value = mmss(state.durationSec),
                    tag = UiTag.P05_DURATION,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "完成次数",
                    value = state.completedReps.toString(),
                    tag = UiTag.P05_REPS,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "暂停",
                    value = "${state.pauseCount} 次",
                    tag = UiTag.P05_PAUSE_COUNT,
                    modifier = Modifier.weight(1f),
                )
            }

            // 覆盖率卡：不达标整块不出现（设计稿要求）
            val cov = state.signalCoverage
            if (cov != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(HrGoodContainer)
                        .padding(16.dp)
                        .testTag(UiTag.P05_COVERAGE),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "本次信号覆盖率",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HrGood,
                        )
                        Text(
                            text = state.avgHr?.let { "数据完整，平均心率 ${it.toInt()} bpm" }
                                ?: "数据完整",
                            fontSize = 11.sp,
                            color = HrGood.copy(alpha = 0.8f),
                        )
                    }
                    Text(
                        "${(cov * 100).toInt()}%",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HrGood,
                    )
                }
            }

            // 难度
            Text("今天这个强度感觉如何？", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HrInk)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Difficulty.entries.forEach { d ->
                    Pill(
                        label = d.label,
                        selected = state.difficulty == d,
                        selectedBg = HrInk,
                        selectedFg = HrWhite,
                        selectedStroke = HrInk,
                        unselectedFg = HrSlate700,
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

            // 不适
            Text("练习中有没有不舒服？", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HrInk)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill(
                    label = "没有不适",
                    selected = state.discomfort == false,
                    selectedBg = HrGoodContainer,
                    selectedFg = HrGoodDeep,
                    selectedStroke = HrGood,
                    unselectedFg = HrSlate700,
                    tag = UiTag.P05_DISCOMFORT_TOGGLE,
                    onClick = { vm.selectDiscomfort(false) },
                    modifier = Modifier.weight(1f),
                )
                Pill(
                    label = "有点不适",
                    selected = state.discomfort == true,
                    selectedBg = HrGoodContainer,
                    selectedFg = HrGoodDeep,
                    selectedStroke = HrGood,
                    unselectedFg = HrSlate700,
                    tag = UiTag.P05_DISCOMFORT_BANNER,
                    onClick = { vm.selectDiscomfort(true) },
                    modifier = Modifier.weight(1f),
                )
            }

            // AC-11：有不适 → 停止 + 就医，且**不给加量建议**
            if (state.discomfort == true) {
                Text(
                    text = "先暂停练习，并尽快咨询医生或专业人员。等情况明确后再决定是否继续。",
                    fontSize = 13.sp,
                    color = HrWarn,
                    modifier = Modifier.testTag(UiTag.P05_DISCOMFORT_NOTICE),
                )
            }

            Spacer(Modifier.height(8.dp))
        }

        // 保存并返回（h56 r28，固定底部）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 20.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(HrInk)
                .clickable(role = Role.Button, onClick = onDone)
                .testTag(UiTag.P05_DONE_BUTTON),
            contentAlignment = Alignment.Center,
        ) {
            Text("保存并返回首页", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = HrWhite)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, tag: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(86.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(HrFaintCard)
            .border(BorderStroke(1.dp, HrLine), RoundedCornerShape(14.dp))
            .padding(start = 12.dp, top = 12.dp)
            .testTag(tag),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = HrInkTertiary)
        Text(
            value,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = HrInk,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        )
    }
}

@Composable
private fun Pill(
    label: String,
    selected: Boolean,
    selectedBg: Color,
    selectedFg: Color,
    selectedStroke: Color,
    unselectedFg: Color,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(if (selected) selectedBg else HrWhite)
            .border(
                BorderStroke(1.dp, if (selected) selectedStroke else HrTrack),
                RoundedCornerShape(25.dp),
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) selectedFg else unselectedFg,
        )
    }
}

/** 「192 秒」→「3:12」，与设计稿数据卡的值格式一致。 */
private fun mmss(totalSec: Long): String {
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

private val HrWhite = Color(0xFFFFFFFF)
private val HrInk = Color(0xFF0F1A24)
private val HrInkMuted = Color(0xFF64748B)
private val HrInkTertiary = Color(0xFF94A3B8)
private val HrLine = Color(0xFFE2E8F0)
private val HrTrack = Color(0xFFCBD5E1)
private val HrFaintCard = Color(0xFFF8FAFC)
private val HrGood = Color(0xFF047857)
private val HrGoodDeep = Color(0xFF065F46)
private val HrGoodContainer = Color(0xFFE7F6EC)
private val HrWarn = Color(0xFFB45309)
private val HrSlate700 = Color(0xFF334155)