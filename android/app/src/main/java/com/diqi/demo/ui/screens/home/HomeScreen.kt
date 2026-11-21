package com.diqi.demo.ui.screens.home

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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.diqi.demo.ui.UiTag
import com.diqi.demo.ui.rememberContainerViewModel
import com.diqi.demo.ui.theme.HrType

/**
 * P02 首页（未完成 / 已完成两态）。
 *
 * ## 每个数值的出处
 *
 * 都能在 `docs/DESIGN-SPEC.md` 的 `P02-a` / `P02-b` 段落逐行核到。
 * 换算规则是用户拍定的 **1 设计 px = 1sp / 1dp**（见 Type.kt 头部）。
 *
 * ## 与上一版的结构差异（"和设计稿差距巨大"的主因）
 *
 * 1. 主行动按钮在**深色卡内部**（h52 / r26 / 带图标），不是页面底部的独立按钮
 * 2. **卡主文是训练量**「8 次 · 约 3 分钟」；行动文案在按钮上
 * 3. 删掉设计稿里不存在的「Day N · 主题」一行
 * 4. 本周打卡是**直径 26 的圆**（不是 12dp 小点），并带「N / 7 天」计数
 * 5. 末尾有「上次训练」行
 * 6. 页面内边距 22（水平）/ 10（垂直），区块间距 18
 *
 * ## 图标（如实标注）
 *
 * 设计稿用 lucide 的 `play` / `rotate-ccw` / `video`。本项目没有 Pencil 的
 * 栅格化工具，无法把 icon 节点导出 PNG：
 * - `play` → `Icons.Filled.PlayArrow`（形状等价）
 * - `rotate-ccw` → `Icons.Filled.Refresh`（形状等价）
 * - `video` → [VideoGlyph] 手绘（material-icons-core 没有摄像机图标，
 *   不用 Face / Info 这类语义不符的图标顶替）
 */
@Composable
fun TrainingHomeScreen(
    onStartWithoutCamera: () -> Unit,
    onStartWithCamera: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = rememberContainerViewModel { HomeViewModel(it) }
    val state by vm.state.collectAsState()

    // 从别的页面回来要刷新（完成训练后首页要变成"已完成"态）
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { vm.refresh() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HrColorCanvas)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 10.dp)
            .testTag(UiTag.P02_ROOT),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // ── 问候 16 / 主标题 30 ────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = state.greeting,
                style = HrType.bodyLarge.copy(fontSize = 16.sp),
                color = HrColorInkSecondary,
                modifier = Modifier.testTag(UiTag.P02_GREETING),
            )
            Text(
                text = if (state.trainedToday) "今天已完成" else "今天练一次吧",
                style = HrType.headline,
                color = HrColorInk,
            )
        }

        if (state.trainedToday) {
            DoneCard(state = state, onAgain = onStartWithoutCamera)
        } else {
            TodayCard(onStart = onStartWithCamera, onSkipCamera = onStartWithoutCamera)
        }

        SensorRow(state = state, onOpenCalibrate = onStartWithCamera)

        WeekCard(state = state)

        Text(
            text = state.lastTrainingText,
            style = HrType.bodyMedium,
            color = HrColorInkMuted,
            modifier = Modifier.testTag(UiTag.P02_LAST_TRAINING),
        )

        Spacer(Modifier.height(8.dp))
    }
}

// ── P02-a 今日训练卡：深色，主按钮在卡内 ────────────────────────────────

@Composable
private fun TodayCard(onStart: () -> Unit, onSkipCamera: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(HrColorInk)
            .padding(20.dp)
            .testTag(UiTag.P02_TODAY_CARD),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("今日训练", style = HrType.labelSmall, color = HrColorAccent)
        Text("8 次 · 约 3 分钟", style = HrType.titleLarge, color = HrColorSurface)
        Text(
            text = "准备 5s / 收缩 5s / 保持 5s / 放松 5s",
            style = HrType.labelSmall,
            color = HrColorPanelMuted,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(HrColorAccent)
                .clickable(role = Role.Button, onClick = onStart)
                .testTag(UiTag.P02_START_BUTTON),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = HrColorOnAccent,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("开启摄像头训练", style = HrType.button, color = HrColorOnAccent)
        }

        // 第二条路径：不用摄像头。
        // 必须和主按钮一样是**页面上可见、可点的一等选项**，
        // 而不是塞进某个二级页或弹窗 —— FR-03 的「摄像头可选」不是"藏起来"。
        // 视觉上用描边次级样式，保持"摄像头是加分项、不是门槛"的层级。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(25.dp))
                .border(BorderStroke(1.dp, HrColorHairlineDeep), RoundedCornerShape(25.dp))
                .clickable(role = Role.Button, onClick = onSkipCamera)
                .testTag(UiTag.P02_SKIP_CAMERA_BUTTON),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "不用摄像头，直接训练",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = HrColorPanelMuted,
            )
        }
    }
}

// ── P02-b 已完成卡：白卡 + 次按钮 ──────────────────────────────────────

