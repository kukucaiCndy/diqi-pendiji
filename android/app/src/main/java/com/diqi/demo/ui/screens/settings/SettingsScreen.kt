package com.diqi.demo.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diqi.demo.AppContainer
import com.diqi.demo.data.prefs.AppSettings
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
import com.diqi.demo.work.ReminderWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val cleared: Boolean = false,
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            container.settingsStore.settings.collect {
                _state.value = _state.value.copy(settings = it)
            }
        }
    }

    fun setAudioSpeed(speed: Float) {
        viewModelScope.launch { container.settingsStore.setAudioSpeed(speed) }
    }

    fun setReminder(enabled: Boolean) {
        viewModelScope.launch {
            container.settingsStore.setReminderEnabled(enabled)
            if (enabled) ReminderWorker.schedule(container.context)
            else ReminderWorker.cancel(container.context)
        }
    }

    /**
     * 演示模式开关（FR-14）。
     *
     * 关闭时必须**同时清掉演示产生的记录** —— 需求原文「演示数据不写库、
     * 不计入打卡」，虽按 D9 裁定落了库，但关闭演示时清掉是最不容易出错的收敛方式
     * （否则记录页会一直挂着一块"演示记录"）。
     */
    fun setDemoMode(enabled: Boolean) {
        viewModelScope.launch {
            container.settingsStore.setDemoMode(enabled)
            if (!enabled) container.sessionRepository.clearSimulated()
        }
    }

    /**
     * 清除数据。
     *
     * 只清业务数据（训练记录、日记、业务偏好），**保留同意与筛查留痕** ——
     * 合规证据不应被用户误触销毁（评审 A4 的裁定）。
     */
    fun clearData() {
        viewModelScope.launch {
            container.sessionRepository.clearAll()
            container.leakDiaryRepository.clearAll()
            container.settingsStore.clearBusinessSettings()
            _state.value = _state.value.copy(cleared = true)
        }
    }
}

