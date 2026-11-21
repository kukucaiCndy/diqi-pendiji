package com.diqi.demo.ui.screens.training

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diqi.demo.domain.training.TrainingFrame
import com.diqi.demo.domain.training.TrainingPhase
import com.diqi.demo.domain.training.TrainingStatus
import com.diqi.demo.ui.UiTag
import com.diqi.demo.ui.rememberContainerViewModel
import com.diqi.demo.ui.theme.HrType

/**
 * P04 训练中（运行 / 暂停 / 建信号三态）。
 *
 * ## 数值来源
 *
 * 全部取自 `docs/DESIGN-SPEC.md` 的 `P04-a` 段落，换算 1 设计 px = 1sp / 1dp。
 *
 * ## 这一版纠正的结构性错误
 *
 * 上一版以**节律圆环**为绝对主视觉（决策 D10 保留了设计稿之外的圆环）。
 * 复核设计稿：**P04 里没有圆环**，主视觉是
 * 视频区(300) → 深色阶段时间轴 → 当前阶段头 + 进度条 → HR/RR 双波形卡 → 双按钮。
 * 圆环已删除。
 *
 * ## 波形图的处理（绝不画假曲线）
 *
 * 设计稿的 HR/RR 卡里有真实曲线（`path` 节点，stroke `#E11D48` / `#2563EB`）。
 * 本项目还没有 FacePhys SDK，**没有任何真实波形数据**，所以：
 * - 保留卡片结构、通道标、网格线（这些是静态装饰）
 * - **不画曲线**，数值位显示「--」
 * 出假曲线比不画更糟 —— 那是伪造生理数据。
 */
@Composable
fun TrainingScreen(
    onFinished: (String?) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = rememberContainerViewModel { TrainingViewModel(it) }
    val meta by vm.state.collectAsState()
    val frame by vm.frames.collectAsState()
    val metric by vm.metric.collectAsState()

    LaunchedEffect(meta.finished) {
        if (meta.finished) onFinished(meta.savedSessionId)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HrWhite)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag(UiTag.P04_ROOT),
    ) {
        // 视频区固定 240：设计稿画布 880，本机 827dp，若照搬 300 会把底部按钮挤出屏幕。
        // 取 240 后「时间轴 + 阶段头 + 双卡 + 免责 + 按钮」全部可见。
        VideoArea(ready = metric !is MetricUiState.NotEnabled, height = 240.dp)

        PhaseTimelineBar(frame = frame)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 14.dp),
        ) {
            // ── 当前阶段头 ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "当前阶段　CURRENT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HrInkTertiary,
                    )
                    Text(
                        text = frame.phase.displayName,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = HrInk,
                        modifier = Modifier.testTag(UiTag.P04_PHASE_NAME),
                    )
                    Text(
                        text = frame.phase.hint,
                        fontSize = 14.sp,
                        color = HrInkSecondary,
                    )
                }
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = frame.countdownText,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HrInk,
                    )
                    Text(
                        text = "/ " + secText(frame.phaseTotalMs),
                        fontSize = 12.sp,
                        color = HrInkTertiary,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 进度轨 4dp
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(HrLine),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(frame.phaseProgress)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(HrBrand),
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── HR / RR 双卡 ────────────────────────────────────────────
            WaveCard(
                label = "HR　心率　bpm",
                value = metric.heartRateOrNull(),
                valueColor = HrHeartRate,
                tag = UiTag.P04_METRIC_HR,
            )
            Spacer(Modifier.height(10.dp))
            WaveCard(
                label = "RR　呼吸率　次/分",
                value = metric.breathingRateOrNull(),
                valueColor = HrBreathing,
                tag = UiTag.P04_METRIC_RR,
            )

            Spacer(Modifier.height(10.dp))

            // 状态行：建立中 / 信号暂失 / 信号弱 / 信号一般 / 演示水印
            StatusLine(metric = metric, demo = meta.demoMode)

            Spacer(Modifier.height(6.dp))

            Text(
                text = "曲线与数值反映身体状态，不代表盆底肌收缩质量",
                fontSize = 10.sp,
                color = HrInkTertiary,
                modifier = Modifier.testTag(UiTag.P04_DISCLAIMER),
            )

        }

        // ── 按钮行 h50 r25（固定在底部，任何时候都可见）──────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .border(BorderStroke(1.dp, HrTrack), RoundedCornerShape(25.dp))
                        .clickable(role = Role.Button) { vm.togglePause() }
                        .testTag(UiTag.P04_PAUSE_BUTTON),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PauseGlyph(HrInk, Modifier.size(15.dp), paused = meta.paused)
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = if (meta.paused) "继续" else "暂停",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HrInk,
                    )
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(HrInk)
                        .clickable(role = Role.Button) { vm.requestStop() }
                        .testTag(UiTag.P04_STOP_BUTTON),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(13.dp)
                            .background(HrWhite),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = "结束",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = HrWhite,
                    )
            }
        }
    }

    // ── 弹窗（M03 恢复 / M01 结束确认 / 保存失败）──────────────────────
    meta.pendingRecovery?.let { snap ->
        AlertDialog(
            onDismissRequest = { },
            title = { Text("上次的训练还没做完", style = HrType.titleSmall, color = HrInk) },
            text = {
                Text(
                    "你已经完成 ${snap.completedRepsIfAny()}，要继续吗？",
                    style = HrType.bodyMedium,
                    color = HrInkSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.continueFromSnapshot() }) {
                    Text("继续", style = HrType.button, color = HrBrand)
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.discardSnapshot() }) {
                    Text("丢弃", style = HrType.button, color = HrInkSecondary)
                }
            },
        )
    }

    if (meta.showStopConfirm) {
        AlertDialog(
            onDismissRequest = { vm.cancelStop() },
            title = { Text("结束这次训练？", style = HrType.titleSmall, color = HrInk) },
            text = {
                Text(
                    "已完成的 ${vm.completedRepsNow()} 次会保存下来，不会补算没做完的次数。",
                    style = HrType.bodyMedium,
                    color = HrInkSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.confirmStop() }) {
                    Text("结束", style = HrType.button, color = HrBrand)
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.cancelStop() }) {
                    Text("继续练", style = HrType.button, color = HrInkSecondary)
                }
            },
        )
    }

    if (meta.saveFailed) {
        AlertDialog(
            onDismissRequest = { vm.abandonOnSaveFailure() },
            title = { Text("这次记录没保存成功", style = HrType.titleSmall, color = HrInk) },
            text = {
                Text(
                    "可能是存储空间满了。可以再试一次，或者先不保存。",
                    style = HrType.bodyMedium,
                    color = HrInkSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.retrySave() }) {
                    Text("再试一次", style = HrType.button, color = HrBrand)
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.abandonOnSaveFailure() }) {
                    Text("先不保存", style = HrType.button, color = HrInkSecondary)
                }
            },
        )
    }
}

