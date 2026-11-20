package com.diqi.demo.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.diqi.demo.data.prefs.ScreeningAnswer
import com.diqi.demo.ui.UiTag
import com.diqi.demo.ui.components.HrBanner
import com.diqi.demo.ui.components.HrCard
import com.diqi.demo.ui.components.HrPage
import com.diqi.demo.ui.components.HrPageWithAction
import com.diqi.demo.ui.components.HrPrimaryButton
import com.diqi.demo.ui.components.HrSecondaryButton
import com.diqi.demo.ui.components.ChipTone
import com.diqi.demo.ui.components.VSpace
import com.diqi.demo.ui.theme.HrColor
import com.diqi.demo.ui.theme.HrRadius
import com.diqi.demo.ui.theme.HrSize
import com.diqi.demo.ui.theme.HrSpacing
import com.diqi.demo.ui.theme.HrType

/**
 * P01-a 欢迎与隐私。
 *
 * 顺序是**合规关键**：价值说明 → **隐私说明** → 主按钮。
 * 隐私必须先于任何授权（FR-01），所以这里没有任何权限申请，
 * 相机权限只在用户真的点「开启摄像头」时才请求（S6）。
 */
@Composable
fun WelcomeScreen(
    vm: OnboardingViewModel,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {

    // 主操作固定在底部：2.0× 字号下「开始」必须始终可见（AC-09 / 一屏一主任务）
    HrPageWithAction(
        modifier = modifier,
        tag = UiTag.P01_ROOT,
        action = {
            // 动作区只放主按钮：免责声明属于正文，跟着内容滚，
            // 否则 2.0× 字号下它会独占三行、把可滚动区压得很小。
            HrPrimaryButton(
                text = "开始",
                tag = UiTag.P01_PRIMARY_BUTTON,
                onClick = {
                    vm.acceptPrivacy()
                    onStart()
                },
            )
        },
    ) {
        // 品牌行
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(HrColor.Good),
            )
            Spacer(Modifier.size(8.dp))
            Text("她的节律 · HerRhythm", style = HrType.micro, color = HrColor.Good)
        }

        VSpace(HrSpacing.xxl)

        Text(
            text = "今天，我们一起练习照顾自己的身体。",
            style = HrType.display,
            color = HrColor.Ink,
        )

        VSpace(HrSpacing.md)

        Text(
            text = "跟着语音的节奏做一组轻轻的练习，每天几分钟。不用戴任何设备。",
            style = HrType.bodyMedium,
            color = HrColor.InkSecondary,
        )

        VSpace(HrSpacing.xl)

        // 隐私卡（FR-01 三项必写）
        HrCard(color = HrColor.SubtleContainer, modifier = Modifier.testTag(UiTag.P01_PRIVACY_CARD)) {
            Column(verticalArrangement = Arrangement.spacedBy(HrSpacing.sm)) {
                Text("关于你的隐私", style = HrType.titleSmall, color = HrColor.Ink)
                PrivacyRow("画面只在手机本地处理")
                PrivacyRow("不录像，不留任何画面")
                PrivacyRow("不上传，原始画面不出手机")
            }
        }

        VSpace(HrSpacing.sm)

        Text(
            text = "摄像头是可选的。不开启也能完成全部练习。",
            style = HrType.bodySmall,
            color = HrColor.InkSecondary,
        )

        VSpace(HrSpacing.sm)

        Text(
            text = "本应用用于日常练习引导，不能替代医生诊断或治疗。",
            style = HrType.disclaimer,
            color = HrColor.InkSecondary,
        )

        VSpace(HrSpacing.xl)
    }
}

@Composable
private fun PrivacyRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HrSpacing.xs),
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(HrColor.Surface),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(HrColor.Good),
            )
        }
        Text(text, style = HrType.bodyMediumBold, color = HrColor.Slate800)
    }
}

/**
 * P01-b 安全筛查。
 *
 * 一屏一题、进度可见、三选项（含不确定）。答完后若触发分流，
 * 结果由 `SafetyScreening` 的派生属性表达，本页只负责展示提示。
 */
