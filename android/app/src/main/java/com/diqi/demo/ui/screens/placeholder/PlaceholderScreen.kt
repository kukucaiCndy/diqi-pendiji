package com.diqi.demo.ui.screens.placeholder

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.diqi.demo.ui.components.HrCard
import com.diqi.demo.ui.components.HrPage
import com.diqi.demo.ui.components.VSpace
import com.diqi.demo.ui.theme.HrColor
import com.diqi.demo.ui.theme.HrSpacing
import com.diqi.demo.ui.theme.HrType

/**
 * 待建页面的占位。
 *
 * ## 为什么先放占位而不是"等做到再建"
 *
 * 导航骨架必须在 S4 就完整接通，否则底栏、返回栈、通知跳转这些
 * **跨页行为**无法验证 —— 而它们正是最容易在最后阶段集中爆炸的部分。
 * 占位页只显示"这一页在做什么、什么时候做"，既不假装有功能，
 * 也让导航可达性可以真机验证。
 */
@Composable
fun PlaceholderScreen(
    title: String,
    message: String,
    tag: String,
    modifier: Modifier = Modifier,
    extra: (@Composable () -> Unit)? = null,
) {
    HrPage(modifier = modifier, tag = tag) {
        Text(title, style = HrType.headline, color = HrColor.Ink)
        VSpace(HrSpacing.sm)
        HrCard {
            Text(message, style = HrType.bodyMedium, color = HrColor.InkSecondary)
        }
        VSpace(HrSpacing.md)
        Text(
            text = "该页面尚未实现",
            style = HrType.label,
            color = HrColor.Warn,
            modifier = Modifier.testTag("$tag.pending"),
        )
        if (extra != null) {
            VSpace(HrSpacing.md)
            extra()
        }
        Spacer(Modifier.height(HrSpacing.xxl))
    }
}