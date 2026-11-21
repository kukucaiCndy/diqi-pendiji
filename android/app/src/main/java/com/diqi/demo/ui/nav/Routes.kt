package com.diqi.demo.ui.nav

import com.diqi.demo.ui.UiTag

/** 路由常量。集中一处，避免各页各写字符串导致拼写不一致。 */
object Routes {
    const val ONBOARDING_WELCOME = "onboarding/welcome"
    const val ONBOARDING_SCREENING = "onboarding/screening"

    /**
     * 训练 Tab 的落地页（**合并了原「首页」与「训练」**）。
     *
     * 产品决定：首页与训练合并为一个「训练」Tab，底栏从 4 个减为 3 个
     * （训练 / 记录 / 我的）。设计稿未覆盖这个信息架构，故此处为**新设计**，
     * 决策与理由见 `docs/INTERACTION-DESIGN.md`。
     */
    const val TRAINING_HOME = "training/home"

    const val RECORDS = "records"
    const val PROFILE = "profile"

    /**
     * 训练中：**顶级路由**，不在底部 Tab 图里（切 Tab 会 detach 相机，决策 D13）。
     *
     * `camera` 参数表示这次训练是否启用摄像头 —— 用户在训练 Tab 上就能选，
     * 不需要先进某个设置页（见 INTERACTION-DESIGN.md §2）。
     */
    const val TRAINING_LIVE = "training/live?camera={camera}"

    /** 构造带参数的实际路由 */
    fun trainingLive(camera: Boolean): String = "training/live?camera=$camera"

    /** 摄像头校准（S6）*/
    const val CALIBRATE = "training/calibrate"

    /** 完成与自评（S7）*/
    const val COMPLETE = "training/complete"

    /** 家人周报（S8）*/
    const val WEEKLY_REPORT = "records/weekly"

    /** 漏尿日记（S7）*/
    const val LEAK_DIARY = "records/leak"

    /** 设计系统与语音自检（仅 debug）*/
    const val DEV_DESIGN = "dev/design"

    /** 三个 Tab 的路由集合（决定底栏是否显示）*/
    val TAB_ROUTES = setOf(TRAINING_HOME, RECORDS, PROFILE)
}

/**
 * 底部 Tab 定义：**3 个**（训练 / 记录 / 我的）。
 *
 * 这是对需求 §5.1「底部 4 Tab（首页 / 训练 / 记录 / 我的）」的**主动变更**：
 * 产品决定把「首页」与「训练」合并为一个「训练」Tab。
 * 变更依据与交互设计见 `docs/INTERACTION-DESIGN.md`。
 *
 * 顺带解决了一个原有矛盾：旧的「训练」Tab 指向准备页，而准备页主按钮又跳去训练页，
 * 中间的「首页」其实承担了训练入口 —— 两个 Tab 的职责是重叠的。
 */
enum class BottomTab(
    val route: String,
    val label: String,
    val tag: String,
) {
    TRAINING(Routes.TRAINING_HOME, "训练", UiTag.TAB_TRAINING),
    RECORDS(Routes.RECORDS, "记录", UiTag.TAB_RECORDS),
    PROFILE(Routes.PROFILE, "我的", UiTag.TAB_PROFILE),
}