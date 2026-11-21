package com.diqi.demo.ui.screens.calibrate

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.diqi.demo.BuildConfig
import com.diqi.demo.sensor.camera.CameraController
import com.diqi.demo.sensor.camera.fillParent
import com.diqi.demo.ui.UiTag
import com.diqi.demo.ui.rememberContainerViewModel

/**
 * P03 摄像头校准（可选）。
 *
 * ## 数值来源
 *
 * 全部取自 `docs/DESIGN-SPEC.md` 的 `P03-c`：视频区 h430 `#1B2A38`、
 * 人脸框 232×288 r120 `#5EEAD4`、相机角标 132×30 r15 `#0F1A24B3`、
 * 镜像标 86×30、状态字 15/700 `#5EEAD4`、
 * 下区 padding22 gap16：主提示 28/700、辅助 15/400 `#64748B`、
 * 诊断卡 r12 p[14,16] `#F8FAFC` + `#E2E8F0`（头 12/700 `#475569`，行 11sp）、
 * 主按钮 h56 r28 `#0F1A24` + 17 图标 + 17/700。
 *
 * ## 两点保持不变的硬约束
 *
 * 1. **摄像头是可选**：无需授权即可「跳过，直接训练」完成练习（AC-04）
 * 2. **失败必须可解释**：SDK 不可用时诊断区如实显示原因，不静默失败
 */
