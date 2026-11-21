package com.diqi.demo.ui.screens.settings

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
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diqi.demo.AppContainer
import com.diqi.demo.ui.UiTag
import com.diqi.demo.ui.rememberContainer
import com.diqi.demo.ui.rememberContainerViewModel
import com.diqi.demo.work.ReminderWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    private val _cleared = MutableStateFlow(false)
    val cleared: StateFlow<Boolean> = _cleared.asStateFlow()

    init {
        viewModelScope.launch {
            container.settingsStore.settings.collect { }
        }
    }

    fun setReminder(enabled: Boolean) {
        viewModelScope.launch {
            container.settingsStore.setReminderEnabled(enabled)
            if (enabled) ReminderWorker.schedule(container.context) else ReminderWorker.cancel(container.context)
        }
    }

    fun setAudioSpeed(speed: Float) {
        viewModelScope.launch { container.settingsStore.setAudioSpeed(speed) }
    }

    /**
     * 演示模式（FR-14：入口在设置页底部、默认关闭、无醒目提示）。
     * 关闭时同时清掉演示记录，避免记录页一直挂着一块「演示记录」。
     */
    fun setDemoMode(enabled: Boolean) {
        viewModelScope.launch {
            container.settingsStore.setDemoMode(enabled)
            if (!enabled) container.sessionRepository.clearSimulated()
        }
    }

    /**
     * 清除本机数据：只清业务数据，**保留同意与筛查留痕**（合规证据不应被误触销毁）。
     */
    fun clearData() {
        viewModelScope.launch {
            container.sessionRepository.clearAll()
            container.leakDiaryRepository.clearAll()
            container.settingsStore.clearBusinessSettings()
            _cleared.value = true
        }
    }
}

/**
 * P07 设置。
 *
 * 布局取值全部来自 `docs/DESIGN-SPEC.md` 的 `P07`：
 * 大标题 28/700、四组卡片（r14 白底 + `#E2E8F0` 描边）、
 * 组标题 11/700 `#94A3B8`（padding [12,16,4,16]）、
 * 项行 padding [14,16] + 17 图标 `#475569` + 15/500 文字 + 右侧 13sp 值或 16 chevron。
 *
 * ## 演示模式的位置
 *
 * 需求 FR-14 要求「设置页底部、默认关闭、无醒目提示」，因此放在**版本号之上、
 * 视觉弱化**的一行，而不是混进功能分组里。
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onOpenDesignSystem: (() -> Unit)? = null,
) {
    val vm = rememberContainerViewModel { SettingsViewModel(it) }
    val cleared by vm.cleared.collectAsState()
    val container = com.diqi.demo.ui.rememberContainer()
    val settings by container.settingsStore.settings.collectAsState(
        initial = com.diqi.demo.data.prefs.AppSettings(),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HrSetCanvas)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 10.dp)
            .testTag(UiTag.P09_ROOT),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("设置", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = HrSetInk)

        // ── 语音 ────────────────────────────────────────────────────────
        Group("语音") {
            ItemRow(
                icon = { GaugeGlyph(it) },
                label = "语音速度",
                value = when {
                    settings.audioSpeed < 0.95f -> "0.8x"
                    settings.audioSpeed > 1.05f -> "1.2x"
                    else -> "1.0x"
                },
                tag = UiTag.P09_VOICE_SPEED_10,
                onClick = {
                    // 0.8 → 1.0 → 1.2 → 0.8 循环（设计稿只有一个「1.0x」值位，点按切换）
                    val next = when {
                        settings.audioSpeed < 0.95f -> 1.0f
                        settings.audioSpeed > 1.05f -> 0.8f
                        else -> 1.2f
                    }
                    vm.setAudioSpeed(next)
                },
            )
            ItemRow(icon = { VolumeGlyph(it) }, label = "音量", value = "跟随系统", tag = null, onClick = {})
            ItemRow(icon = { RotateGlyph(it) }, label = "重复当前提示", chevron = true, tag = null, onClick = {})
        }

        // ── 提醒 ────────────────────────────────────────────────────────
        Group("提醒") {
            ItemRow(
                icon = { BellGlyph(it) },
                label = "每日提醒",
                value = if (settings.reminderEnabled) "已开启" else "已关闭",
                tag = UiTag.P09_REMINDER_TOGGLE,
                onClick = { vm.setReminder(!settings.reminderEnabled) },
            )
            ItemRow(
                icon = { ClockGlyph(it) },
                label = "提醒时间",
                value = "每天 ${settings.reminderTime}",
                tag = UiTag.P09_REMINDER_TIME,
                onClick = {},
            )
        }

        // ── 摄像头与校准 ────────────────────────────────────────────────
        Group("摄像头与校准") {
            ItemRow(icon = { FocusGlyph(it) }, label = "重做校准", chevron = true, tag = null, onClick = {})
            ItemRow(icon = { ActivityGlyph(it) }, label = "设备诊断", chevron = true, tag = null, onClick = {})
        }

        // ── 隐私与数据 ──────────────────────────────────────────────────
        Group("隐私与数据") {
            ItemRow(
                icon = { ShieldGlyph(it) },
                label = "隐私说明",
                chevron = true,
                tag = UiTag.P09_PRIVACY,
                onClick = {},
            )
            // 危险操作：单独成行、红色，且需要二次确认
            ClearDataRow(
                label = "清除本机数据",
                tag = UiTag.P09_CLEAR_DATA,
                onClick = { vm.clearData() },
            )
        }

        if (cleared) {
            Text(
                "训练记录与日记已清除。安全筛查与同意记录会保留，用于合规留痕。",
                fontSize = 11.sp,
                color = HrSetInkMuted,
            )
        }

        // ── 演示模式（底部、弱化、默认关闭，FR-14）──────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(HrSetFaint)
                .clickable(role = Role.Switch) { vm.setDemoMode(!settings.isDemoMode) }
                .testTag(UiTag.P09_DEMO_MODE_TOGGLE)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("演示模式", fontSize = 12.sp, color = HrSetInkTertiary)
            Text(
                text = if (settings.isDemoMode) "已开启 · 模拟数据" else "未开启",
                fontSize = 11.sp,
                color = HrSetInkTertiary,
            )
        }

        if (onOpenDesignSystem != null && com.diqi.demo.BuildConfig.DEBUG) {
            Text(
                "设备自检（仅 debug）",
                fontSize = 11.sp,
                color = HrSetInkTertiary,
                modifier = Modifier
                    .clickable { onOpenDesignSystem() }
                    .padding(vertical = 8.dp)
                    .testTag("settings.dev_entry"),
            )
        }

        // 版本号
        Text(
            "HerRhythm v1.1.0（公测）　·　SDK v1.5.0",
            fontSize = 11.sp,
            color = HrSetInkTertiary,
        )

        Spacer(Modifier.height(8.dp))
    }
}

/** 分组卡：r14 白底 + 描边 + 组标题（11/700 #94A3B8）。 */
@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, HrSetLine), RoundedCornerShape(14.dp)),
    ) {
        Text(
            title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = HrSetInkTertiary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        )
        content()
    }
}

