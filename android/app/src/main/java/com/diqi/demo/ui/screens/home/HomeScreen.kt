package com.diqi.demo.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.diqi.demo.ui.UiTag
import com.diqi.demo.ui.components.ChipTone
import com.diqi.demo.ui.components.HrBanner
import com.diqi.demo.ui.components.HrCard
import com.diqi.demo.ui.components.HrPage
import com.diqi.demo.ui.components.HrPrimaryButton
import com.diqi.demo.ui.components.HrSecondaryButton
import com.diqi.demo.ui.components.HrStatusChip
import com.diqi.demo.ui.components.HrWeekDots
import com.diqi.demo.ui.components.VSpace
import com.diqi.demo.ui.rememberContainerViewModel
import com.diqi.demo.ui.theme.HrColor
import com.diqi.demo.ui.theme.HrRadius
import com.diqi.demo.ui.theme.HrSpacing
import com.diqi.demo.ui.theme.HrType

/**
 * P02 首页（未完成 / 已完成两态）。
 *
 * 布局取自 `design/desing.pen` 的 P02-a / P02-b：
 * 问候 → 主标题（Day 主题）→ 今日训练卡（深色）或已完成卡（白色）
 * → 感知状态行 → 本周打卡 → 上次训练。
 *
 * 两处与本页有关的硬要求：
 * - **感知状态行可点但不阻断**：不点也能直接开始训练（FR-02 / AC-04）
 * - **就医提示常驻**：筛查触发分流时，首页与训练页都要有提示条（FR-01）
 */
@Composable
fun HomeScreen(
    onStartTraining: () -> Unit,
    onOpenCalibrate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = rememberContainerViewModel { HomeViewModel(it) }
    val state by vm.state.collectAsState()

    // 除了进入页面，从后台回来（ON_RESUME）也要刷新：
    // 跨天、或前台服务在后台练完一场后回来，数据已经变了，
    // 只靠 LaunchedEffect(Unit) 刷不到这些场景。
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) vm.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 每次**进入**首页都重新读一遍数据。
    // ViewModel 会随返回栈保留（导航时 saveState），只在 init 里加载会导致
    // "刚练完/刚造完数据回到首页还是旧状态" —— 实测踩到过：
    // 造 3 天记录后首页仍显示 Day 1、也没有已完成卡。
    LaunchedEffect(Unit) { vm.refresh() }

    HrPage(modifier = modifier, tag = UiTag.P02_ROOT) {
        Text(
            text = state.greeting,
            style = HrType.bodyMedium,
            color = HrColor.InkSecondary,
            modifier = Modifier.testTag(UiTag.P02_GREETING),
        )
        VSpace(HrSpacing.xxs)
        Text(
            text = if (state.cycleComplete) "七天体验已走完" else state.dayTheme,
            style = HrType.headline,
            color = HrColor.Ink,
            modifier = Modifier.testTag(UiTag.P02_TODAY_THEME),
        )
        VSpace(HrSpacing.xxs)
        Text(state.dayHint, style = HrType.bodySmall, color = HrColor.InkSecondary)

        VSpace(HrSpacing.md)

        // 常驻就医提示（FR-01）
        if (state.clinicalNoticeRequired) {
            HrBanner(
                text = "你之前回答过需要留意的情况，练到不舒服就停下来，并先咨询医生。",
                tone = ChipTone.WARN,
                tag = UiTag.P02_CLINICAL_BANNER,
            )
            VSpace(HrSpacing.sm)
        }

        if (state.trainedToday) {
            DoneCard(
                sessionCount = state.weekSessionCount,
                totalSec = state.weekTotalSec,
                onAgain = onStartTraining,
            )
        } else {
            TodayCard(
                dayIndex = state.dayIndex,
                dayHint = state.dayHint,
                onStart = onStartTraining,
            )
        }

        VSpace(HrSpacing.sm)

        SensorRow(sensor = state.sensor, onClick = onOpenCalibrate)

        VSpace(HrSpacing.sm)

        HrCard(modifier = Modifier.testTag(UiTag.P02_WEEK_CHECKIN)) {
            Column(verticalArrangement = Arrangement.spacedBy(HrSpacing.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "本周打卡",
                        style = HrType.label,
                        color = HrColor.Ink,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        text = "${state.weekSessionCount} 次 · ${formatMinutes(state.weekTotalSec)}",
                        style = HrType.labelSmall,
                        color = HrColor.Brand,
                    )
                }
                HrWeekDots(
                    trainedDays = state.trainedWeekdays,
                    todayWeekday = state.todayWeekday,
                )
            }
        }

        VSpace(HrSpacing.sm)

        Text(
            text = state.lastTrainingText,
            style = HrType.bodySmall,
            color = HrColor.InkSecondary,
            modifier = Modifier.testTag(UiTag.P02_LAST_TRAINING),
        )

        VSpace(HrSpacing.xxl)
    }
}

