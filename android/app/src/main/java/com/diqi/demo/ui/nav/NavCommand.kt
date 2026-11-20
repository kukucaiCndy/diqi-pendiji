package com.diqi.demo.ui.nav

/**
 * 跨入口的导航指令。
 *
 * 为什么需要它：`singleTop` + `onNewIntent` 之后，通知点击、深链、冷启动
 * 三条路径都会产生"要去某个页面"的意图，如果各自直接调 NavController，
 * 就会出现重复入栈 / 冷启动时 NavHost 还没准备好 / 热启动时指令丢失。
 *
 * 统一做法：Activity 只把意图写进一个 `MutableStateFlow`，由 NavHost
 * 在 composition 内部消费并 `clear()`。这样冷热启动行为一致，
 * 也不会出现"第二套 NavHost"。
 */
enum class NavCommand {
    /** 回到训练页（训练中通知点击） */
    OPEN_TRAINING,
    /** 回到首页（训练结束通知点击） */
    OPEN_HOME,
}