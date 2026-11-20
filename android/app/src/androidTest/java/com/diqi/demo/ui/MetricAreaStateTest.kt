package com.diqi.demo.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.diqi.demo.data.db.MissingReason
import com.diqi.demo.ui.screens.training.MetricArea
import com.diqi.demo.ui.screens.training.MetricUiState
import com.diqi.demo.ui.theme.HerRhythmTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 指标区四态渲染测试（FR-06 第四层 / FR-08 / AC-15 / AC-16 / AC-17）。
 *
 * ## 为什么放在 instrumented 而不是继续用 adb 点击
 *
 * 这四条 AC 的判据都是"界面显示了什么"。用 adb 点击验证时，
 * 脚本反复被中断恢复弹窗和 `BackHandler` 打断（按返回 → 二次确认 → 再按退出应用），
 * 实测拿不到稳定证据。
 *
 * 直接渲染 `MetricArea` 的好处：
 * 1. **不受导航与弹窗影响**，每次结果确定；
 * 2. 能进 CI，改动指标区时会自动回归；
 * 3. 断言的是"哪个节点出现了"，比截图判读可判定。
 *
 * 它替代的是"界面呈现"的验证，`QualityGate` 的判定规则仍由
 * `QualityGateTest` 的 14 项纯函数测试覆盖 —— 两层各管一段。
 */
@RunWith(AndroidJUnit4::class)
class MetricAreaStateTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(state: MetricUiState, demo: Boolean = false) {
        composeRule.setContent {
            HerRhythmTheme { MetricArea(metric = state, demoMode = demo) }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun 未启用感知时只显示一行提示不显示任何数字() {
        render(MetricUiState.NotEnabled)
        composeRule.onNodeWithText("未启用感知").assertIsDisplayed()
        composeRule.onNodeWithTag(UiTag.P04_METRIC_PLACEHOLDER).assertIsDisplayed()
    }

    @Test
    fun 建立信号时显示进度文案且不出现心率数值() {
        // AC-15：前 30 秒不得出现任何数字指标
        render(MetricUiState.Establishing(elapsedSec = 12))
        composeRule.onNodeWithText("信号建立中").assertIsDisplayed()
        composeRule.onNodeWithText("正在建立信号 12s / 约 30s，请把手机放稳。").assertIsDisplayed()
    }

    @Test
    fun 信号暂失时显示可解释文案且节律照常说明() {
        // AC-16：遮脸 ≥3s → NO_DATA
        render(MetricUiState.NoData(MissingReason.FACE_LOST))
        composeRule.onNodeWithText("信号暂失").assertIsDisplayed()
        composeRule.onNodeWithText("节律提示照常进行，不影响练习。").assertIsDisplayed()
    }

    @Test
    fun 信号弱时显示双横线而不是数字() {
        // AC-16 的 INVALID 分支：有数据但不可信，显示「--」+「信号弱」
        render(MetricUiState.Invalid(sqi = 0.2f))
        composeRule.onNodeWithText("信号弱").assertIsDisplayed()
        composeRule.onNodeWithText("--").assertIsDisplayed()
    }

    @Test
    fun 光照不足时标注信号一般并保留可靠数值() {
        // AC-17：降为 DEGRADED，但仍显示可靠项
        render(
            MetricUiState.Degraded(
                heartRateBpm = 72f,
                breathingRatePerMin = null,
                sqi = 0.7f,
                banner = "光线不足，可以把手机换个方向",
            ),
        )
        composeRule.onNodeWithText("信号一般").assertIsDisplayed()
        composeRule.onNodeWithText("72").assertIsDisplayed()
        composeRule.onNodeWithText("光线不足，可以把手机换个方向").assertIsDisplayed()
    }

    @Test
    fun 有效时显示心率与呼吸率缺失时的横线() {
        render(
            MetricUiState.Valid(
                heartRateBpm = 72f,
                breathingRatePerMin = null,
                sqi = 0.85f,
                dimmed = false,
            ),
        )
        composeRule.onNodeWithText("信号良好").assertIsDisplayed()
        composeRule.onNodeWithText("72").assertIsDisplayed()
        // 呼吸率未满 60 秒：必须是「--」，不能是编造的数字
        composeRule.onNodeWithText("--").assertIsDisplayed()
    }

    @Test
    fun 演示模式下必须出现水印() {
        // AC-19：演示数据必须有全屏水印
        render(
            MetricUiState.Valid(72f, 16f, 0.85f, dimmed = false),
            demo = true,
        )
        composeRule.onNodeWithTag(UiTag.P04_DEMO_WATERMARK).assertIsDisplayed()
        composeRule.onNodeWithText("模拟数据 · 仅用于演示").assertIsDisplayed()
    }

    @Test
    fun 非演示模式下不得出现水印() {
        render(MetricUiState.Valid(72f, 16f, 0.85f, dimmed = false), demo = false)
        composeRule.onAllNodes(androidx.compose.ui.test.hasTestTag(UiTag.P04_DEMO_WATERMARK))
            .fetchSemanticsNodes()
            .let { nodes ->
                assert(nodes.isEmpty()) { "非演示模式不应出现演示水印" }
            }
    }
}
