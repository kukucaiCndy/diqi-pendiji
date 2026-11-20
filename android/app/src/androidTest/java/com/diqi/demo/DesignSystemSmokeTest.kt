package com.diqi.demo

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented 冒烟测试（首个，用于证明仪器化测试通路可用）。
 *
 * 为什么 S1 就要有：评审指出"零 instrumented test"，而 AC-08（连点竞态）、
 * AC-16（3 秒判据）、AC-02（≤100ms）这三类判据**纯 adb 物理上做不到**
 * （uiautomator dump 每次耗时 1–2s）。等 S7 才第一次跑 instrumented，
 * 会同时面对新功能 + 新测试框架两类风险。
 *
 * 现在只验证：
 *   1. 仪器化环境能启动 app 并与 Compose 树交互
 *   2. S1 自检页的测试锚点可被定位（这是全部 E2E 定位的基础）
 */
@RunWith(AndroidJUnit4::class)
class DesignSystemSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun 设计系统自检页可渲染且测试锚点存在() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dev.design_system.root").assertExists()
    }

    @Test
    fun 页面关键文案可被定位() {
        composeRule.waitForIdle()
        composeRule.onNodeWithText("HerRhythm DS v1.0").assertExists()
    }
}
