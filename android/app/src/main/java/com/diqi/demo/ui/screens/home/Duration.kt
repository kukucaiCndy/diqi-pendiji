package com.diqi.demo.ui.screens.home

/**
 * 时长文案：「125 秒」→「2 分 5 秒」。
 *
 * 原先定义在 HomeScreen.kt 里，重写首页时被一并删掉，导致
 * RecordsScreen 等处的 import 断裂 —— 抽到独立文件，避免再次被页面重写波及。
 *
 * 格式与设计稿一致：P02-b 的数据行是「3 分 12 秒」，P06 的累计时长是「4 分 10 秒」。
 */
fun formatMinutes(totalSec: Long): String {
    val safe = if (totalSec < 0) 0 else totalSec
    val m = safe / 60
    val s = safe % 60
    return when {
        m > 0 && s > 0 -> "$m 分 $s 秒"
        m > 0 -> "$m 分钟"
        else -> "$s 秒"
    }
}
