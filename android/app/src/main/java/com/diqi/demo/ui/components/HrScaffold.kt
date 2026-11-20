package com.diqi.demo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.diqi.demo.ui.theme.HrColor
import com.diqi.demo.ui.theme.HrSpacing

/**
 * 页面骨架。
 *
 * 存在的理由：`enableEdgeToEdge()` 之后内容会顶到状态栏下面（S1 自检页首版
 * 就踩到了：标题被状态栏压在字上）。**所有页面必须走这里**，不要各自
 * 处理 insets，否则总会漏掉一页。
 *
 * 另外这是"372dp 窄屏 + 正文 20sp + 系统字号可能 2.0×"的直接后果：
 * 默认 [scrollable] = true。凡是要"一屏放下"的页面，先验证过 2.0×
 * 再关掉滚动。
 */
@Composable
fun HrPage(
    modifier: Modifier = Modifier,
    tag: String? = null,
    background: Color = HrColor.Canvas,
    scrollable: Boolean = true,
    horizontalPadding: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    var columnModifier = Modifier
        .fillMaxSize()
        .background(background)
        .windowInsetsPadding(WindowInsets.safeDrawing)

    if (horizontalPadding) {
        columnModifier = columnModifier
            .padding(horizontal = HrSpacing.pageHorizontal)
    }
    if (scrollable) {
        columnModifier = columnModifier.verticalScroll(rememberScrollState())
    }
    if (tag != null) {
        columnModifier = columnModifier.testTag(tag)
    }

    Column(modifier = modifier.then(columnModifier), content = content)
}

/**
 * 底部固定操作区（不随内容滚动）。
 *
 * 主按钮 ≥ 56dp 高、与安全区底部留白 —— 适老化要求手指能稳稳按到。
 */
@Composable
fun HrBottomBar(
    modifier: Modifier = Modifier,
    containerColor: Color = HrColor.Canvas,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(
                start = HrSpacing.pageHorizontal,
                end = HrSpacing.pageHorizontal,
                top = HrSpacing.xs,
                bottom = HrSpacing.sm,
            ),
        content = content,
    )
}