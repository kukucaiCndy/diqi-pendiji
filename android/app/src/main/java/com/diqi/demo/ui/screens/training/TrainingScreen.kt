package com.diqi.demo.ui.screens.training

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.diqi.demo.domain.training.TrainingPhase
import com.diqi.demo.domain.training.TrainingStatus
import com.diqi.demo.ui.UiTag
import com.diqi.demo.ui.components.ChipTone
import com.diqi.demo.ui.components.HrBanner
import com.diqi.demo.ui.components.HrCard
import com.diqi.demo.ui.components.HrPageWithActionLazy
import com.diqi.demo.ui.components.HrPrimaryButton
import com.diqi.demo.ui.components.HrSecondaryButton
import com.diqi.demo.ui.components.HrStatusChip
import com.diqi.demo.ui.components.PhaseTimeline
import com.diqi.demo.ui.components.RhythmRing
import com.diqi.demo.ui.components.VSpace
import com.diqi.demo.ui.rememberContainerViewModel
import com.diqi.demo.ui.theme.HrColor
import com.diqi.demo.ui.theme.HrRadius
import com.diqi.demo.ui.theme.HrSpacing
import com.diqi.demo.ui.theme.HrType

/**
 * P04 训练中（设计稿 P04-a 运行 / P04-b 暂停 / P04-c 建信号）。
 *
 * ## 布局自下而上说明（为什么是这个顺序）
 *
 * 1. **视频区**在最上，未启用感知时是深色占位 + 一行「未启用感知」——
 *    绝不显示任何假画面或假数值（FR-06、AC-04）。
 * 2. **阶段时间轴**紧接着：回答"我进行到第几步"。
 * 3. **节律圆环 + 倒计时**居中：回答"这一秒该做什么"。圆环常驻「节律提示」（D10）。
 * 4. **指标区**：未启用感知时整块不显示，只留一行提示（FR-06 原文）。
 * 5. **免责文案**常驻（§5.6 强制）。
 * 6. **暂停 / 结束固定在底部**（不影响滚动内容，2.0× 字号也够得着）。
 */