// ── 视频区（h300，#1B2A38）─────────────────────────────────────────────

@Composable
private fun VideoArea(ready: Boolean, height: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .background(HrVideoBg),
    ) {
        // 底部渐变（#0F1A2400 → #0F1A24E6，180°）
        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(100.dp)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color(0xE60F1A24))),
                ),
        )

        if (!ready) {
            Text(
                text = "未开启摄像头\n不影响练习",
                fontSize = 13.sp,
                color = Color(0xFF8FA3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            // 人脸框 184×228 r96 stroke #5EEAD4
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(width = 184.dp, height = 228.dp)
                    .border(
                        BorderStroke(1.5.dp, HrAccent),
                        RoundedCornerShape(96.dp),
                    )
                    .testTag(UiTag.P04_FACE_BOX),
            )
        }

        // 相机角标 130×28 r14 #0F1A24B3
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 12.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xB30F1A24))
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (ready) HrAccent else Color(0xFF5E7386)),
            )
            Text(
                text = if (ready) "摄像头工作中" else "未开启摄像头",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = HrWhite,
            )
        }

        if (ready) {
            Text(
                text = "已对准　肩颈放松",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = HrWhite,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp),
            )
        }
    }
}

// ── 阶段时间轴（深色，#0F1A24）────────────────────────────────────────

@Composable
private fun PhaseTimelineBar(frame: TrainingFrame) {
    val order = when (frame.phase) {
        TrainingPhase.IDLE, TrainingPhase.PREPARATION -> 0
        TrainingPhase.CONTRACT -> 1
        TrainingPhase.HOLD -> 2
        TrainingPhase.RELAX -> 3
        TrainingPhase.COMPLETE -> 4
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(HrInk)
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp)
            .testTag(UiTag.P04_TIMELINE),
    ) {
        Text(
            "阶段时间轴　PHASE TIMELINE",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF5E7386),
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PHASE_STEPS.forEachIndexed { i, name ->
                val state = when {
                    i < order -> StepState.DONE
                    i == order -> StepState.CURRENT
                    else -> StepState.PENDING
                }
                TimelineStep(
                    name = name,
                    duration = "00:" + "%02d".format(5),
                    state = state,
                )
            }
            // 第 5 列是次数计数，不是阶段
            TimelineStep(
                name = "第 ${frame.repIndex} 次",
                duration = "—",
                state = StepState.PENDING,
            )
        }
    }
}

private enum class StepState { DONE, CURRENT, PENDING }

@Composable
private fun TimelineStep(name: String, duration: String, state: StepState) {
    val dotSize = when (state) {
        StepState.CURRENT -> 22.dp
        StepState.DONE -> 18.dp
        StepState.PENDING -> 16.dp
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        when (state) {
            StepState.DONE -> Box(
                Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(HrGood),
            )
            StepState.CURRENT -> Box(
                Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(HrAccent),
            )
            StepState.PENDING -> Box(
                Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(HrVideoBg)
                    .border(1.dp, Color(0xFF3A4D5E), CircleShape),
            )
        }
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = if (state == StepState.CURRENT) FontWeight.Bold else FontWeight.SemiBold,
            color = when (state) {
                StepState.DONE -> Color(0xFFC6D5E2)
                StepState.CURRENT -> HrAccent
                StepState.PENDING -> Color(0xFF6E8598)
            },
        )
        Text(
            text = duration,
            fontSize = 9.sp,
            color = if (state == StepState.CURRENT) HrAccent else Color(0xFF5F7285),
        )
    }
}

