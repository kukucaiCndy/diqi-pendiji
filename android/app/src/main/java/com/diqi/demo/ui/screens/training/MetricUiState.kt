package com.diqi.demo.ui.screens.training

import com.diqi.demo.data.db.MissingReason
import com.diqi.demo.data.db.QualityState

/**
 * 训练页指标区状态（FR-06 第四层 / FR-08 四态）。
 *
 * 首版这里是一个 `Boolean` 假开关（恒 false 且无赋值点）——
 * S6 一翻转就会显示没有数据支撑的状态。改成密封接口后，
 * 接入感知层时必须**显式**选择其中一种，编译期就能看出缺哪一种。
 *
 * ## 为什么数值字段是 `Float?` 而不是 `Float`
 *
 * 需求禁止"无数据时显示数字"。用可空类型把这条约束交给类型系统：
 * 想显示数字就必须先证明它有值。
 */
sealed interface MetricUiState {

    /** 未启用感知：整块不显示，只留一行「未启用感知」（FR-06 原文）*/
    data object NotEnabled : MetricUiState

    /** 已启用但信号尚未建立（前 30 秒）。**不得显示任何数字**（AC-15）。*/
    data class Establishing(val elapsedSec: Int) : MetricUiState

    /**
     * NO_DATA：未授权 / SDK 不可用 / 离框 > 3s。整块置灰，显示「信号暂失」。
     *
     * [banner] 是"该怎么办"的提示（例如"看不到脸了，请把手机放稳"）。
     * 首版没有这个字段，于是 QualityGate 产出的提示被丢弃，用户只知道出问题、
     * 不知道如何处理。
     */
    data class NoData(
        val reason: MissingReason,
        val banner: String? = null,
    ) : MetricUiState

    /** INVALID：有数据但不可信（SQI 低于门限）。显示「--」+「信号弱」。*/
    data class Invalid(val sqi: Float?) : MetricUiState

    /** DEGRADED：部分质量下降。只显示仍可靠的项，标注「信号一般」。*/
    data class Degraded(
        val heartRateBpm: Float?,
        val breathingRatePerMin: Float?,
        val sqi: Float?,
        val banner: String?,
    ) : MetricUiState

    /** VALID：显示真实指标。*/
    data class Valid(
        val heartRateBpm: Float?,
        val breathingRatePerMin: Float?,
        val sqi: Float?,
        val dimmed: Boolean,
    ) : MetricUiState

    /** 质量三点指示数量 0–3 */
    val qualityDots: Int
        get() = when (this) {
            is Valid -> 3
            is Degraded -> 2
            is Invalid -> 1
            else -> 0
        }

    /** 是否允许显示数字。**false 时 UI 不得渲染任何数值。** */
    val showsNumbers: Boolean
        get() = this is Valid || this is Degraded

    val state: QualityState
        get() = when (this) {
            is Valid -> QualityState.VALID
            is Degraded -> QualityState.DEGRADED
            is Invalid -> QualityState.INVALID
            else -> QualityState.NO_DATA
        }
}