@Composable
fun TrainingScreen(
    onFinished: (String?) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = rememberContainerViewModel { TrainingViewModel(it) }
    val state by vm.state.collectAsState()

    // 训练期间挂前台服务：锁屏/切后台时计时与语音不中断（AC-05）。
    // 离开页面即停 —— 训练已结束的情况下没有保活需求。
    val context = androidx.compose.ui.platform.LocalContext.current

    // Android 13+ 前台服务的通知需要 POST_NOTIFICATIONS 才会显示。
    // 不申请的话服务仍在跑（计时正确），但用户在通知栏看不到训练状态，
    // 也无法从通知回到训练页。首次进入训练时申请一次。
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        ) { /* 拒绝也不阻断训练：只是通知不可见 */ }
        androidx.compose.runtime.LaunchedEffect(Unit) {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        com.diqi.demo.service.TrainingForegroundService.start(context)
        onDispose {
            com.diqi.demo.service.TrainingForegroundService.stop(context)
            vm.onClearedComposition()
        }
    }

    // 训练结束 → 去完成页（S7 之前是占位）
    LaunchedEffect(state.finished) {
        if (state.finished) onFinished(state.savedSessionId)
    }

    // 中断恢复对话框（AC-13）
    state.pendingRecovery?.let { snap ->
        AlertDialog(
            onDismissRequest = { },
            title = { Text("上次的训练还没做完", style = HrType.titleSmall, color = HrColor.Ink) },
            text = {
                // 快照里不存"已完成次数"，因为它完全由已用时长决定（纯函数）。
                // 这里现算，避免两处数字不一致。
                val done = com.diqi.demo.domain.training.TrainingTimeline
                    .completedRepsAt(snap.elapsedActiveMs, state.params)
                Text(
                    "你已经完成 $done 次，用时 ${snap.elapsedActiveMs / 1000} 秒。要继续吗？",
                    style = HrType.bodySmall,
                    color = HrColor.InkSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { vm.continueFromSnapshot() },
                    modifier = Modifier.testTag(UiTag.P04_RECOVERY_CONTINUE),
                ) { Text("继续", style = HrType.button, color = HrColor.Brand) }
            },
            dismissButton = {
                TextButton(
                    onClick = { vm.discardSnapshot() },
                    modifier = Modifier.testTag(UiTag.P04_RECOVERY_DISCARD),
                ) { Text("丢弃", style = HrType.button, color = HrColor.InkSecondary) }
            },
            containerColor = HrColor.Surface,
            modifier = Modifier.testTag(UiTag.P04_RECOVERY_DIALOG),
        )
    }

    // 结束二次确认（FR-06 底部「结束（二次确认）」）
    if (state.showStopConfirm) {
        AlertDialog(
            onDismissRequest = { vm.cancelStop() },
            title = { Text("要结束这次练习吗？", style = HrType.titleSmall, color = HrColor.Ink) },
            text = {
                Text(
                    "已完成的 ${vm.completedRepsNow()} 次会保存下来，不会补算没做完的次数。",
                    style = HrType.bodySmall,
                    color = HrColor.InkSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { vm.confirmStop() },
                    modifier = Modifier.testTag(UiTag.P04_STOP_CONFIRM),
                ) { Text("结束", style = HrType.button, color = HrColor.Danger) }
            },
            dismissButton = {
                TextButton(
                    onClick = { vm.cancelStop() },
                    modifier = Modifier.testTag(UiTag.P04_STOP_CANCEL),
                ) { Text("继续练", style = HrType.button, color = HrColor.Brand) }
            },
            containerColor = HrColor.Surface,
            modifier = Modifier.testTag(UiTag.P04_STOP_CONFIRM_DIALOG),
        )
    }

    // 返回键必须走「结束二次确认」（FR-06）。
    // 首版没有拦截：按返回会直接退出页面，但会话是容器级的仍在跑，
    // 而帧收集器已经没了 —— 训练跑完也不会落库（记录静默丢失）。
    androidx.activity.compose.BackHandler(enabled = !state.showStopConfirm) {
        vm.requestStop()
    }

    if (state.saveFailed) {
        androidx.compose.material3.AlertDialog(
            // 允许点外部关闭 = 等同于"先不保存"，避免出现关不掉的死弹窗
            onDismissRequest = { vm.abandonOnSaveFailure() },
            title = { Text("这次记录没保存成功", style = HrType.titleSmall, color = HrColor.Ink) },
            text = {
                Text(
                    "练习本身已经完成，只是记录写入失败（可能是存储空间不足）。下次再练一次即可。",
                    style = HrType.bodySmall,
                    color = HrColor.InkSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.retrySave() }) {
                    Text("再试一次", style = HrType.button, color = HrColor.Brand)
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.abandonOnSaveFailure() }) {
                    Text("先不保存", style = HrType.button, color = HrColor.InkSecondary)
                }
            },
            containerColor = HrColor.Surface,
        )
    }

    HrPageWithActionLazy(
        modifier = modifier,
        tag = UiTag.P04_ROOT,
        action = {
            Row(horizontalArrangement = Arrangement.spacedBy(HrSpacing.xs)) {
                Box(Modifier.weight(1f)) {
                    // 暂停/继续共用**同一个 testTag**：首版按状态切换 tag，
                    // 自动化脚本"暂停→恢复"的第二步就找不到节点。
                    // 状态差异通过 text 与 contentDescription 表达。
                    HrSecondaryButton(
                        text = if (state.paused) "继续" else "暂停",
                        tag = UiTag.P04_PAUSE_BUTTON,
                        onClick = { vm.togglePause() },
                    )
                }
                Box(Modifier.weight(1f)) {
                    HrPrimaryButton(
                        text = "结束",
                        tag = UiTag.P04_STOP_BUTTON,
                        onClick = { vm.requestStop() },
                    )
                }
            }
        },
    ) {
        // 每个区块一个 item：推帧只会重测倒计时那一项，
        // 而不是整块滚动内容（Column 会全量重测）。
        item {
            // ── 视频区（未启用感知时深色占位）────────────────────────────
            VideoAreaPlaceholder(demoMode = state.demoMode)
        }

        item { VSpace(HrSpacing.xs) }

        // FR-01 分流：训练页常驻提示条
        if (state.clinicalNotice) {
            item {
                HrBanner(
                    text = "按之前的回答，练到不舒服就停下来，并先咨询医生。",
                    tone = ChipTone.WARN,
                )
                VSpace(HrSpacing.xs)
            }
        }

        // 每秒变化的一切收在同一个 item 的 TickingSection 里
        item { TickingSection(vm = vm, paused = state.paused) }

        // ── 指标区 ───────────────────────────────────────────────────────
        item { MetricsSection(vm = vm, demoMode = state.demoMode) }

        item {
            VSpace(HrSpacing.sm)
            // ── 常驻免责文案（§5.6 强制）────────────────────────────────
            Text(
                text = "上述内容反映身体状态，不代表盆底肌收缩质量。",
                style = HrType.disclaimer,
                color = HrColor.InkSecondary,
                modifier = Modifier.testTag(UiTag.P04_DISCLAIMER),
            )
            VSpace(HrSpacing.lg)
        }
    }
}