@Composable
fun ScreeningScreen(
    vm: OnboardingViewModel,
    onFinished: () -> Unit,
    onBackFromFirst: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 注意：VM 由导航层传入，**不能在这里再 new 一个** ——
    // 首版两个页面各建一个实例，导致"欢迎→筛查"共享不到答案，
    // 且筛查页 pop 后 entry-scoped 的实例被清空（评审指出）。
    val state by vm.state.collectAsState()

    val question = state.current ?: return

    HrPageWithAction(
        modifier = modifier,
        tag = UiTag.P01_SCREENING_ROOT,
        action = {
            HrPrimaryButton(
                text = if (state.isLast) "完成" else "下一题",
                tag = UiTag.P01_SCREENING_NEXT,
                enabled = state.canGoNext && !state.saving,
                onClick = {
                    if (state.isLast) vm.submit(onFinished) else vm.next()
                },
            )
            VSpace(HrSpacing.xs)
            HrSecondaryButton(
                text = "跳过，直接开始",
                // 跳过也必须落"已确认"标记，否则每次冷启动都会重走引导
                onClick = { vm.skip(onFinished) },
            )
        },
    ) {
        // 导航行
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                Modifier
                    .heightIn(min = HrSize.minTouchTarget)
                    .clickable(role = Role.Button, onClickLabel = "返回") {
                        if (state.currentIndex == 0) onBackFromFirst() else vm.back()
                    }
                    .padding(end = HrSpacing.md),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text("返回", style = HrType.button, color = HrColor.Ink)
            }
            Text(
                text = "${state.currentIndex + 1} / ${state.questions.size}",
                style = HrType.labelSmall,
                color = HrColor.WeakText,
                modifier = Modifier.testTag(UiTag.P01_SCREENING_PROGRESS),
            )
        }

        // 进度条
        Box(
            Modifier
                .fillMaxWidth()
                .height(HrSize.progressTrack)
                .clip(HrRadius.capsuleShape)
                .background(HrColor.Line),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(state.progress)
                    .height(HrSize.progressTrack)
                    .clip(HrRadius.capsuleShape)
                    .background(HrColor.Brand),
            )
        }

        VSpace(HrSpacing.xl)

        Text(
            text = question.title,
            style = HrType.question,
            color = HrColor.Ink,
            modifier = Modifier.testTag(UiTag.P01_SCREENING_QUESTION),
        )

        VSpace(HrSpacing.xs)

        Text(question.hint, style = HrType.bodySmall, color = HrColor.InkSecondary)

        VSpace(HrSpacing.lg)

        question.options.forEach { (answer, label) ->
            OptionRow(
                label = label,
                selected = state.selected == answer,
                tag = when (answer) {
                    ScreeningAnswer.YES -> UiTag.P01_OPTION_YES
                    ScreeningAnswer.NO -> UiTag.P01_OPTION_NO
                    ScreeningAnswer.UNSURE -> UiTag.P01_OPTION_UNSURE
                },
                onClick = { vm.answer(question.index, answer) },
            )
            VSpace(HrSpacing.xs)
        }

        // 答完最后一题时，若触发分流则先告知（不阻断继续）
        if (state.isLast && state.canGoNext) {
            val screening = state.toScreening()
            if (screening.clinicalNoticeRequired) {
                VSpace(HrSpacing.sm)
                HrBanner(
                    text = "按你刚才的回答，建议先咨询医生或专业人员，再决定练习强度。",
                    tone = ChipTone.WARN,
                    tag = UiTag.P01_CLINICAL_NOTICE,
                )
            } else if (screening.suggestsProfessionalAssessment) {
                VSpace(HrSpacing.sm)
                HrBanner(
                    text = "建议先找专业人员评估一次，再跟着本应用练习。现在也可以继续。",
                    tone = ChipTone.NEUTRAL,
                    tag = UiTag.P01_CLINICAL_NOTICE,
                )
            }
        }

        VSpace(HrSpacing.lg)
    }
}

@Composable
private fun OptionRow(
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = HrSize.primaryButton)
            .clip(HrRadius.controlShape)
            .background(if (selected) HrColor.GoodContainer else HrColor.Surface)
            .border(
                1.dp,
                if (selected) HrColor.Good else HrColor.Line,
                HrRadius.controlShape,
            )
            .clickable(role = Role.RadioButton) { onClick() }
            .testTag(tag)
            .semantics {
                this.selected = selected
                contentDescription = label
            }
            .padding(horizontal = HrSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HrSpacing.sm),
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) HrColor.Good else HrColor.Surface)
                .border(2.dp, if (selected) HrColor.Good else HrColor.Track, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(HrColor.Surface),
                )
            }
        }
        Text(
            text = label,
            style = if (selected) HrType.bodyMediumBold else HrType.bodyMedium,
            color = if (selected) HrColor.GoodDeep else HrColor.Slate800,
        )
    }
}