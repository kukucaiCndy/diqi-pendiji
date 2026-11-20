package com.diqi.demo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.diqi.demo.ui.theme.HrColor
import com.diqi.demo.ui.theme.HrRadius
import com.diqi.demo.ui.theme.HrSize
import com.diqi.demo.ui.theme.HrSpacing
import com.diqi.demo.ui.theme.HrType

/**
 * 全应用共用的按钮与状态组件。
 *
 * ## 为什么集中在这里
 *
 * 需求 FR-15 的三条硬指标（正文 ≥20sp、热区 ≥44dp、对比度 ≥4.5:1）
 * 只有集中实现才可能一致满足。散在各页手写 `Box` + `clickable` 必然
 * 出现"某个按钮只有 36dp 高"的情况，而那是**可访问性缺陷**，不是风格问题。
 */

/** 主按钮：深色底、56dp 高、白色 20sp 字。 */
@Composable
fun HrPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tag: String? = null,
    containerColor: Color = HrColor.Panel,
    contentColor: Color = HrColor.Surface,
) {
    val bg = if (enabled) containerColor else HrColor.Line
    val fg = if (enabled) contentColor else HrColor.InkTertiary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HrSize.primaryButton)
            .clip(HrRadius.controlShape)
            .background(bg)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = text) { onClick() }
            // 合并语义必须放在 clickable **之后**（更外层），
            // 这样无障碍树里"可点"与"可朗读文本"落在同一个节点上。
            // 首版把语义放在 clickable 之前，实测 dump 里出现
            // 「可点节点无 content-desc、描述挂在子节点」的错位。
            .semantics(mergeDescendants = true) { contentDescription = text }
            .let { if (tag != null) it.then(Modifier.testTag(tag)) else it }
            .padding(horizontal = HrSpacing.md, vertical = HrSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = HrType.button, color = fg, textAlign = TextAlign.Center)
    }
}

/** 次按钮：白底描边。 */
@Composable
fun HrSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HrSize.secondaryButton)
            .clip(HrRadius.controlShape)
            .background(HrColor.Surface)
            .border(1.dp, HrColor.Line, HrRadius.controlShape)
            .clickable(role = Role.Button, onClickLabel = text) { onClick() }
            .semantics(mergeDescendants = true) { contentDescription = text }
            .let { if (tag != null) it.then(Modifier.testTag(tag)) else it }
            .padding(horizontal = HrSpacing.md, vertical = HrSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = HrType.buttonSecondary, color = HrColor.Ink, textAlign = TextAlign.Center)
    }
}

/** 状态胶囊（FR-02 感知状态三态、FR-06 摄像头角标共用）。 */
enum class ChipTone { GOOD, WARN, NEUTRAL, DANGER }

@Composable
fun HrStatusChip(
    text: String,
    tone: ChipTone,
    modifier: Modifier = Modifier,
    tag: String? = null,
    leadingDot: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val (bg, fg) = when (tone) {
        ChipTone.GOOD -> HrColor.GoodContainer to HrColor.Good
        ChipTone.WARN -> HrColor.WarnContainer to HrColor.Warn
        ChipTone.NEUTRAL -> HrColor.SubtleContainer to HrColor.InkSecondary
        ChipTone.DANGER -> HrColor.DangerContainer to HrColor.Danger
    }
    Row(
        modifier = modifier
            .heightIn(min = HrSize.minTouchTarget)
            .clip(HrRadius.capsuleShape)
            .background(bg)
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClickLabel = text) { onClick() }
                } else {
                    Modifier
                },
            )
            // 合并语义在 clickable 之后：保证可点与可朗读落在同一节点
            .semantics(mergeDescendants = true) { contentDescription = text }
            .let { if (tag != null) it.then(Modifier.testTag(tag)) else it }
            .padding(horizontal = HrSpacing.sm, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (leadingDot) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(fg))
        }
        Text(text = text, style = HrType.capsule, color = fg)
    }
}

/**
 * 提示条（就医提示、光照提示、状态提示共用）。
 *
 * 需求 FR-01 要求「训练页常驻提示条」—— 常驻意味着它不能被折叠、
 * 不能只弹一次 toast。所以做成可复用的横幅。
 */
@Composable
fun HrBanner(
    text: String,
    tone: ChipTone,
    modifier: Modifier = Modifier,
    tag: String? = null,
) {
    val (bg, fg) = when (tone) {
        ChipTone.GOOD -> HrColor.GoodContainer to HrColor.Good
        ChipTone.WARN -> HrColor.WarnContainer to HrColor.Warn
        ChipTone.NEUTRAL -> HrColor.SubtleContainer to HrColor.InkSecondary
        ChipTone.DANGER -> HrColor.DangerContainer to HrColor.Danger
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(HrRadius.controlShape)
            .background(bg)
            .let { if (tag != null) it.then(Modifier.testTag(tag)) else it }
            .padding(horizontal = HrSpacing.sm, vertical = HrSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HrSpacing.xs),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(fg))
        // 提示条承载的是**正文**（例如常驻就医提示），必须 ≥20sp（FR-15）。
        // 首版用了 labelSmall(14sp)，直接违反适老化红线。
        Text(text = text, style = HrType.bodySmall, color = fg)
    }
}

/** 白色卡片容器。 */
@Composable
fun HrCard(
    modifier: Modifier = Modifier,
    color: Color = HrColor.Surface,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(HrRadius.cardShape)
            .background(color)
            .padding(HrSpacing.cardInner),
    ) {
        content()
    }
}

/**
 * 本周打卡七点（P02）。
 *
 * @param trainedDays 有训练的自然日（0 = 周一 … 6 = 周日）
 * @param todayWeekday 今天对应 0–6
 */
@Composable
fun HrWeekDots(
    trainedDays: Set<Int>,
    todayWeekday: Int,
    modifier: Modifier = Modifier,
    tag: String? = null,
) {
    val labels = listOf("一", "二", "三", "四", "五", "六", "日")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .let { if (tag != null) it.then(Modifier.testTag(tag)) else it }
            .semantics {
                contentDescription =
                    "本周打卡：${labels.filterIndexed { i, _ -> i in trainedDays }.joinToString("、")}" +
                    if (trainedDays.isEmpty()) "本周还没有记录" else "已训练 ${trainedDays.size} 天"
            },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        labels.forEachIndexed { index, label ->
            val done = index in trainedDays
            val isToday = index == todayWeekday
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier
                        .size(HrSize.weekDot)
                        .clip(CircleShape)
                        .background(
                            when {
                                done -> HrColor.Good
                                isToday -> HrColor.Surface
                                else -> HrColor.Surface
                            },
                        )
                        .border(
                            width = if (isToday) 2.dp else 1.dp,
                            color = if (isToday) HrColor.Brand else HrColor.Track,
                            shape = CircleShape,
                        ),
                )
                Text(label, style = HrType.micro, color = HrColor.WeakText)
            }
        }
    }
}

/** 无障碍用：给可点区域补最少 44dp 热区。 */
fun Modifier.minTouchTarget(): Modifier = this.heightIn(min = HrSize.minTouchTarget)

/** 垂直间距简写，避免各处写 Spacer(Modifier.height(N.dp))。 */
@Composable
fun VSpace(height: androidx.compose.ui.unit.Dp) = Spacer(Modifier.height(height))

/** 水平间距简写。 */
@Composable
fun HSpace(width: androidx.compose.ui.unit.Dp) = Spacer(Modifier.width(width))