package com.diqi.demo.ui.screens.training

import androidx.camera.view.PreviewView
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight as TextFontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.diqi.demo.domain.training.TrainingFrame
import com.diqi.demo.domain.training.TrainingPhase
import com.diqi.demo.domain.training.TrainingStatus
import com.diqi.demo.sensor.camera.CameraController
import com.diqi.demo.sensor.camera.fillParent
import com.diqi.demo.ui.UiTag
import com.diqi.demo.ui.components.HrBottomSheetDialog
import com.diqi.demo.ui.components.HrCenterDialog
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
    /**
     * 本次训练是否启用摄像头。
     *
     * 用户在「训练」Tab 上做的选择，通过路由参数传进来。
     * cameraEnabled=true 且 BuildConfig.FACEPHYS_APP_KEY 非空时，
     * 训练页渲染真实相机预览 + RppgSensor 真实指标；
     * 否则如实显示「未开启感知」—— 不因为"开了摄像头"就假装有数据。
     */
    cameraEnabled: Boolean = false,
) {
    val vm = rememberContainerViewModel { TrainingViewModel(it, cameraEnabled = cameraEnabled) }
    val meta by vm.state.collectAsState()
    val frame by vm.frames.collectAsState()
    val metric by vm.metric.collectAsState()
    // 1Hz 真实读数历史（SDK 无波形接口）：喂给 WaveCard 画趋势线
    val hrHistory by vm.hrHistory.collectAsState()
    val rrHistory by vm.rrHistory.collectAsState()
    // 波形门控：只看**窗口是否攒够真实数据**（设计稿 P04-a:326），不能看 metric 状态。
    // 用 metric（Valid/Degraded）当门控会把「有数据但 SQI<0.50 判 INVALID」误当成
    // 「还没开始采集」，卡片永远停在「信号建立中」骨架（真机实测症状）。
    // 数值显示仍由 QualityGate/SQI 门控，两件事不混。
    val hasWindow by vm.windowReady.collectAsState()
    val textMeasurer = rememberTextMeasurer()

    // 真实相机预览（照 CalibrateScreen:89-140 范本）：
    // isRealSensor 时渲染 PreviewView + CameraController，否则维持现有占位渲染。
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isReal = meta.isRealSensor
    // remember(isReal) 必须带 key：isReal 来自 VM 异步 init，首帧 false。
    // 无 key 时首帧把 previewView 记为 null 并永久沿用 → 相机训练无预览。
    val previewView = remember(isReal) {
        if (isReal) {
            PreviewView(context).apply {
                fillParent()
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        } else null
    }
    val camera = remember(isReal) {
        if (isReal) CameraController(context, lifecycleOwner, vm.analyzer) else null
    }
    DisposableEffect(isReal) {
        val cam = camera
        val pv = previewView
        if (cam != null && pv != null) {
            cam.start(pv)
        }
        onDispose {
            cam?.release()
        }
    }

    // 首次检测到人脸时锁定 AE/AWB（AWB 已对当前场景收敛，锁定而非禁用）
    LaunchedEffect(meta.isRealSensor, meta.faceFound) {
        if (meta.isRealSensor && meta.faceFound) camera?.lockAeAwb()
    }

    LaunchedEffect(meta.finished) {
        if (meta.finished) onFinished(meta.savedSessionId)
    }

    // FR-14 全屏演示水印：覆盖层画在内容之上（8% 透明度不影响阅读），
    // 不加 clickable/pointerInput，触摸穿透。容器 Box 只为承载覆盖层。
    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HrWhite)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag(UiTag.P04_ROOT),
    ) {
        // 视频区固定 240：设计稿画布 880，本机 827dp，若照搬 300 会把底部按钮挤出屏幕。
        // 取 240 后「时间轴 + 阶段头 + 双卡 + 免责 + 按钮」全部可见。
        VideoArea(
            ready = metric !is MetricUiState.NotEnabled,
            height = 240.dp,
            isRealSensor = isReal,
            previewView = previewView,
            cameraError = camera?.lastError,
        )

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
                history = hrHistory,
                hasEnoughWindow = hasWindow,
            )
            Spacer(Modifier.height(10.dp))
            WaveCard(
                label = "RR　呼吸率　次/分",
                value = metric.breathingRateOrNull(),
                valueColor = HrBreathing,
                tag = UiTag.P04_METRIC_RR,
                history = rrHistory,
                hasEnoughWindow = hasWindow,
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

            Spacer(Modifier.height(10.dp))

            // ── 重复提示按钮（FR-05 P0）──────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(BorderStroke(1.dp, HrTrack), RoundedCornerShape(20.dp))
                    .clickable(role = Role.Button) { vm.repeatVoice() }
                    .testTag(UiTag.P04_REPEAT_VOICE_BUTTON),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "重复提示",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = HrInkSecondary,
                )
            }

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

    // FR-14 全屏演示水印覆盖层：画在内容之上，不加 clickable/pointerInput，触摸穿透。
    if (meta.demoMode) {
        Box(
            Modifier
                .matchParentSize()
                .testTag(UiTag.DEMO_WATERMARK)
                .drawBehind { drawDemoWatermark(size, textMeasurer) },
        )
    }

    // ── 弹窗（M03 恢复 / M01 结束确认 / 保存失败）──────────────────────
    meta.pendingRecovery?.let { snap ->
        // M03：居中弹窗。主操作=继续，次操作=丢弃（红字）
        HrCenterDialog(
            title = "上次练习没有完成",
            message = "检测到一次未结束的练习。你可以接着上次的进度继续，或者丢弃重新开始。"
                + "（上次练了 ${secText(snap.elapsedActiveMs)}）",
            primaryLabel = "继续上次进度",
            onPrimary = { vm.continueFromSnapshot() },
            secondaryLabel = "丢弃并重新开始",
            onSecondary = { vm.discardSnapshot() },
        )
    }

    if (meta.showStopConfirm) {
        // M01：底部弹窗。设计稿把「继续练习」做成主按钮、「仍然结束」做成红字次按钮
        // —— 意图是避免用户误结束。
        HrBottomSheetDialog(
            title = "要结束今天的练习吗？",
            message = "已练完 ${vm.completedRepsNow()} 次，提前结束不会补全剩下的次数。",
            primaryLabel = "继续练习",
            onPrimary = { vm.cancelStop() },
            secondaryLabel = "仍然结束",
            onSecondary = { vm.confirmStop() },
            onDismiss = { vm.cancelStop() },
        )
    }

    if (meta.saveFailed) {
        HrCenterDialog(
            title = "这次记录没保存成功",
            message = "可能是存储空间满了。可以再试一次，或者先不保存。",
            primaryLabel = "再试一次",
            onPrimary = { vm.retrySave() },
            secondaryLabel = "先不保存",
            onSecondary = { vm.abandonOnSaveFailure() },
            onDismiss = { vm.abandonOnSaveFailure() },
        )
    }
    } // Box(Modifier.fillMaxSize())
}