/**
 * 视频区。感知层（S6）接入前如实显示「未启用感知」。
 *
 * 不画假的人脸框、不放伪造的波形 —— 需求 FR-08 的「宁可不显示」在这里同样适用。
 */
@Composable
private fun VideoAreaPlaceholder(demoMode: Boolean) {
    // 视频区文案必须**如实反映当前数据源**：
    // 演示模式下如果还写"未启用感知"，就与指标区自相矛盾了。
    // 视频区只区分"演示 / 未启用感知"两种静态形态。
    // 首版还读 metric 决定是否显示"感知中"，那会让视频区每秒跟着重组 ——
    // 而它本来就不含任何每秒变化的信息。
    val title = if (demoMode) "演示模式" else "未启用感知"
    val subtitle = if (demoMode) "没有画面，仅模拟数值" else "不开摄像头也能完成练习"
    Box(
        Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(HrRadius.cardShape)
            .background(HrColor.PanelSecond),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                title,
                style = HrType.bodyMediumBold,
                color = HrColor.TimelineName,
                modifier = Modifier.testTag(UiTag.P04_SENSOR_CHIP),
            )
            Text(
                subtitle,
                style = HrType.labelSmall,
                color = HrColor.TimelineLabel,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * 指标区。
 *
 * FR-06 原文：「未开启感知 → 整块不显示，仅留一行『未启用感知』」。
 * S6 会在这里加四态渲染（VALID/DEGRADED/INVALID/NO_DATA）。
 */
@Composable
internal fun MetricArea(metric: MetricUiState, demoMode: Boolean = false) {
    when (metric) {
        is MetricUiState.NotEnabled -> {
            // FR-06：「未开启感知 → 整块不显示，仅留一行」
            HrCard(color = HrColor.SubtleContainer, modifier = Modifier.testTag(UiTag.P04_METRIC_AREA)) {
                Text(
                    "未启用感知",
                    style = HrType.bodySmall,
                    color = HrColor.InkSecondary,
                    modifier = Modifier.testTag(UiTag.P04_METRIC_PLACEHOLDER),
                )
            }
        }

        is MetricUiState.Establishing -> {
            // AC-15：前 30 秒只显示「信号建立中」，**不显示任何数字**
            HrCard(color = HrColor.SubtleContainer, modifier = Modifier.testTag(UiTag.P04_METRIC_AREA)) {
                Column(verticalArrangement = Arrangement.spacedBy(HrSpacing.xxs)) {
                    HrStatusChip(
                        text = "信号建立中",
                        tone = ChipTone.WARN,
                        tag = UiTag.P04_METRIC_QUALITY,
                    )
                    Text(
                        text = "正在建立信号 ${metric.elapsedSec}s / 约 30s，请把手机放稳。",
                        style = HrType.bodySmall,
                        color = HrColor.InkSecondary,
                        modifier = Modifier.testTag(UiTag.P04_METRIC_PLACEHOLDER),
                    )
                }
            }
        }

        is MetricUiState.NoData -> {
            HrCard(color = HrColor.SubtleContainer, modifier = Modifier.testTag(UiTag.P04_METRIC_AREA)) {
                Column(verticalArrangement = Arrangement.spacedBy(HrSpacing.xxs)) {
                    HrStatusChip(
                        text = "信号暂失",
                        tone = ChipTone.NEUTRAL,
                        tag = UiTag.P04_METRIC_QUALITY,
                    )
                    // QualityGate 给的 banner（例如"看不到脸了，请把手机放稳"）
                    // 必须显示出来 —— 首版把它丢了，用户只知道"信号暂失"却不知道该怎么办。
                    if (metric.banner != null) {
                        Text(
                            text = metric.banner,
                            style = HrType.bodySmall,
                            color = HrColor.Warn,
                            modifier = Modifier.testTag(UiTag.P04_METRIC_PLACEHOLDER),
                        )
                    }
                    Text(
                        text = "节律提示照常进行，不影响练习。",
                        style = HrType.bodySmall,
                        color = HrColor.InkSecondary,
                    )
                }
            }
        }

        is MetricUiState.Invalid -> {
            HrCard(modifier = Modifier.testTag(UiTag.P04_METRIC_AREA)) {
                Column(verticalArrangement = Arrangement.spacedBy(HrSpacing.xxs)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("心率", style = HrType.label, color = HrColor.WeakText)
                        Text("--", style = HrType.monoMetric, color = HrColor.WeakText)
                    }
                    HrStatusChip(
                        text = "信号弱",
                        tone = ChipTone.DANGER,
                        tag = UiTag.P04_METRIC_QUALITY,
                    )
                }
            }
        }

        is MetricUiState.Degraded -> MetricNumbers(
            hr = metric.heartRateBpm,
            br = metric.breathingRatePerMin,
            state = metric,
            dimmed = false,
            banner = metric.banner,
            demoMode = demoMode,
        )

        is MetricUiState.Valid -> MetricNumbers(
            hr = metric.heartRateBpm,
            br = metric.breathingRatePerMin,
            state = metric,
            dimmed = metric.dimmed,
            banner = null,
            demoMode = demoMode,
        )
    }
}

@Composable
internal fun MetricNumbers(
    hr: Float?,
    br: Float?,
    state: MetricUiState,
    dimmed: Boolean,
    banner: String?,
    demoMode: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrRadius.cardShape)
            .background(HrColor.Surface)
            .padding(HrSpacing.md)
            .testTag(UiTag.P04_METRIC_AREA),
        verticalArrangement = Arrangement.spacedBy(HrSpacing.xs),
    ) {
        // 演示数据必须有全屏水印（AC-19）。这里再给一条就地提示，
        // 因为"全屏水印"在大屏上不一定会覆盖到指标卡本身。
        if (demoMode) {
            HrStatusChip(
                text = "模拟数据 · 仅用于演示",
                tone = ChipTone.WARN,
                tag = UiTag.P04_DEMO_WATERMARK,
            )
        }
        if (banner != null) {
            HrBanner(text = banner, tone = ChipTone.WARN)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HrSpacing.md),
        ) {
            MetricCell(
                label = "心率",
                value = hr?.let { "%.0f".format(it) },
                unit = "bpm",
                color = HrColor.HeartRate,
                tag = UiTag.P04_METRIC_HR,
                dimmed = dimmed,
            )
            MetricCell(
                label = "呼吸率",
                value = br?.let { "%.0f".format(it) },
                unit = "次/分",
                color = HrColor.BreathingRate,
                tag = UiTag.P04_METRIC_BR,
                dimmed = dimmed,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HrStatusChip(
                text = when (state) {
                    is MetricUiState.Degraded -> "信号一般"
                    else -> "信号良好"
                },
                tone = if (state is MetricUiState.Degraded) ChipTone.WARN else ChipTone.GOOD,
                tag = UiTag.P04_METRIC_QUALITY,
            )
            QualityDots(count = state.qualityDots)
        }
    }
}

