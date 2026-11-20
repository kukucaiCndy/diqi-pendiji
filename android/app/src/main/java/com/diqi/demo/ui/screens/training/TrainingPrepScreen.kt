package com.diqi.demo.ui.screens.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.diqi.demo.audio.VoiceScript
import com.diqi.demo.ui.components.ChipTone
import com.diqi.demo.ui.components.HrBanner
import com.diqi.demo.ui.components.HrCard
import com.diqi.demo.ui.components.HrPageWithAction
import com.diqi.demo.ui.components.HrPrimaryButton
import com.diqi.demo.ui.components.HrSecondaryButton
import com.diqi.demo.ui.components.HrStatusChip
import com.diqi.demo.ui.components.VSpace
import com.diqi.demo.ui.rememberContainerViewModel
import com.diqi.demo.ui.screens.home.HomeViewModel
import com.diqi.demo.ui.theme.HrColor
import com.diqi.demo.ui.theme.HrSpacing
import com.diqi.demo.ui.theme.HrType

/**
 * 训练准备页（P03 的入口页之一，需求 FR-03）。
 *
 * ## 这个页面存在的一个理由
 *
 * 「摄像头是**可选**的」这句话必须在用户真正开始前出现在她能看到的界面上，
 * 而不是藏在隐私说明里。所以这里有一个显式的入口：
 * 「开启摄像头（可选）」+ 常驻的「直接开始」。
 * 不点摄像头也一定能开始 —— 这是 AC-04 的用户可见形态。
 */
@Composable
fun TrainingPrepScreen(
    onStartTraining: () -> Unit,
    onOpenCalibrate: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = rememberContainerViewModel { HomeViewModel(it) }
    val state by vm.state.collectAsState()

    HrPageWithAction(
        modifier = modifier,
        tag = "prep.root",
        action = {
            HrPrimaryButton(
                text = "开始训练",
                tag = "prep.start_button",
                onClick = onStartTraining,
            )
        },
    ) {
        Text("准备开始", style = HrType.headline, color = HrColor.Ink)
        VSpace(HrSpacing.xxs)
        Text(
            text = "${state.dayTheme} · ${state.dayHint}",
            style = HrType.bodySmall,
            color = HrColor.InkSecondary,
        )

        VSpace(HrSpacing.md)

        HrCard(color = HrColor.SubtleContainer) {
            Text(
                text = VoiceScript.START.text,
                style = HrType.bodyMedium,
                color = HrColor.Slate800,
            )
        }

        if (state.clinicalNoticeRequired) {
            VSpace(HrSpacing.sm)
            HrBanner(
                text = "按之前的回答，练到不舒服就停下来，并先咨询医生。",
                tone = ChipTone.WARN,
            )
        }

        VSpace(HrSpacing.md)

        // 感知入口：默认关闭，不点也能开始（AC-04）
        HrStatusChip(
            text = "开启摄像头（可选）",
            tone = ChipTone.NEUTRAL,
            tag = "prep.camera_entry",
            onClick = onOpenCalibrate,
        )

        VSpace(HrSpacing.xs)

        Text(
            text = "不想开也可以直接开始，练习内容完全一样。",
            style = HrType.bodySmall,
            color = HrColor.InkSecondary,
        )

        VSpace(HrSpacing.md)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HrSpacing.xs),
        ) {
            HrSecondaryButton(
                text = "返回",
                tag = "prep.back_button",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(0.5f),
            )
            Box(Modifier.weight(1f)) { }
        }

        VSpace(HrSpacing.lg)
    }
}