// ── 视频区（h300，#1B2A38）─────────────────────────────────────────────

@Composable
private fun VideoArea(
    ready: Boolean,
    height: androidx.compose.ui.unit.Dp,
    isRealSensor: Boolean = false,
    previewView: PreviewView? = null,
    cameraError: String? = null,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .background(HrVideoBg),
    ) {
        // 真实相机预览（照 CalibrateScreen:89-140 范本）
        if (isRealSensor && previewView != null) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(UiTag.P04_PREVIEW),
            )
        }

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

        if (!isRealSensor && !ready) {
            Text(
                text = "未开启摄像头\n不影响练习",
                fontSize = 13.sp,
                color = Color(0xFF8FA3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )
        } else if (isRealSensor && cameraError != null) {
            // 相机错误文案（照校准页 cameraError 处理）
            Text(
                text = cameraError,
                fontSize = 13.sp,
                color = Color(0xFFFBBF24),
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
                    .background(if (ready || isRealSensor) HrAccent else Color(0xFF5E7386)),
            )
            Text(
                text = if (isRealSensor) "摄像头工作中" else if (ready) "摄像头工作中" else "未开启摄像头",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = HrWhite,
            )
        }

        if (ready || isRealSensor) {
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

// ── 波形卡（真实趋势线 / 骨架）────────────────────────────────────────
//
// 曲线数据来自 ViewModel 的 1Hz 采样历史（SDK 没有 PPG 波形接口，见
// TrainingViewModel.hrHistory 注释）。历史不足 2 点或窗口还没建够时画骨架
// （设计稿 P04-c：虚线底 + 「信号建立中」），够了一律画**真实读数**连成的线，
// 台阶平台是 SDK 每 ~10s 才刷新一次的固有形态，不插值。

@Composable
private fun WaveCard(
    label: String,
    value: String,
    valueColor: Color,
    tag: String,
    history: List<Float>,
    hasEnoughWindow: Boolean,
) {
    val showCurve = history.size >= 2 && hasEnoughWindow
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

        // 曲线区：水平 inset 12dp、高 64dp（设计稿 path 404×64）
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, end = 12.dp, bottom = 4.dp)
                .fillMaxWidth()
                .height(64.dp),
        ) {
            if (showCurve) {
                Canvas(Modifier.fillMaxSize()) {
                    val points = normalizeCurvePoints(history, size.width, size.height)
                    if (points.size >= 2) {
                        val path = Path()
                        path.moveTo(points[0].x, points[0].y)
                        for (i in 1 until points.size) {
                            path.lineTo(points[i].x, points[i].y)
                        }
                        drawPath(
                            path = path,
                            color = valueColor,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                }
            } else {
                // 骨架：虚线底 404×1 #E2E8F0 +「信号建立中」11sp #94A3B8（P04-c）
                Canvas(Modifier.fillMaxSize()) {
                    val y = size.height / 2f
                    drawLine(
                        color = HrLine,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(4.dp.toPx(), 4.dp.toPx()),
                        ),
                    )
                }
                // 文案只在**窗口真未满**时出现：窗口满了之后每秒都会采点
                // （TrainingViewModel.recordHistory 的采点口径 = 窗口满 + 值非 null，
                // 不再受 SQI 判定限制），此刻还没画出线只是历史点不足 2 个，
                // 不能再用「信号建立中」误导用户。
                if (!hasEnoughWindow) {
                    Text(
                        text = "信号建立中",
                        fontSize = 11.sp,
                        color = HrInkTertiary,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 2.dp),
                    )
                }
            }
        }
    }
}

