package com.diqi.demo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.diqi.demo.ui.UiTag
import com.diqi.demo.ui.theme.HrColor
import com.diqi.demo.ui.theme.HrSpacing
import com.diqi.demo.ui.theme.HrType
import androidx.compose.material3.Text

/**
 * 节律圆环。
 *
 * ## 为什么必须保留这个圆环（决策 D10）
 *
 * 设计稿用「阶段时间轴」替代了圆环，但需求 §5.6 的文案红线里有一条明文「必须」：
 * **圆环必须标注「节律提示」，不得暗示实时肌肉状态**。
 * 这是防止用户把图形误读成"我的肌肉正在用力"的合规要求。
 * 所以时间轴与圆环**并存**：时间轴在上（看进度），圆环+倒计时居中（跟着节奏做）。
 *
 * ## 为什么环上的进度是"时间"而不是"力度"
 *
 * 环的填充完全由阶段剩余时间驱动。它**不**反映任何生理量，
 * 环内也不出现任何数字以外的指标 —— 这是 `「节律提示」` 标注能成立的唯一前提。
 */
@Composable
fun RhythmRing(
    progress: Float,
    centerText: String,
    totalText: String,
    activeTone: Boolean,
    modifier: Modifier = Modifier,
) {
    val ringColor = if (activeTone) HrColor.Brand else HrColor.Track
    Box(
        modifier = modifier
            .size(232.dp)
            .testTag(UiTag.P04_RHYTHM_RING)
            .semantics {
                contentDescription = "节律提示 $centerText，$totalText"
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(232.dp)) {
            val stroke = 18.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            // 底环
            drawArc(
                color = HrColor.Line,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            // 进度环：从 12 点方向顺时针
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = centerText,
                style = HrType.monoDisplay,
                color = HrColor.Ink,
                modifier = Modifier.testTag(UiTag.P04_COUNTDOWN),
            )
            Text(totalText, style = HrType.labelSmall, color = HrColor.WeakText)
            Box(Modifier.size(4.dp))
            // 常驻标注（§5.6 合规红线）
            Text(
                text = "节律提示",
                style = HrType.labelSmall,
                color = HrColor.WeakText,
                modifier = Modifier.testTag(UiTag.P04_RHYTHM_RING_LABEL),
            )
        }
    }
}