/**
 * 今日训练卡（深色，设计稿 P02-a）。
 *
 * 深色卡是首页唯一的视觉焦点，用 `accent` 强调按钮 —— 目的是让 55+ 用户
 * 一眼找到"该点哪儿"，而不是先读文字。
 */
@Composable
private fun TodayCard(dayIndex: Int, dayHint: String, onStart: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(HrRadius.cardShape)
            .background(HrColor.Panel)
            .padding(HrSpacing.lg)
            .testTag(UiTag.P02_TODAY_CARD),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(HrSpacing.sm)) {
            Text("今日训练 · Day $dayIndex", style = HrType.micro, color = HrColor.Accent)
            Text("开始今天的训练", style = HrType.titleLarge, color = HrColor.Surface)
            // 卡片里不再重复一句与副标题相同的提示，改为引导动作
            Text(
                "准备好就点下面开始，跟着语音走完一组。",
                style = HrType.bodySmall,
                color = HrColor.PanelTextMuted,
            )

            VSpace(HrSpacing.xxs)

            HrPrimaryButton(
                text = "开始",
                tag = UiTag.P02_START_BUTTON,
                containerColor = HrColor.Accent,
                contentColor = HrColor.OnAccent,
                onClick = onStart,
            )
        }
    }
}

/** 已完成卡（白色，设计稿 P02-b）。 */
@Composable
private fun DoneCard(sessionCount: Int, totalSec: Long, onAgain: () -> Unit) {
    HrCard(modifier = Modifier.testTag(UiTag.P02_DONE_CARD)) {
        Column(verticalArrangement = Arrangement.spacedBy(HrSpacing.sm)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HrSpacing.xs),
            ) {
                Box(
                    Modifier
                        .sizeIn(minWidth = 24.dp, minHeight = 24.dp)
                        .clip(CircleShape)
                        .background(HrColor.Good)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✓", style = HrType.label, color = HrColor.Surface)
                }
                Text("今天已完成，可再练一次", style = HrType.bodyMediumBold, color = HrColor.Ink)
            }
            Text(
                text = "本周 $sessionCount 次 · 累计 ${formatMinutes(totalSec)}",
                style = HrType.bodySmall,
                color = HrColor.InkSecondary,
            )
            HrSecondaryButton(
                text = "再练一次",
                tag = UiTag.P02_AGAIN_BUTTON,
                onClick = onAgain,
            )
        }
    }
}

/**
 * 感知状态行（FR-02 三态）。
 *
 * **可点，但不阻断**：不点它也能直接开始训练。这一点是 AC-04 的核心。
 */
@Composable
private fun SensorRow(sensor: SensorStatusRow, onClick: () -> Unit) {
    // onClick 必须真的接上：首版这个形参从未被引用，
    // 于是"点这里了解"是**假承诺**，而 P03 校准页整个变成不可达路由。
    val (tone, label) = when (sensor) {
        SensorStatusRow.OFF -> ChipTone.NEUTRAL to "${sensor.label} · 点这里了解"
        SensorStatusRow.WEAK -> ChipTone.WARN to sensor.label
        SensorStatusRow.READY -> ChipTone.GOOD to sensor.label
    }
    HrStatusChip(
        text = label,
        tone = tone,
        tag = UiTag.P02_SENSOR_STATUS,
        modifier = Modifier.padding(vertical = 2.dp),
        onClick = onClick,
    )
}

/** 秒 → 「3 分 20 秒」；不足 1 分钟显示秒，符合老年用户阅读习惯。 */
internal fun formatMinutes(totalSec: Long): String {
    if (totalSec <= 0L) return "0 秒"
    val m = totalSec / 60
    val s = totalSec % 60
    return when {
        m <= 0L -> "$s 秒"
        s == 0L -> "$m 分钟"
        else -> "$m 分 $s 秒"
    }
}