/**
 * P09 设置（P1 基础版）。
 *
 * 布局顺序刻意把「演示模式」放在**最底部且视觉弱化**（需求 FR-14：
 * 「入口：设置页底部「演示模式」，默认关闭，无醒目提示」）。
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onOpenDesignSystem: (() -> Unit)? = null,
) {
    val vm = rememberContainerViewModel { SettingsViewModel(it) }
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    HrPage(modifier = modifier, tag = UiTag.P09_ROOT) {
        Text("我的", style = HrType.headline, color = HrColor.Ink)
        VSpace(HrSpacing.xxs)
        Text(
            text = "语音、提醒与隐私设置",
            style = HrType.bodySmall,
            color = HrColor.InkSecondary,
        )

        VSpace(HrSpacing.md)

        // ── 语音速度 ──────────────────────────────────────────────────────
        HrCard {
            Column(verticalArrangement = Arrangement.spacedBy(HrSpacing.xs)) {
                Text("语音速度", style = HrType.label, color = HrColor.Ink)
                Row(horizontalArrangement = Arrangement.spacedBy(HrSpacing.xs)) {
                    SpeedPill(
                        label = "慢", value = 0.8f, current = state.settings.audioSpeed,
                        tag = UiTag.P09_VOICE_SPEED_08, onClick = vm::setAudioSpeed,
                        modifier = Modifier.weight(1f),
                    )
                    SpeedPill(
                        label = "标准", value = 1.0f, current = state.settings.audioSpeed,
                        tag = UiTag.P09_VOICE_SPEED_10, onClick = vm::setAudioSpeed,
                        modifier = Modifier.weight(1f),
                    )
                    SpeedPill(
                        label = "快", value = 1.2f, current = state.settings.audioSpeed,
                        tag = UiTag.P09_VOICE_SPEED_12, onClick = vm::setAudioSpeed,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    "语音素材是预先录好的，切换后从下一句开始生效。",
                    style = HrType.disclaimer,
                    color = HrColor.InkSecondary,
                )
            }
        }

        VSpace(HrSpacing.sm)

        // ── 提醒 ──────────────────────────────────────────────────────────
        HrCard {
            Column(verticalArrangement = Arrangement.spacedBy(HrSpacing.xs)) {
                Text("每天提醒", style = HrType.label, color = HrColor.Ink)
                HrStatusChip(
                    text = if (state.settings.reminderEnabled) {
                        "已开启 · 每天 ${state.settings.reminderTime}"
                    } else {
                        "已关闭 · 点这里开启"
                    },
                    tone = if (state.settings.reminderEnabled) ChipTone.GOOD else ChipTone.NEUTRAL,
                    tag = UiTag.P09_REMINDER_TOGGLE,
                    onClick = { vm.setReminder(!state.settings.reminderEnabled) },
                )
                Text(
                    "提醒只是一条本地通知，不会联网，也不会发给你以外的人。",
                    style = HrType.disclaimer,
                    color = HrColor.InkSecondary,
                    modifier = Modifier.testTag(UiTag.P09_REMINDER_TIME),
                )
            }
        }

        VSpace(HrSpacing.sm)

        // ── 隐私 ──────────────────────────────────────────────────────────
        HrCard(color = HrColor.SubtleContainer, modifier = Modifier.testTag(UiTag.P09_PRIVACY)) {
            Column(verticalArrangement = Arrangement.spacedBy(HrSpacing.xxs)) {
                Text("隐私说明", style = HrType.label, color = HrColor.Ink)
                Text("· 画面只在手机本地处理，不保存、不上传", style = HrType.bodySmall, color = HrColor.Slate800)
                Text("· 训练记录、日记只存在这台手机上", style = HrType.bodySmall, color = HrColor.Slate800)
                Text("· 云端备份与换机迁移均已关闭", style = HrType.bodySmall, color = HrColor.Slate800)
            }
        }

        VSpace(HrSpacing.sm)

        HrSecondaryButton(
            text = "清除数据",
            tag = UiTag.P09_CLEAR_DATA,
            onClick = { vm.clearData() },
        )
        if (state.cleared) {
            VSpace(HrSpacing.xxs)
            HrBanner(
                text = "训练记录与日记已清除。安全筛查与同意记录会保留，用于合规留痕。",
                tone = ChipTone.NEUTRAL,
            )
        }

        if (onOpenDesignSystem != null && com.diqi.demo.BuildConfig.DEBUG) {
            VSpace(HrSpacing.sm)
            HrSecondaryButton(
                text = "设备自检（仅 debug）",
                tag = "settings.dev_entry",
                onClick = onOpenDesignSystem,
            )
        }

        VSpace(HrSpacing.xl)

        // ── 演示模式：底部、弱化、默认关闭（FR-14）────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .clip(HrRadius.controlShape)
                .background(HrColor.FaintContainer)
                .clickable(role = Role.Switch) { vm.setDemoMode(!state.settings.isDemoMode) }
                .testTag(UiTag.P09_DEMO_MODE_TOGGLE)
                .padding(HrSpacing.sm),
        ) {
            Column {
                Text(
                    "演示模式",
                    style = HrType.labelSmall,
                    color = HrColor.WeakText,
                )
                Text(
                    text = if (state.settings.isDemoMode) {
                        "已开启 · 指标为模拟数据，不会计入打卡"
                    } else {
                        "未开启"
                    },
                    style = HrType.micro,
                    color = HrColor.WeakText,
                )
            }
        }

        VSpace(HrSpacing.xxl)
    }
}

@Composable
private fun SpeedPill(
    label: String,
    value: Float,
    current: Float,
    tag: String,
    onClick: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = kotlin.math.abs(current - value) < 0.01f
    Box(
        modifier = modifier
            .heightIn(min = HrSize.minTouchTarget)
            .clip(HrRadius.controlShape)
            .background(if (selected) HrColor.GoodContainer else HrColor.Surface)
            .border(1.dp, if (selected) HrColor.Good else HrColor.Line, HrRadius.controlShape)
            .clickable(role = Role.RadioButton) { onClick(value) }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = if (selected) HrType.bodyMediumBold else HrType.bodyMedium,
            color = if (selected) HrColor.GoodDeep else HrColor.Slate800,
        )
    }
}