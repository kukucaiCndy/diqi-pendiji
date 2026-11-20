package com.diqi.demo.ui.screens.calibrate

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.diqi.demo.sensor.camera.CameraController
import com.diqi.demo.sensor.camera.fillParent
import com.diqi.demo.ui.UiTag
import com.diqi.demo.ui.components.ChipTone
import com.diqi.demo.ui.components.HrBanner
import com.diqi.demo.ui.components.HrCard
import com.diqi.demo.ui.components.HrPage
import com.diqi.demo.ui.components.HrPrimaryButton
import com.diqi.demo.ui.components.HrSecondaryButton
import com.diqi.demo.ui.components.HrStatusChip
import com.diqi.demo.ui.components.VSpace
import com.diqi.demo.ui.rememberContainerViewModel
import com.diqi.demo.ui.theme.HrColor
import com.diqi.demo.ui.theme.HrRadius
import com.diqi.demo.ui.theme.HrSize
import com.diqi.demo.ui.theme.HrSpacing
import com.diqi.demo.ui.theme.HrType

/**
 * P03 摄像头校准（可选）。
 *
 * ## 这个页面承担的三件事
 *
 * 1. **把"摄像头是可选"这件事讲清楚**：预览上方文案 + 常驻「跳过，直接训练」
 * 2. **四类实时反馈**：未检测到人脸 / 脸过小过大 / 光照不足 / 信号建立中（FR-03）
 * 3. **诊断可解释**：SDK 版本、初始化结果、License 剩余、当前 SQI，折叠在底部
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
    val camera = remember {
        CameraController(context, lifecycleOwner, vm.analyzer)
    }

    DisposableEffect(granted) {
        if (granted) camera.start(previewView)
        onDispose { camera.release() }
    }

    HrPage(modifier = modifier, tag = UiTag.P03_ROOT) {
        // ── 视频区 + 人脸框 ───────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(HrRadius.cardShape)
                .background(HrColor.PanelSecond),
        ) {
            if (granted) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(UiTag.P03_PREVIEW),
                )
                // 人脸框：居中固定尺寸，作为"把脸放进框里"的参照（设计稿 200×248）
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(width = HrSize.faceBoxWidth, height = HrSize.faceBoxHeight)
                        .border(
                            width = 2.dp,
                            color = when (state.hint) {
                                CalibrationHint.READY -> HrColor.Accent
                                CalibrationHint.NO_FACE -> HrColor.Surface
                                else -> HrColor.Amber500
                            },
                            shape = RoundedCornerShape(24.dp),
                        )
                        .testTag(UiTag.P03_FACE_BOX),
                )
                // 相机角标
                Row(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(HrSpacing.xs)
                        .clip(HrRadius.capsuleShape)
                        .background(HrColor.ScrimChip)
                        .padding(horizontal = HrSpacing.xs, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(
                                if (state.hint == CalibrationHint.READY) {
                                    HrColor.Accent
                                } else {
                                    HrColor.Amber500
                                },
                            ),
                    )
                    Text(
                        if (state.hint == CalibrationHint.READY) "感知中" else "建立中",
                        style = HrType.micro,
                        color = HrColor.Surface,
                    )
                }
                // 镜像标记
                Text(
                    "镜像显示",
                    style = HrType.micro,
                    color = HrColor.Surface,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(HrSpacing.xs)
                        .clip(HrRadius.capsuleShape)
                        .background(HrColor.ScrimChip)
                        .padding(horizontal = HrSpacing.xs, vertical = 4.dp),
                )
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(HrSpacing.md),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "没有相机权限",
                        style = HrType.bodyMediumBold,
                        color = HrColor.Surface,
                    )
                    VSpace(HrSpacing.xxs)
                    Text(
                        "不开摄像头也能完整训练，直接点下面的按钮即可。",
                        style = HrType.labelSmall,
                        color = HrColor.TimelineLabel,
                    )
                }
            }

            // 状态字（覆盖在底部）
            Text(
                text = state.statusText,
                style = HrType.bodyMediumBold,
                color = if (state.hint == CalibrationHint.READY) HrColor.Accent else HrColor.Amber300,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(HrColor.ScrimFaceMask)
                    .padding(HrSpacing.xs)
                    .testTag(UiTag.P03_STATUS_TEXT),
            )
        }

        VSpace(HrSpacing.xs)

        Text(
            "摄像头只用来看你的面部变化，画面不会保存，也不会传出去。",
            style = HrType.bodySmall,
            color = HrColor.InkSecondary,
        )

        // 建立进度环（简化：用横向进度条 + 秒数，与时间轴风格一致）
        VSpace(HrSpacing.sm)
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(HrRadius.capsuleShape)
                .background(HrColor.Line)
                .testTag(UiTag.P03_SIGNAL_RING),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(state.signalProgress)
                    .height(8.dp)
                    .clip(HrRadius.capsuleShape)
                    .background(HrColor.Brand),
            )
        }

        // 失败原因必须可解释（FR-03）
        val reason = state.failureMessage ?: state.cameraError
        if (reason != null) {
            VSpace(HrSpacing.sm)
            HrBanner(
                text = reason,
                tone = ChipTone.WARN,
                tag = UiTag.P03_FAILURE_REASON,
            )
        }

        VSpace(HrSpacing.sm)

        // 三条操作提示（设计稿 P03-b）
        HrCard(color = HrColor.SubtleContainer) {
            Column(verticalArrangement = Arrangement.spacedBy(HrSpacing.xxs)) {
                HintRow("面向光源，不要背对窗户")
                HintRow("脸占画面 20%–60% 最合适")
                HintRow("手机立在桌上，与脸齐高")
            }
        }

        VSpace(HrSpacing.sm)

        // 诊断折叠区（P03 设计稿）
        HrCard(color = HrColor.FaintContainer, modifier = Modifier.testTag(UiTag.P03_DIAGNOSTICS)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("设备诊断", style = HrType.label, color = HrColor.Slate600)
                DiagRow("SDK 版本", state.sdkVersion, UiTag.P03_DIAG_SDK_VERSION)
                DiagRow(
                    "初始化",
                    if (state.initOk) "已就绪" else (reason ?: "未开始"),
                    UiTag.P03_DIAG_INIT,
                )
                DiagRow(
                    "License 剩余",
                    state.licenseDaysLeft?.let { "$it 天" } ?: "—",
                    UiTag.P03_DIAG_LICENSE,
                )
                DiagRow(
                    "当前 SQI",
                    state.sqi?.let { "%.2f".format(it) } ?: "—",
                    UiTag.P03_DIAG_SQI,
                )
                DiagRow("ROI 字节", "${state.roiBytes} / 243", null)
            }
        }

        VSpace(HrSpacing.md)

        HrPrimaryButton(
            text = if (state.canStartWithSensor) "开始训练" else "信号还没建立好",
            tag = UiTag.P03_PRIMARY_BUTTON,
            enabled = state.canStartWithSensor,
            onClick = onStartWithSensor,
        )

        VSpace(HrSpacing.xs)

        HrSecondaryButton(
            text = "跳过，直接训练",
            tag = UiTag.P03_SKIP_BUTTON,
            onClick = {
                vm.skipCamera()
                onSkip()
            },
        )

        VSpace(HrSpacing.xxl)
    }
}

@Composable
private fun HintRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HrSpacing.xs),
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(HrColor.Brand),
        )
        Text(text, style = HrType.labelSmall, color = HrColor.Slate700)
    }
}

@Composable
private fun DiagRow(key: String, value: String, tag: String?) {
    Row(
        Modifier
            .fillMaxWidth()
            .let { if (tag != null) it.then(Modifier.testTag(tag)) else it },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(key, style = HrType.micro, color = HrColor.WeakText)
        Text(value, style = HrType.micro, color = HrColor.Slate700)
    }
}