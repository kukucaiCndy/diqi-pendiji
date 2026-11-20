package com.diqi.demo.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.diqi.demo.AppContainer
import com.diqi.demo.ui.screens.onboarding.OnboardingViewModel
import com.diqi.demo.ui.screens.onboarding.WelcomeScreen
import com.diqi.demo.ui.theme.HerRhythmTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * AC-10 的可访问性断言（Compose 语义层）。
 *
 * ## 为什么不用 uiautomator 的 `clickable` 属性做判据
 *
 * 实测发现：带 `testTag` 的节点在 `uiautomator dump` 里显示
 * `clickable=true` 但 `content-desc` 为空，而真正的朗读文本挂在
 * 它的**合并子节点**上 —— 直接按 dump 统计会得到"6 个可点节点全部缺描述"
 * 的假阴性。原因：uiautomator 反映的是扁平化后的 Android 无障碍树，
 * 而 Compose 的 `semantics(mergeDescendants = true)` 把语义合并到父节点，
 * 两者对"节点"的定义不一致。
 *
 * 所以 AC-10 的自动化判据放在 Compose 语义层。真机 TalkBack 流程无法验证
 * （本机没有任何 TalkBack 包），在验收记录里如实标注。
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val hasContentDescription =
        SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription)

    @Test
    fun 引导页主按钮带无障碍描述() {
        val container = AppContainer(ApplicationProvider.getApplicationContext())
        composeRule.setContent {
            HerRhythmTheme {
                WelcomeScreen(vm = OnboardingViewModel(container), onStart = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(UiTag.P01_PRIMARY_BUTTON)
            .assertExists()
            .assert(hasContentDescription)
    }

    @Test
    fun 首屏根节点存在() {
        val container = AppContainer(ApplicationProvider.getApplicationContext())
        composeRule.setContent {
            HerRhythmTheme {
                WelcomeScreen(vm = OnboardingViewModel(container), onStart = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(UiTag.P01_ROOT).assertExists()
    }
}