/** 状态行：把四态与演示水印放在一起，位置在建指标卡下方。 */
@Composable
private fun StatusLine(metric: MetricUiState, demo: Boolean) {
    val (text, color) = when (metric) {
        is MetricUiState.NotEnabled -> "未开启感知" to HrInkTertiary
        is MetricUiState.Establishing -> "信号建立中 ${metric.elapsedSec}s / 约 ${metric.totalSec}s" to HrBrand
        is MetricUiState.NoData -> "信号暂失" to HrWarn
        // SQI 低于门限：数值门控生效（数字显示「--」），这里给出完整的一句「该怎么办」，
        // 不能只留半句「信号弱，」（旧文案尾逗号后无下文）。
        is MetricUiState.Invalid -> "信号弱，请调整位置或光线" to HrWarn
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

/**
 * 全屏演示水印（FR-14 / AC-19）。
 *
 * 半透明（约 8% 不透明度）、-30° 斜向重复平铺「模拟数据 · 仅用于演示」。
 * 用 [drawBehind] 绘制，不拦截任何点击。
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDemoWatermark(
    canvasSize: Size,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
) {
    val text = "模拟数据 · 仅用于演示"
    val rotationDeg = -30f
    // 平铺间距
    val stepX = 280f
    val stepY = 160f
    // 旋转后需覆盖更大的范围，取对角线长度作为半边
    val halfDiag = kotlin.math.sqrt(
        canvasSize.width * canvasSize.width + canvasSize.height * canvasSize.height,
    ) / 2f

    val textResult = textMeasurer.measure(
        text = AnnotatedString(text),
        style = TextStyle(
            fontSize = 14.sp,
            color = Color(0x140F1A24), // ~8% 不透明度
        ),
        overflow = TextOverflow.Visible,
    )

    rotate(rotationDeg, pivot = Offset(canvasSize.width / 2f, canvasSize.height / 2f)) {
        var y = -halfDiag
        while (y <= halfDiag) {
            var x = -halfDiag
            while (x <= halfDiag) {
                drawText(textResult, topLeft = Offset(x, y))
                x += stepX
            }
            y += stepY
        }
    }
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