// ── 波形卡（有结构无假曲线）───────────────────────────────────────────

@Composable
private fun WaveCard(label: String, value: String, valueColor: Color, tag: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(HrWhite)
            .border(BorderStroke(1.dp, HrLine), RoundedCornerShape(12.dp))
            .testTag(tag),
    ) {
        // 网格线（设计稿静态装饰，3 条 #F1F5F9）
        Canvas(Modifier.fillMaxSize()) {
            val gap = size.height / 4f
            for (i in 1..3) {
                drawLine(
                    color = Color(0xFFF1F5F9),
                    start = Offset(0f, gap * i),
                    end = Offset(size.width, gap * i),
                    strokeWidth = 1f,
                )
            }
        }
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = HrInkTertiary,
            modifier = Modifier.padding(start = 12.dp, top = 10.dp),
        )
        Text(
            text = value,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 12.dp, top = 6.dp),
        )
    }
}

/** 状态行：把四态与演示水印放在一起，位置在建指标卡下方。 */
@Composable
private fun StatusLine(metric: MetricUiState, demo: Boolean) {
    val (text, color) = when (metric) {
        is MetricUiState.NotEnabled -> "未开启感知" to HrInkTertiary
        is MetricUiState.Establishing -> "信号建立中 ${metric.elapsedSec}s / 约 30s" to HrBrand
        is MetricUiState.NoData -> "信号暂失" to HrWarn
        is MetricUiState.Invalid -> "信号弱，" to HrWarn
        is MetricUiState.Degraded -> "信号一般" to HrWarn
        is MetricUiState.Valid -> null to HrGood
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(UiTag.P04_METRIC_QUALITY),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text ?: "信号良好",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
        val banner = when (metric) {
            is MetricUiState.NoData -> metric.banner
            is MetricUiState.Degraded -> metric.banner
            else -> null
        }
        if (banner != null) {
            Text(text = banner, fontSize = 11.sp, color = HrWarn)
        } else if (demo) {
            Text(
                text = "模拟数据 · 仅用于演示",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = HrWarn,
                modifier = Modifier.testTag(UiTag.P04_DEMO_WATERMARK),
            )
        }
    }
}

// ── 手绘图标 ───────────────────────────────────────────────────────────

@Composable
private fun PauseGlyph(color: Color, modifier: Modifier, paused: Boolean) {
    Canvas(modifier) {
        if (paused) {
            // 播放三角
            drawPath(
                androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * 0.1f, 0f)
                    lineTo(size.width * 0.95f, size.height / 2f)
                    lineTo(size.width * 0.1f, size.height)
                    close()
                },
                color = color,
            )
        } else {
            val w = size.width * 0.28f
            drawLine(
                color, Offset(size.width * 0.28f, 0f),
                Offset(size.width * 0.28f, size.height), strokeWidth = w, cap = StrokeCap.Round,
            )
            drawLine(
                color, Offset(size.width * 0.72f, 0f),
                Offset(size.width * 0.72f, size.height), strokeWidth = w, cap = StrokeCap.Round,
            )
        }
    }
}

private fun secText(ms: Long): String {
    val s = ((ms + 999) / 1000).toInt().coerceAtLeast(0)
    return "%02d:%02d".format(s / 60, s % 60)
}

/** 指标取值：只有 Valid/Degraded 才有数，其余给「--」（FR-08 四态）。 */
private fun MetricUiState.heartRateOrNull(): String = when (this) {
    is MetricUiState.Valid -> heartRateBpm?.let { it.toInt().toString() } ?: "--"
    is MetricUiState.Degraded -> heartRateBpm?.let { it.toInt().toString() } ?: "--"
    else -> "--"
}

private fun MetricUiState.breathingRateOrNull(): String = when (this) {
    is MetricUiState.Valid -> breathingRatePerMin?.let { it.toInt().toString() } ?: "--"
    is MetricUiState.Degraded -> breathingRatePerMin?.let { it.toInt().toString() } ?: "--"
    else -> "--"
}

private fun Any?.completedRepsIfAny(): String = "数"

private val PHASE_STEPS = listOf("准备", "收缩", "保持", "放松")

// ── 设计稿页面内联色（DESIGN-SPEC §P04-a）─────────────────────────────
private val HrWhite = Color(0xFFFFFFFF)
private val HrInk = Color(0xFF0F1A24)
private val HrInkSecondary = Color(0xFF5C6B7A)
private val HrInkTertiary = Color(0xFF94A3B8)
private val HrLine = Color(0xFFE2E8F0)
private val HrTrack = Color(0xFFCBD5E1)
private val HrBrand = Color(0xFF0F766E)
private val HrGood = Color(0xFF047857)
private val HrWarn = Color(0xFFB45309)
private val HrAccent = Color(0xFF5EEAD4)
private val HrVideoBg = Color(0xFF1B2A38)
private val HrHeartRate = Color(0xFFE11D48)
private val HrBreathing = Color(0xFF2563EB)