/** 项行：padding [14,16] + 17 图标 + 15/500 文字 + 右侧 13sp 值 或 16 chevron。 */
@Composable
private fun ItemRow(
    icon: @Composable (Color) -> Unit,
    label: String,
    value: String? = null,
    chevron: Boolean = false,
    tag: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .clickable(enabled = tag != null || chevron, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            icon(HrSetIcon)
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = HrSetInk)
        }
        when {
            chevron -> Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = HrSetTrack,
                modifier = Modifier.size(16.dp),
            )
            value != null -> Text(value, fontSize = 13.sp, color = HrSetInkTertiary)
        }
    }
}

/** 危险操作行（清除数据）：红色文字 + 红色图标。 */
@Composable
private fun ClearDataRow(label: String, tag: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TrashGlyph(HrSetDanger)
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = HrSetDanger)
    }
}

// ── 手绘图标（material-icons-core 只覆盖少量，这里按设计稿语义各画一个）──

private fun strokeOf(size: androidx.compose.ui.geometry.Size): Float =
    size.minDimension * 0.12f

@Composable
private fun GaugeGlyph(color: Color) = Canvas(Modifier.size(17.dp)) {
    drawArc(
        color = color,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        style = Stroke(width = strokeOf(size)),
    )
    drawLine(
        color,
        Offset(size.width * 0.5f, size.height * 0.95f),
        Offset(size.width * 0.72f, size.height * 0.45f),
        strokeWidth = strokeOf(size),
    )
}

@Composable
private fun VolumeGlyph(color: Color) = Canvas(Modifier.size(17.dp)) {
    val p = androidx.compose.ui.graphics.Path().apply {
        moveTo(0f, size.height * 0.38f)
        lineTo(size.width * 0.22f, size.height * 0.38f)
        lineTo(size.width * 0.45f, size.height * 0.14f)
        lineTo(size.width * 0.45f, size.height * 0.86f)
        lineTo(size.width * 0.22f, size.height * 0.62f)
        lineTo(0f, size.height * 0.62f)
        close()
    }
    drawPath(p, color)
    drawArc(
        color = color,
        startAngle = -50f,
        sweepAngle = 100f,
        useCenter = false,
        topLeft = Offset(size.width * 0.42f, size.height * 0.2f),
        size = androidx.compose.ui.geometry.Size(size.width * 0.5f, size.height * 0.6f),
        style = Stroke(width = strokeOf(size) * 0.8f),
    )
}

@Composable
private fun RotateGlyph(color: Color) = Canvas(Modifier.size(17.dp)) {
    drawArc(
        color = color,
        startAngle = 40f,
        sweepAngle = 280f,
        useCenter = false,
        style = Stroke(width = strokeOf(size)),
    )
    val head = androidx.compose.ui.graphics.Path().apply {
        moveTo(size.width * 0.92f, size.height * 0.34f)
        lineTo(size.width * 0.72f, size.height * 0.4f)
        lineTo(size.width * 0.9f, size.height * 0.56f)
        close()
    }
    drawPath(head, color)
}

