package com.diqi.demo.ui.screens.training

/**
 * 训练页指标区状态（FR-06 第四层 / FR-08 四态）。
 *
 * 首版这里是一个 `Boolean` 假开关，恒为 false 且全工程无赋值点 ——
 * S6 一翻转就会显示没有数据支撑的状态。改成枚举后，
 * 接入感知层时必须**显式**选择其中一种，编译期就能看出缺哪一种。
 *
 * S6 需补齐：Valid(hr, br, sqi) / Degraded(...) / Invalid(...) / NoData(reason) / Establishing。
 */
sealed interface MetricUiState {
    /** 未启用感知：整块不显示，只留一行「未启用感知」（FR-06 原文）*/
    data object NotEnabled : MetricUiState

    /** 已启用但信号尚未建立（前 30 秒）。不得显示任何数字（AC-15）。 */
    data object Establishing : MetricUiState
}