@Composable
private fun DoneCard(state: HomeUiState, onAgain: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(HrColorSurface)
            .border(BorderStroke(1.dp, HrColorLine), RoundedCornerShape(20.dp))
            .padding(20.dp)
            .testTag(UiTag.P02_DONE_CARD),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(HrColorGood),
                contentAlignment = Alignment.Center,
            ) {
                Text("✓", style = HrType.footnote, color = HrColorSurface)
            }
            Text("今天已完成，可再练一次", style = HrType.titleSmall, color = HrColorInk)
        }

        // 数据行。心率只在覆盖率达标时出现（FR-09），设计稿是「· 平均心率 71」
        Text(
            text = state.todaySummaryText,
            style = HrType.bodyMedium,
            color = HrColorInkMuted,
            modifier = Modifier.testTag(UiTag.P02_DONE_SUMMARY),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .border(BorderStroke(1.dp, HrColorTrack), RoundedCornerShape(26.dp))
                .clickable(role = Role.Button, onClick = onAgain)
                .testTag(UiTag.P02_AGAIN_BUTTON),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                tint = HrColorInk,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("再练一次", style = HrType.buttonSecondary, color = HrColorInk)
        }
    }
}

// ── 感知状态行 ──────────────────────────────────────────────────────────

@Composable
private fun SensorRow(state: HomeUiState, onOpenCalibrate: () -> Unit) {
    val ready = state.sensor == SensorStatusRow.READY
    val bg = if (ready) HrColorGoodContainer else HrColorWarnContainer
    val fg = if (ready) HrColorGood else HrColorWarn

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(role = Role.Button, onClick = onOpenCalibrate)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(UiTag.P02_SENSOR_STATUS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (ready) {
            Box(
                Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(fg),
            )
        } else {
            VideoGlyph(color = fg, modifier = Modifier.size(16.dp))
        }
        Text(
            text = state.sensor.label,
            style = HrType.capsule,
            color = fg,
            modifier = Modifier.weight(1f),
        )
        if (!ready && state.sensor.actionLabel.isNotBlank()) {
            Text(
                text = state.sensor.actionLabel,
                style = HrType.capsule.copy(fontWeight = FontWeight.Bold),
                color = fg,
            )
        }
    }
}

// ── 本周打卡：圆点直径 26，带「N / 7 天」───────────────────────────────

@Composable
private fun WeekCard(state: HomeUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(HrColorSurface)
            .border(BorderStroke(1.dp, HrColorLine), RoundedCornerShape(16.dp))
            .padding(18.dp)
            .testTag(UiTag.P02_WEEK_CHECKIN),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("本周打卡", style = HrType.label, color = HrColorInk)
            Text(
                "${state.trainedWeekdays.size} / 7 天",
                style = HrType.labelSmall,
                color = HrColorBrand,
                modifier = Modifier.testTag(UiTag.P02_WEEK_COUNT),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            WEEK_LABELS.forEachIndexed { i, label ->
                // state.trainedWeekdays 是 0-based（0 = 周一，见 HomeUiState 注释），
                // 首版这里误用 i + 1，导致"今天练过"也显示 0 / 7 天。
                val done = state.trainedWeekdays.contains(i)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (done) HrColorGood else HrColorSurface)
                            .border(1.dp, if (done) HrColorGood else HrColorTrack, CircleShape),
                    )
                    Text(label, style = HrType.micro, color = HrColorInkTertiary)
                }
            }
        }
    }
}

/**
 * 摄像机图标。
 *
 * material-icons-core 里没有摄像机/视频图标，而设计稿用的是 lucide `video`。
 * 这里按等价形状手绘，**不用 Face / Info 这类语义不符的通用图标顶替**
 * —— 那正是 skill 反复强调的错误做法。
 */
@Composable
private fun VideoGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val bodyW = w * 0.70f
        val bodyH = h * 0.62f
        val top = (h - bodyH) / 2f
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, top),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(h * 0.16f),
            style = Stroke(width = h * 0.13f),
        )
        val lens = Path().apply {
            moveTo(bodyW + w * 0.05f, h * 0.36f)
            lineTo(w, h * 0.22f)
            lineTo(w, h * 0.78f)
            lineTo(bodyW + w * 0.05f, h * 0.64f)
            close()
        }
        drawPath(lens, color = color)
    }
}

private val WEEK_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")

// ── 设计稿页面内联色（DESIGN-SPEC §P02-a / §P02-b）─────────────────────
private val HrColorCanvas = Color(0xFFF4F6F8)
private val HrColorInk = Color(0xFF0F1A24)
private val HrColorInkSecondary = Color(0xFF5C6B7A)
private val HrColorInkTertiary = Color(0xFF94A3B8)
private val HrColorInkMuted = Color(0xFF64748B)
private val HrColorSurface = Color(0xFFFFFFFF)
private val HrColorLine = Color(0xFFE2E8F0)
private val HrColorTrack = Color(0xFFCBD5E1)
private val HrColorBrand = Color(0xFF0F766E)
private val HrColorGood = Color(0xFF047857)
private val HrColorGoodContainer = Color(0xFFE7F6EC)
private val HrColorWarn = Color(0xFFB45309)
private val HrColorWarnContainer = Color(0xFFFEF3C7)
private val HrColorAccent = Color(0xFF5EEAD4)
private val HrColorOnAccent = Color(0xFF062530)
private val HrColorPanelMuted = Color(0xFF8FA3B8)
private val HrColorHairlineDeep = Color(0xFF34495E)