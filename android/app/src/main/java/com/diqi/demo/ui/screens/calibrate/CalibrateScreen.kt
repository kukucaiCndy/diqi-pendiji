package com.diqi.demo.ui.screens.calibrate

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
                // 人脸框 232×288 r120
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(width = 232.dp, height = 288.dp)
                        .border(
                            BorderStroke(2.dp, HrAccent),
                            RoundedCornerShape(120.dp),
                        )
                        .testTag(UiTag.P03_FACE_BOX),
                )
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

            // 状态字 15/700 accent
            Text(
                text = state.statusText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (state.readReady) HrAccent else Color(0xFFFBBF24),
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (state.readReady) "可以开始了" else "正在建立信号",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = HrInk,
                )
                Text(
                    if (state.readReady) {
                        "信号已建立，现在开始能记录到完整数据"
                    } else {
                        "把脸放进框里，保持不动约 30 秒"
                    },
                    fontSize = 15.sp,
                    color = HrMuted,
                )
            }

            // 建立进度条
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(HrLine)
                    .testTag(UiTag.P03_SIGNAL_RING),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(state.signalProgress)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(HrBrand),
                )
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
                ) {
                    Text("设备诊断", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HrIcon)
                    Text("⌄", fontSize = 12.sp, color = HrMuted)
                }
                DiagRow("SDK 版本", state.sdkVersion, UiTag.P03_DIAG_SDK_VERSION)
                DiagRow("初始化", if (state.initOk) "成功" else (reason ?: "未开始"), UiTag.P03_DIAG_INIT)
                DiagRow(
                    "License 剩余",
                    state.licenseDaysLeft?.let { "$it 天" } ?: "—",
                    UiTag.P03_DIAG_LICENSE,
                )
                DiagRow("当前 SQI", state.sqi?.let { "%.2f".format(it) } ?: "—", UiTag.P03_DIAG_SQI)
                DiagRow("ROI 字节", "${state.roiBytes} / 243", null)
            }
        }

        // ── 主按钮（就绪才亮）+ 跳过 ────────────────────────────────────
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
                    .background(if (state.readReady) HrInk else HrTrack)
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
                Text("开始训练", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
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

private val HrInk = Color(0xFF0F1A24)
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