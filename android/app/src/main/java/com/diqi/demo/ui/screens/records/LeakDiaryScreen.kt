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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.diqi.demo.data.db.LeakContext
import com.diqi.demo.data.db.LeakFrequency
import com.diqi.demo.ui.UiTag
import com.diqi.demo.ui.rememberContainerViewModel
import kotlinx.coroutines.launch

/**
 * P08 漏尿简易日记。
 *
 * ## 数值来源
 *
 * 布局取自 `docs/DESIGN-SPEC.md` 的 `P08`：大标题 26/700、副文 14/400、
 * 频次卡（r16 p18，五档 h52 r12）、情境卡（r16 p18，标签 h40 r20 padding 14）。
 *
 * ## 一处与设计稿的**有意**差异（有出处）
 *
 * 设计稿五档写「没有 / 1 次 / 2 次 / 3 次 / 更多」，但需求 **FR-12 原文**是
 * 「没有 / 1 次 / 2–3 次 / 4 次及以上 / 不记得」。分档语义不同（2 次 vs 2–3 次），
 * 按需求为准 —— 否则数据无法与临床口径对齐。
 *
 * ## 红线
 *
 * 全部可跳过；仅本地存储；**不生成任何诊断结论**；不进入周报（AC-22/AC-23）。
 */
class LeakDiaryViewModel(private val container: AppContainer) : ViewModel() {
    fun save(frequency: LeakFrequency?, context: LeakContext?) {
        viewModelScope.launch {
            container.leakDiaryRepository.save(System.currentTimeMillis(), frequency, context)
        }
    }

    fun skip() {
        viewModelScope.launch {
            container.leakDiaryRepository.skip(System.currentTimeMillis())
        }
    }
}

@Composable
fun LeakDiaryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = rememberContainerViewModel { LeakDiaryViewModel(it) }
    var frequency by remember { mutableStateOf<LeakFrequency?>(null) }
    var context by remember { mutableStateOf<LeakContext?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HrLkCanvas)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 22.dp)
            .testTag(UiTag.LEAK_ROOT),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("今天有漏尿吗？", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = HrLkInk)
            Text(
                "记下来能帮你和医生看清规律，也可以跳过。",
                fontSize = 14.sp,
                color = HrLkMuted,
            )
        }

        // 频次卡
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(BorderStroke(1.dp, HrLkLine), RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("漏尿次数", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HrLkInk)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LeakFrequency.entries.forEach { f ->
                    FreqChip(
                        label = f.designLabel,
                        selected = frequency == f,
                        tag = when (f) {
                            LeakFrequency.NONE -> UiTag.LEAK_FREQ_NONE
                            LeakFrequency.ONCE -> UiTag.LEAK_FREQ_ONCE
                            LeakFrequency.TWO_TO_THREE -> UiTag.LEAK_FREQ_2_3
                            LeakFrequency.FOUR_PLUS -> UiTag.LEAK_FREQ_4_PLUS
                            LeakFrequency.UNSURE -> UiTag.LEAK_FREQ_UNSURE
                        },
                        onClick = { frequency = f },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // 情境卡（可多选场景，这里单选即可，None 时隐藏）
        if (frequency != null && frequency != LeakFrequency.NONE) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(BorderStroke(1.dp, HrLkLine), RoundedCornerShape(16.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("当时在做什么？可多选", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HrLkInk)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LeakContext.entries.take(3).forEach { c ->
                        ContextChip(
                            label = c.label,
                            selected = context == c,
                            tag = when (c) {
                                LeakContext.COUGH -> UiTag.LEAK_CONTEXT_COUGH
                                LeakContext.SPORT -> UiTag.LEAK_CONTEXT_SPORT
                                LeakContext.URGE -> UiTag.LEAK_CONTEXT_URGE
                                else -> UiTag.LEAK_CONTEXT_OTHER
                            },
                            onClick = { context = c },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LeakContext.entries.drop(3).forEach { c ->
                        ContextChip(
                            label = c.label,
                            selected = context == c,
                            tag = UiTag.LEAK_CONTEXT_OTHER,
                            onClick = { context = c },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // 保存
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(if (frequency != null) HrLkInk else HrLkTrack)
                .clickable(enabled = frequency != null, role = Role.Button) {
                    vm.save(frequency, context)
                    onBack()
                }
                .testTag(UiTag.LEAK_SAVE_BUTTON),
            contentAlignment = Alignment.Center,
        ) {
            Text("保存", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        // 跳过（设计稿明确"也可以跳过"）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .border(BorderStroke(1.dp, HrLkTrack), RoundedCornerShape(26.dp))
                .clickable(role = Role.Button) {
                    // 跳过也写一行 skipped=true：与"从未接触"可区分（AC-23）
                    vm.skip()
                    onBack()
                }
                .testTag(UiTag.LEAK_SKIP_BUTTON),
            contentAlignment = Alignment.Center,
        ) {
            Text("跳过这一步", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = HrLkInk)
        }
    }
}

@Composable
private fun FreqChip(
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) HrLkInk else HrLkFaint)
            .border(
                BorderStroke(1.dp, if (selected) HrLkInk else HrLkLine),
                RoundedCornerShape(12.dp),
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else Color(0xFF475569),
        )
    }
}

@Composable
private fun ContextChip(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) HrLkGoodContainer else HrLkFaint)
            .border(
                BorderStroke(1.dp, if (selected) HrLkGood else HrLkLine),
                RoundedCornerShape(20.dp),
            )
            .clickable(role = Role.Checkbox, onClick = onClick)
            .testTag(tag)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) HrLkGoodDeep else Color(0xFF475569),
        )
    }
}

/**
 * 设计稿五档标签 vs 需求 FR-12 原文的差异：**按需求**。
 * 设计「2 次 / 3 次 / 更多」与需求「2–3 次 / 4 次及以上 / 不记得」语义不同，
 * 分档必须与临床口径一致，所以取需求。
 */
private val LeakFrequency.designLabel: String
    get() = when (this) {
        LeakFrequency.NONE -> "没有"
        LeakFrequency.ONCE -> "1 次"
        LeakFrequency.TWO_TO_THREE -> "2–3 次"
        LeakFrequency.FOUR_PLUS -> "4 次+"
        LeakFrequency.UNSURE -> "不记得"
    }

private val HrLkCanvas = Color(0xFFF4F6F8)
private val HrLkInk = Color(0xFF0F1A24)
private val HrLkMuted = Color(0xFF64748B)
private val HrLkLine = Color(0xFFE2E8F0)
private val HrLkTrack = Color(0xFFCBD5E1)
private val HrLkFaint = Color(0xFFF8FAFC)
private val HrLkGood = Color(0xFF047857)
private val HrLkGoodDeep = Color(0xFF065F46)
private val HrLkGoodContainer = Color(0xFFE7F6EC)