@Composable
fun CalibrateScreen(
    onStartWithSensor: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = rememberContainerViewModel { CalibrateViewModel(it) }
    val state by vm.state.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { ok ->
        granted = ok
        vm.onPermissionResult(ok)
    }
    LaunchedEffect(Unit) {
        if (granted) vm.onPermissionResult(true) else launcher.launch(Manifest.permission.CAMERA)
    }

    val previewView = remember {
        PreviewView(context).apply {
            fillParent()
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val camera = remember { CameraController(context, lifecycleOwner, vm.analyzer) }
    DisposableEffect(granted) {
        if (granted) camera.start(previewView)
        onDispose { camera.release() }
    }

    // 三态（设计稿 P03-a/b/c）：建立中 / 未对准（含过小/过大/弱光）/ 就绪
    val phase = phaseOf(state.hint)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag(UiTag.P03_ROOT),
    ) {
        // ── 视频区 h430（本机 827dp 屏高，取 340 保证下区完整可见）────────
        Box(
            Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(HrVideoBg),
        ) {
            if (granted) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(UiTag.P03_PREVIEW),
                )
                // 人脸框 232×288 r120 —— 三态色：建立中白 / 未对准琥珀 / 就绪薄荷
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(width = 232.dp, height = 288.dp)
                        .border(
                            BorderStroke(2.dp, faceBoxColor(phase)),
                            RoundedCornerShape(120.dp),
                        )
                        .testTag(UiTag.P03_FACE_BOX),
                )
                // 未对准时罩一层暗色（P03-b 暗罩 #0F1A2426），降低背景干扰、突出人脸框。
                // 画在人脸框之上、角标/状态字之下（与设计稿层序一致）。
                if (phase == CalibPhase.NO_FACE) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color(0x260F1A24))
                            .testTag(UiTag.P03_FACE_VEIL),
                    )
                }
            } else {
                Text(
                    "未开启摄像头\n不影响练习",
                    fontSize = 13.sp,
                    color = HrMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            // 底部渐变
            Box(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(100.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xD90F1A24)))),
            )

            // 相机角标（左上）
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 12.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color(0xB30F1A24))
                    .padding(horizontal = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (granted) HrAccent else HrMuted),
                )
                Text(
                    if (granted) "摄像头工作中" else "未开启摄像头",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }

            // 镜像标（右上）
            if (granted) {
                Text(
                    "镜像",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 12.dp, top = 12.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color(0xB30F1A24))
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                )
            }

            // 状态字 15/700：建立中白 / 未对准琥珀 / 就绪薄荷（P03-a/b/c）
            Text(
                text = state.statusText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = when (phase) {
                    CalibPhase.BUILDING -> Color.White
                    CalibPhase.NO_FACE -> Color(0xFFFBBF24)
                    CalibPhase.READY -> HrAccent
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, bottom = 14.dp)
                    .testTag(UiTag.P03_STATUS_TEXT),
            )
        }

        // ── 下区 ────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── 三态下区（P03-a 建立中 / P03-b 未对准 / P03-c 就绪）──────
            when (phase) {
                CalibPhase.BUILDING -> {
                    // 120×120 进度环：底 #E2E8F0、进度弧 #0F766E、中心秒数 26/600
                    SignalRing(
                        sec = state.signalSec,
                        progress = state.signalProgress,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            // P03-a 说明行 15/600（替代原先的 28sp 大标题）
                            "正在建立信号 ${state.signalSec}s / 约 30s",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HrInk,
                        )
                        Text(
                            "请保持不动，看向摄像头，正常呼吸",
                            fontSize = 13.sp,
                            color = HrMuted,
                        )
                    }
                }

                CalibPhase.NO_FACE -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            noFaceHeadline(state.hint),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = HrInk,
                        )
                        Text(
                            "手机放稳，脸正对镜头，光线从前面来",
                            fontSize = 15.sp,
                            color = HrMuted,
                        )
                    }
                    // P03-b 三条提示卡
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TipLine("面向光源，不要背对窗")
                        TipLine("脸占画面 20%–60% 最合适")
                        TipLine("手机立在桌上，与脸齐高")
                    }
                }

                CalibPhase.READY -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "可以开始了",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = HrInk,
                        )
                        Text(
                            "信号已建立，现在开始能记录到完整数据",
                            fontSize = 15.sp,
                            color = HrMuted,
                        )
                    }
                }
            }

            // 失败原因可解释
            val reason = state.failureMessage ?: state.cameraError
            if (reason != null) {
                Text(
                    reason,
                    fontSize = 13.sp,
                    color = HrWarn,
                    modifier = Modifier.testTag(UiTag.P03_FAILURE_REASON),
                )
            }

            // 设备诊断卡
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(HrFaint)
                    .border(BorderStroke(1.dp, HrLine), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag(UiTag.P03_DIAGNOSTICS),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("设备诊断", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HrIcon)
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = HrMuted,
                        modifier = Modifier.size(15.dp),
                    )
                }
                DiagRow("SDK 版本", state.sdkVersion, UiTag.P03_DIAG_SDK_VERSION)
                DiagRow("初始化", if (state.initOk) "成功" else (reason ?: "未开始"), UiTag.P03_DIAG_INIT)
                DiagRow(
                    "License 剩余",
                    state.licenseDaysLeft?.let { "$it 天" } ?: "—",
                    UiTag.P03_DIAG_LICENSE,
                )
                DiagRow("当前 SQI", state.sqi?.let { "%.2f".format(it) } ?: "—", UiTag.P03_DIAG_SQI)
                // ROI 字节只是自检用，正式包里不显示（P03-c 诊断卡无此行）
                if (BuildConfig.DEBUG) {
                    DiagRow("ROI 字节", "${state.roiBytes} / 243", null)
                }
            }
        }

        // ── 主按钮（就绪才亮）+ 常驻原因行 + 跳过 ──────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    // 禁用底 #E2E8F0（P03-a/b）
                    .background(if (state.readReady) HrInk else HrLine)
                    .clickable(enabled = state.readReady, role = Role.Button, onClick = onStartWithSensor)
                    .testTag(UiTag.P03_PRIMARY_BUTTON),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "开始训练",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    // 禁用字色 #94A3B8（P03-a/b），就绪为白
                    color = if (state.readReady) Color.White else HrInkTertiary,
                )
            }

            // 常驻原因行：说明"为什么还不能开始"（P03-a/b）。失败态优先显示失败原因。
            val hintReason = when (phase) {
                CalibPhase.BUILDING -> "信号建立中，约 30 秒后可开始"
                CalibPhase.NO_FACE -> noFaceReason(state.hint)
                CalibPhase.READY -> null
            }
            val reasonLine = state.failureMessage ?: state.cameraError ?: hintReason
            if (reasonLine != null) {
                Text(
                    text = reasonLine,
                    fontSize = 12.sp,
                    color = if (phase == CalibPhase.READY) HrMuted else HrWarn,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(UiTag.P03_HINT_REASON),
                )
            }
            // 摄像头可选：跳过常驻（AC-04）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .border(BorderStroke(1.dp, HrTrack), RoundedCornerShape(26.dp))
                    .clickable(role = Role.Button) {
                        vm.skipCamera()
                        onSkip()
                    }
                    .testTag(UiTag.P03_SKIP_BUTTON),
                contentAlignment = Alignment.Center,
            ) {
                Text("跳过，直接训练", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = HrInk)
            }
        }
    }
}

