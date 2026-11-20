package com.diqi.demo

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.diqi.demo.ui.screens.dev.DesignSystemScreen
import com.diqi.demo.ui.theme.HerRhythmTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 设计系统自检页的仪器化冒烟测试。
 *
 * ## 为什么不通过 MainActivity 进这个页面
 *
 * 首版是 `createAndroidComposeRule<MainActivity>()` 然后断言设计系统页。
 * 但 MainActivity 的起始页现在取决于"筛查是否已确认" ——
 * 真机上跑过一次后数据残留，测试就开始随机失败。
 *
 * 改为**直接渲染目标 composable**：不依赖导航状态与持久化数据，
 * 测什么就渲染什么。这也是 UI 测试更该用的写法。
 */
@RunWith(AndroidJUnit4::class)
class DesignSystemSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun 设计系统自检页可渲染且测试锚点存在() {
        composeRule.setContent { HerRhythmTheme { DesignSystemScreen() } }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dev.design_system.root").assertExists()
    }

    @Test
    fun 页面关键文案可被定位() {
        composeRule.setContent { HerRhythmTheme { DesignSystemScreen() } }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("HerRhythm DS v1.0").assertExists()
    }
}
