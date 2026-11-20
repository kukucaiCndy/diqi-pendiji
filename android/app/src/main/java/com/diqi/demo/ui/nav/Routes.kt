package com.diqi.demo.ui.nav

import com.diqi.demo.ui.UiTag

/** 路由常量。集中一处，避免各页各写字符串导致拼写不一致。 */
object Routes {
    const val ONBOARDING_WELCOME = "onboarding/welcome"
    const val ONBOARDING_SCREENING = "onboarding/screening"

    const val HOME = "home"
    const val RECORDS = "records"
    const val PROFILE = "profile"

    /** 训练准备（S5）*/
    const val TRAINING_PREP = "training/prep"

    /** 训练中：**顶级路由**，不在底部 Tab 图里（切 Tab 会 detach 相机，决策 D13）*/
    const val TRAINING_LIVE = "training/live"

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

    /** 首页 / 记录 / 我的 三个 Tab 的路由集合（决定底栏是否显示）*/
    val TAB_ROUTES = setOf(HOME, RECORDS, PROFILE, TRAINING_PREP)
}

/** 底部 Tab 定义。顺序与需求 §5.1「首页 / 训练 / 记录 / 我的」一致。 */
enum class BottomTab(
    val route: String,
    val label: String,
    val tag: String,
) {
    HOME(Routes.HOME, "首页", UiTag.TAB_HOME),
    TRAINING(Routes.TRAINING_PREP, "训练", UiTag.TAB_TRAINING),
    RECORDS(Routes.RECORDS, "记录", UiTag.TAB_RECORDS),
    PROFILE(Routes.PROFILE, "我的", UiTag.TAB_PROFILE),
}