/**
 * 一个指标单元格。
 *
 * `value == null` 时**不渲染数字**，只留「--」——
 * 需求禁止"无数据时显示数字"，这里的类型已经保证了这一点（`Float?`）。
 */
@Composable
private fun androidx.compose.foundation.layout.RowScope.MetricCell(
    label: String,
    value: String?,
    unit: String,
    color: androidx.compose.ui.graphics.Color,
    tag: String,
    dimmed: Boolean,
) {
    val alpha = if (dimmed) 0.5f else 1f
    Column(
        modifier = Modifier
            .weight(1f)
            .testTag(tag),
    ) {
        Text(label, style = HrType.micro, color = HrColor.WeakText)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value ?: "--",
                style = HrType.monoMetric,
                color = if (value == null) HrColor.WeakText else color.copy(alpha = alpha),
            )
            Text(
                text = unit,
                style = HrType.micro,
                color = HrColor.WeakText,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
        }
    }
}

/** 质量三点指示（设计稿「三点质量指示」）。*/
@Composable
private fun QualityDots(count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { index ->
            Box(
                Modifier
                    .size(8.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(if (index < count) HrColor.Good else HrColor.Track),
            )
        }
    }
}

/**
 * 每秒变化的子树（阶段时间轴 + 当前阶段 + 进度轨 + 节律圆环 + 次数/语音状态）。
 *
 * ## 为什么单独抽出来
 *
 * 实测：训练页运行中 40 帧/20s、22 帧慢（p90 61ms）；暂停后不推帧时
 * **0 帧 0 慢** —— 掉帧 100% 来自"推帧导致整页重组"。
 * 把订阅 `vm.frames` 的动作收敛到这一个子树后，视频区、免责文案、
 * 底部按钮都不会再随帧重组。
 */
