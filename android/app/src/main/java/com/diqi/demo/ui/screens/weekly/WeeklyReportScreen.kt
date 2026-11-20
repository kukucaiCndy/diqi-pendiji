package com.diqi.demo.ui.screens.weekly

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diqi.demo.AppContainer
import com.diqi.demo.share.ShareCard
import com.diqi.demo.share.ShareCardBuilder
import com.diqi.demo.ui.UiTag
import com.diqi.demo.ui.components.ChipTone
import com.diqi.demo.ui.components.HrBanner
import com.diqi.demo.ui.components.HrCard
import com.diqi.demo.ui.components.HrPage
import com.diqi.demo.ui.components.HrPrimaryButton
import com.diqi.demo.ui.components.HrStatusChip
import com.diqi.demo.ui.components.VSpace
import com.diqi.demo.ui.rememberContainerViewModel
import com.diqi.demo.ui.screens.home.formatMinutes
import com.diqi.demo.ui.theme.HrColor
import com.diqi.demo.ui.theme.HrSpacing
import com.diqi.demo.ui.theme.HrType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WeeklyUiState(
    val loading: Boolean = true,
    val trainedDays: Int = 0,
    val totalSec: Long = 0,
    val dayIndex: Int = 1,
    /** FR-13：默认关闭分享 */
    val shareEnabled: Boolean = false,
    val card: ShareCard? = null,
)

/**
 * P08 家人周报（极简版）。
 *
 * ## 三条红线的落地方式
 *
 * 1. **默认关闭分享**：`shareEnabled` 初值来自设置，默认 false；
 *    关闭时「分享」按钮禁用，只能预览（AC-21）
 * 2. **内容仅三项**：天数 / 时长 / 鼓励语由 [ShareCardBuilder] 生成，
 *    该类有单测逐条断言不含心率与日记（AC-22）
 * 3. **不授权就绝不发起分享**：`onClick` 里再判一次 `shareEnabled`，
 *    避免状态漂移导致误分享
 */
class WeeklyViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(WeeklyUiState())
    val state: StateFlow<WeeklyUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val settings = container.settingsStore.current()
            val week = container.progressRepository.lastSevenDays(now)
            val dayIndex = container.progressRepository.currentDayIndex(now)
            _state.value = WeeklyUiState(
                loading = false,
                trainedDays = week.trainedDays.size,
                totalSec = week.totalSec,
                dayIndex = dayIndex,
                shareEnabled = settings.shareEnabled,
                card = ShareCardBuilder.build(week, dayIndex),
            )
        }
    }

    fun setShareEnabled(enabled: Boolean) {
        viewModelScope.launch {
            container.settingsStore.setShareEnabled(enabled, System.currentTimeMillis())
            _state.value = _state.value.copy(shareEnabled = enabled)
        }
    }
}

@Composable
fun WeeklyReportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = rememberContainerViewModel { WeeklyViewModel(it) }
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    HrPage(modifier = modifier, tag = UiTag.P08_ROOT) {
        Text("家人周报", style = HrType.headline, color = HrColor.Ink)
        VSpace(HrSpacing.xxs)
        Text(
            text = "只包含本周天数与时长，不包含任何身体数据。",
            style = HrType.bodySmall,
            color = HrColor.InkSecondary,
        )

        VSpace(HrSpacing.md)

        // 预览卡（先预览再分享，FR-13）
        HrCard(modifier = Modifier.testTag(UiTag.P08_PREVIEW_CARD)) {
            Column(verticalArrangement = Arrangement.spacedBy(HrSpacing.xs)) {
                Text(
                    "${state.dayIndex} / 7 天体验中",
                    style = HrType.micro,
                    color = HrColor.Brand,
                )
                Text(
                    text = if (state.trainedDays > 0) {
                        "这周我完成了 ${state.trainedDays} 天练习"
                    } else {
                        "这周还没开始练习"
                    },
                    style = HrType.titleSmall,
                    color = HrColor.Ink,
                )
                Text(
                    text = if (state.trainedDays > 0) {
                        "累计 ${formatMinutes(state.totalSec)}"
                    } else {
                        "下周一起加油"
                    },
                    style = HrType.bodySmall,
                    color = HrColor.InkSecondary,
                )

                VSpace(HrSpacing.xxs)

                Text(
                    "内容不含：心率、呼吸率、HRV、漏尿记录、任何画面。",
                    style = HrType.disclaimer,
                    color = HrColor.InkSecondary,
                )
            }
        }

        VSpace(HrSpacing.sm)

        // 分享开关：默认关闭
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            val hint = if (state.shareEnabled) {
                "分享已开启 · 可以点下面的按钮"
            } else {
                "分享默认关闭 · 打开后才能发送"
            }
            HrStatusChip(
                text = hint,
                tone = if (state.shareEnabled) ChipTone.GOOD else ChipTone.NEUTRAL,
                tag = UiTag.P08_ENABLE_SHARE_TOGGLE,
                onClick = { vm.setShareEnabled(!state.shareEnabled) },
            )
        }

        VSpace(HrSpacing.md)

        HrPrimaryButton(
            text = if (state.shareEnabled) "分享给家人" else "开启分享后才能发送",
            tag = UiTag.P08_SHARE_BUTTON,
            enabled = state.shareEnabled,
            onClick = {
                // 双重守卫：即使 enabled 判断被绕过，也不发起分享（FR-13）
                if (!state.shareEnabled) return@HrPrimaryButton
                val card = state.card ?: return@HrPrimaryButton
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, card.toShareText())
                    putExtra(Intent.EXTRA_SUBJECT, card.title)
                }
                context.startActivity(
                    Intent.createChooser(send, "分享给家人").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
        )

        VSpace(HrSpacing.sm)

        HrBanner(
            text = "分享出去后由你自己决定发给谁；应用不做接收人管理，也不会自动发送。",
            tone = ChipTone.NEUTRAL,
        )

        VSpace(HrSpacing.xxl)
    }
}

private val HrPrimaryButtonLabel: Unit = Unit