/** 诊断卡一行（k 左 / v 右，11sp）。 */
@Composable
private fun DiagRow(key: String, value: String, tag: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(key, fontSize = 11.sp, color = HrMuted)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = HrSlate)
    }
}

/** P03-b 的一条提示（图标 15dp #0F766E + 13sp #334155）。 */
@Composable
private fun TipLine(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = HrBrand,
            modifier = Modifier.size(15.dp),
        )
        Text(text, fontSize = 13.sp, color = HrSlate)
    }
}

/**
 * 120×120 进度环（P03-a）：
 * 底环 #E2E8F0、进度弧 #0F766E、中心秒数 26sp/600 #0F1A24。
 *
 * 用描边圆环而不是实心椭圆 —— 设计稿的"底环/进度"是同尺寸两个圆，
 * 实心会盖住中心秒数；描边环才是它能被读出来的样子。
 */
@Composable
private fun SignalRing(sec: Int, progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .testTag(UiTag.P03_SIGNAL_RING),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(120.dp)) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = HrLine,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke),
            )
            val sweep = 360f * progress.coerceIn(0f, 1f)
            if (sweep > 0f) {
                drawArc(
                    color = HrBrand,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            text = "${sec}s",
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            color = HrInk,
        )
    }
}

/** 校准页三态（P03-a 建立中 / P03-b 未对准系 / P03-c 就绪）。 */
private enum class CalibPhase { BUILDING, NO_FACE, READY }

private fun phaseOf(hint: CalibrationHint): CalibPhase = when (hint) {
    CalibrationHint.BUILDING -> CalibPhase.BUILDING
    CalibrationHint.READY -> CalibPhase.READY
    // 过小 / 过大 / 弱光都属于"没对准"，与 P03-b 同一套呈现
    CalibrationHint.NO_FACE,
    CalibrationHint.FACE_TOO_SMALL,
    CalibrationHint.FACE_TOO_LARGE,
    CalibrationHint.LOW_LIGHT,
    -> CalibPhase.NO_FACE
}

/** 人脸框三态色：建立中 #FFFFFFB3 / 未对准 #F59E0B / 就绪 #5EEAD4。 */
private fun faceBoxColor(phase: CalibPhase): Color = when (phase) {
    CalibPhase.BUILDING -> Color(0xB3FFFFFF)
    CalibPhase.NO_FACE -> Color(0xFFF59E0B)
    CalibPhase.READY -> HrAccent
}

/** 未对准系的主提示（短句，28sp 一行放得下）。 */
private fun noFaceHeadline(hint: CalibrationHint): String = when (hint) {
    CalibrationHint.FACE_TOO_SMALL -> "请靠近一点"
    CalibrationHint.FACE_TOO_LARGE -> "请往后一点"
    CalibrationHint.LOW_LIGHT -> "光线有点暗"
    else -> "请把脸放进框内"
}

/** 未对准系的原因行（常驻主按钮下方）。 */
private fun noFaceReason(hint: CalibrationHint): String = when (hint) {
    CalibrationHint.FACE_TOO_SMALL -> "脸离得有点远，请靠近镜头"
    CalibrationHint.FACE_TOO_LARGE -> "脸离得有点近，请往后一点"
    CalibrationHint.LOW_LIGHT -> "光线不足，请面向光源"
    else -> "未检测到人脸，请先对准镜头"
}

private val HrInk = Color(0xFF0F1A24)
/** 非文本级弱字（#94A3B8）：仅用于大字号/图标与禁用按钮字色 */
private val HrInkTertiary = Color(0xFF94A3B8)
private val HrMuted = Color(0xFF64748B)
private val HrLine = Color(0xFFE2E8F0)
private val HrTrack = Color(0xFFCBD5E1)
private val HrFaint = Color(0xFFF8FAFC)
private val HrIcon = Color(0xFF475569)
private val HrSlate = Color(0xFF334155)
private val HrBrand = Color(0xFF0F766E)
private val HrWarn = Color(0xFFB45309)
private val HrAccent = Color(0xFF5EEAD4)
private val HrVideoBg = Color(0xFF1B2A38)