@Composable
private fun TickingSection(
    vm: com.diqi.demo.ui.screens.training.TrainingViewModel,
    paused: Boolean,
) {
    val frame by vm.frames.collectAsState()

    // 只传**会变的原语**：5 个阶段标签是静态文案，
    // 让它们因 `frame` 变化而每秒重排是纯浪费（实测 p90 从 61ms 降到 48ms 后仍有 48ms）。
    val order = when (frame.phase) {
        com.diqi.demo.domain.training.TrainingPhase.IDLE,
        com.diqi.demo.domain.training.TrainingPhase.PREPARATION,
        -> 0
        com.diqi.demo.domain.training.TrainingPhase.CONTRACT -> 1
        com.diqi.demo.domain.training.TrainingPhase.HOLD -> 2
        com.diqi.demo.domain.training.TrainingPhase.RELAX -> 3
        com.diqi.demo.domain.training.TrainingPhase.COMPLETE -> 4
    }
    PhaseTimeline(
        currentOrder = order,
        repIndex = frame.repIndex,
        totalReps = frame.totalReps,
        completed = frame.status == com.diqi.demo.domain.training.TrainingStatus.COMPLETE,
        params = com.diqi.demo.domain.training.TrainingParams.DEFAULT,
    )
    VSpace(HrSpacing.sm)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text("当前阶段", style = HrType.micro, color = HrColor.WeakText)
            Text(
                text = frame.phase.displayName,
                style = HrType.headline,
                color = HrColor.Ink,
                modifier = Modifier.testTag(UiTag.P04_PHASE_NAME),
            )
            Text(
                text = frame.phase.hint,
                style = HrType.bodySmall,
                color = HrColor.InkSecondary,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = frame.countdownText,
                style = HrType.monoDisplay,
                color = HrColor.Ink,
            )
            Text(
                "共 ${frame.phaseTotalMs / 1000} 秒",
                style = HrType.labelSmall,
                color = HrColor.WeakText,
            )
        }
    }

    VSpace(HrSpacing.xs)

    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(HrRadius.capsuleShape)
            .background(HrColor.Line),
    ) {
        Box(
            Modifier
                .fillMaxWidth(frame.phaseProgress)
                .height(6.dp)
                .clip(HrRadius.capsuleShape)
                .background(HrColor.Brand),
        )
    }

    VSpace(HrSpacing.md)

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        RhythmRing(
            progress = frame.phaseProgress,
            centerText = frame.countdownText,
            totalText = if (paused) "已暂停" else "${frame.repIndex} / ${frame.totalReps} 次",
            activeTone = !paused,
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "第 ${frame.repIndex} / ${frame.totalReps} 次",
            style = HrType.label,
            color = HrColor.Ink,
        )
    }
}

/** 指标区独立订阅 `vm.metric`，不连累父级。 */
@Composable
private fun MetricsSection(
    vm: com.diqi.demo.ui.screens.training.TrainingViewModel,
    demoMode: Boolean,
) {
    val metric by vm.metric.collectAsState()
    MetricArea(metric = metric, demoMode = demoMode)
}