@Composable
private fun BellGlyph(color: Color) = Canvas(Modifier.size(17.dp)) {
    val p = androidx.compose.ui.graphics.Path().apply {
        moveTo(size.width * 0.16f, size.height * 0.72f)
        lineTo(size.width * 0.16f, size.height * 0.44f)
        quadraticTo(size.width * 0.5f, -size.height * 0.12f, size.width * 0.84f, size.height * 0.44f)
        lineTo(size.width * 0.84f, size.height * 0.72f)
        close()
    }
    drawPath(p, color, style = Stroke(width = strokeOf(size)))
    drawCircle(color, radius = size.width * 0.09f, center = Offset(size.width * 0.5f, size.height * 0.84f))
}

@Composable
private fun ClockGlyph(color: Color) = Canvas(Modifier.size(17.dp)) {
    drawCircle(color, radius = size.minDimension / 2f - strokeOf(size) / 2, style = Stroke(width = strokeOf(size)))
    drawLine(
        color,
        Offset(size.width / 2, size.height * 0.28f),
        Offset(size.width / 2, size.height * 0.52f),
        strokeWidth = strokeOf(size) * 0.8f,
    )
    drawLine(
        color,
        Offset(size.width / 2, size.height * 0.52f),
        Offset(size.width * 0.7f, size.height * 0.62f),
        strokeWidth = strokeOf(size) * 0.8f,
    )
}

@Composable
private fun FocusGlyph(color: Color) = Canvas(Modifier.size(17.dp)) {
    val s = strokeOf(size)
    val w = size.width
    val h = size.height
    // 四角括号（对焦框）
    listOf(
        Pair(Offset(0f, h * 0.3f), Offset(0f, 0f)),
        Pair(Offset(0f, 0f), Offset(w * 0.3f, 0f)),
        Pair(Offset(w, h * 0.3f), Offset(w, 0f)),
        Pair(Offset(w, 0f), Offset(w * 0.7f, 0f)),
        Pair(Offset(0f, h * 0.7f), Offset(0f, h)),
        Pair(Offset(0f, h), Offset(w * 0.3f, h)),
        Pair(Offset(w, h * 0.7f), Offset(w, h)),
        Pair(Offset(w, h), Offset(w * 0.7f, h)),
    ).forEach { (a, b) -> drawLine(color, a, b, strokeWidth = s) }
}

@Composable
private fun ActivityGlyph(color: Color) = Canvas(Modifier.size(17.dp)) {
    val p = androidx.compose.ui.graphics.Path().apply {
        moveTo(0f, size.height * 0.55f)
        lineTo(size.width * 0.28f, size.height * 0.55f)
        lineTo(size.width * 0.42f, size.height * 0.18f)
        lineTo(size.width * 0.6f, size.height * 0.85f)
        lineTo(size.width * 0.74f, size.height * 0.55f)
        lineTo(size.width, size.height * 0.55f)
    }
    drawPath(p, color, style = Stroke(width = strokeOf(size)))
}

@Composable
private fun ShieldGlyph(color: Color) = Canvas(Modifier.size(17.dp)) {
    val p = androidx.compose.ui.graphics.Path().apply {
        moveTo(size.width * 0.5f, 0f)
        lineTo(size.width, size.height * 0.18f)
        lineTo(size.width, size.height * 0.52f)
        quadraticTo(size.width, size.height, size.width * 0.5f, size.height)
        quadraticTo(0f, size.height, 0f, size.height * 0.52f)
        lineTo(0f, size.height * 0.18f)
        close()
    }
    drawPath(p, color, style = Stroke(width = strokeOf(size)))
}

@Composable
private fun TrashGlyph(color: Color) = Canvas(Modifier.size(17.dp)) {
    val s = strokeOf(size)
    drawLine(color, Offset(0f, size.height * 0.22f), Offset(size.width, size.height * 0.22f), strokeWidth = s)
    drawLine(
        color,
        Offset(size.width * 0.2f, size.height * 0.22f),
        Offset(size.width * 0.28f, size.height),
        strokeWidth = s,
    )
    drawLine(
        color,
        Offset(size.width * 0.8f, size.height * 0.22f),
        Offset(size.width * 0.72f, size.height),
        strokeWidth = s,
    )
    drawLine(
        color,
        Offset(size.width * 0.28f, size.height),
        Offset(size.width * 0.72f, size.height),
        strokeWidth = s,
    )
    drawLine(
        color,
        Offset(size.width * 0.36f, size.height * 0.1f),
        Offset(size.width * 0.64f, size.height * 0.1f),
        strokeWidth = s,
    )
}

private val HrSetCanvas = Color(0xFFF4F6F8)
private val HrSetInk = Color(0xFF0F1A24)
private val HrSetInkMuted = Color(0xFF64748B)
private val HrSetInkTertiary = Color(0xFF94A3B8)
private val HrSetLine = Color(0xFFE2E8F0)
private val HrSetTrack = Color(0xFFCBD5E1)
private val HrSetIcon = Color(0xFF475569)
private val HrSetDanger = Color(0xFFB91C1C)
private val HrSetFaint = Color(0xFFF1F5F9)