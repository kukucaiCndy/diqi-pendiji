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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
 * 内容可滚动、**主操作固定在底部**的页面骨架。
 *
 * 为什么需要它（AC-09 的直接后果）：372dp 宽 + 正文 20sp + 系统字号 2.0× 时，
 * 「开始」这类主按钮如果跟着内容一起滚，就会落到屏幕外 ——
 * 而「一屏一个主任务」要求用户不需要先滚过三段说明才能找到按钮。
 *
 * 结构：
 * ```
 * ┌──────────────┐
 * │ 可滚动内容    │ ← weight(1f)
 * ├──────────────┤
 * │ 固定主操作    │
 * └──────────────┘
 * ```
 */
@Composable
fun HrPageWithAction(
    modifier: Modifier = Modifier,
    tag: String? = null,
    background: Color = HrColor.Canvas,
    action: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = HrSpacing.pageHorizontal)
                .let { if (tag != null) it.then(Modifier.testTag(tag)) else it },
            content = content,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(background)
                .padding(
                    start = HrSpacing.pageHorizontal,
                    end = HrSpacing.pageHorizontal,
                    top = HrSpacing.xs,
                    bottom = HrSpacing.sm,
                ),
            content = action,
        )
    }
}

/**
 * 与 [HrPageWithAction] 相同，但内容用 **LazyColumn**。
 *
 * ## 为什么需要这个变体（S9 性能实测的直接后果）
 *
 * `Column` + `verticalScroll` 的测量策略是"每次测量都把所有子项重新测一遍"。
 * 训练页里每秒变化的内容（倒计时/圆环）就住在这样一个 Column 里，
 * 于是**每推一帧，整块滚动内容（视频区 + 时间轴 + 圆环 + 指标区 + 免责）
 * 全部重新测量**。实测 p90 48ms，远超 16.7ms 的帧预算。
 *
 * `LazyColumn` 是按 item 增量测量的：某一项失效只重测那一项。
 * 把各区块拆成 `item { }` 之后，推帧只影响倒计时/圆环那一项。
 */
@Composable
fun HrPageWithActionLazy(
    modifier: Modifier = Modifier,
    tag: String? = null,
    background: Color = HrColor.Canvas,
    action: @Composable ColumnScope.() -> Unit,
    content: LazyListScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .let { if (tag != null) it.then(Modifier.testTag(tag)) else it },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = HrSpacing.pageHorizontal,
            ),
            content = content,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(background)
                .padding(
                    start = HrSpacing.pageHorizontal,
                    end = HrSpacing.pageHorizontal,
                    top = HrSpacing.xs,
                    bottom = HrSpacing.sm,
                ),
            content = action,
        )
    }
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