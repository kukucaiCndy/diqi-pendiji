package com.diqi.demo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.diqi.demo.domain.training.TrainingFrame
import com.diqi.demo.domain.training.TrainingPhase
import com.diqi.demo.domain.training.TrainingStatus
import com.diqi.demo.ui.UiTag
import com.diqi.demo.ui.theme.HrColor
import com.diqi.demo.ui.theme.HrRadius
import com.diqi.demo.ui.theme.HrSpacing
import com.diqi.demo.ui.theme.HrType

/**
 * 阶段时间轴（深底面版，取自设计稿 P04 的「阶段时间轴」）。
 *
 * 与圆环分工：时间轴回答"我进行到哪了"，圆环回答"这一秒该做什么"。
 * 四个阶段 + 当前次数，节点三态：已用 / 当前 / 未到。
 */
@Composable
fun PhaseTimeline(
    frame: TrainingFrame,
    params: com.diqi.demo.domain.training.TrainingParams,
    modifier: Modifier = Modifier,
) {
    val phases = listOf(
        TrainingPhase.PREPARATION to params.prepSec,
        TrainingPhase.CONTRACT to params.contractSec,
        TrainingPhase.HOLD to params.holdSec,
        TrainingPhase.RELAX to params.relaxSec,
    )

    val currentOrder = when (frame.phase) {
        TrainingPhase.IDLE, TrainingPhase.PREPARATION -> 0
        TrainingPhase.CONTRACT -> 1
        TrainingPhase.HOLD -> 2
        TrainingPhase.RELAX -> 3
        TrainingPhase.COMPLETE -> 4
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(HrRadius.cardShape)
            .background(HrColor.Panel)
            .padding(horizontal = HrSpacing.md, vertical = HrSpacing.sm)
            .testTag(UiTag.P04_PHASE_TIMELINE)
            .semantics {
                contentDescription =
                    "阶段时间轴，当前 ${frame.phase.displayName}，第 ${frame.repIndex} 次，共 ${frame.totalReps} 次"
            },
    ) {
        Text("阶段", style = HrType.micro, color = HrColor.TimelineLabel)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            phases.forEachIndexed { index, (phase, sec) ->
                val (dotColor, nameColor, nameWeight) = when {
                    index < currentOrder -> Triple(HrColor.Good, HrColor.TimelineName, HrType.labelSmall)
                    index == currentOrder -> Triple(HrColor.Accent, HrColor.Accent, HrType.label)
                    else -> Triple(HrColor.TimelinePending, HrColor.TimelinePending, HrType.labelSmall)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Dot(dotColor)
                    Text(phase.displayName, style = nameWeight, color = nameColor)
                    Text("${sec}s", style = HrType.micro, color = HrColor.TimelineDuration)
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Dot(if (frame.status == TrainingStatus.COMPLETE) HrColor.Good else HrColor.TimelinePending)
                Text(
                    "第 ${frame.repIndex} 次",
                    style = HrType.labelSmall,
                    color = HrColor.TimelineName,
                    modifier = Modifier.testTag(UiTag.P04_REP_COUNTER),
                )
                Text("/ ${frame.totalReps}", style = HrType.micro, color = HrColor.TimelineDuration)
            }
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color),
    )
}