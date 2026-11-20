package com.facephys.rppg

/** 初始化失败分类。P03 的「失败原因必须可解释」即以此映射为用户可读文案。 */
enum class InitErrorCode {
    OK,
    /** 未授权 */
    NOT_AUTHORIZED,
    /** 未激活 / 缺少 AppKey */
    NOT_ACTIVATED,
    /** 包名与 AppKey 不匹配 */
    PACKAGE_MISMATCH,
    /** 无网络（首次激活需要联网） */
    NO_NETWORK,
    /** 配额已满 */
    QUOTA_EXCEEDED,
    /** 模型资源缺失或损坏 */
    MODEL_MISSING,
    /** 未知错误，必须兜底，避免崩溃 */
    UNKNOWN,
}

/**
 * 初始化结果。
 *
 * 注意 [message] 是**厂商侧诊断信息**，不是给用户看的文案。
 * 用户可读文案由 app 层根据 [code] 映射（FR-03「失败原因必须可解释」），
 * 不要把中文用户文案写进 SDK 契约层。
 */
data class InitResult(
    val success: Boolean,
    val code: InitErrorCode,
    val message: String,
) {
    companion object {
        fun ok(message: String = "ok") = InitResult(true, InitErrorCode.OK, message)
        fun fail(code: InitErrorCode, message: String) = InitResult(false, code, message)
    }
}

/**
 * 一次读数。
 *
 * 任一字段为 null 表示该通道当前不可用 —— 调用方必须按「不显示」处理，
 * 不得用 0 或占位符替代（FR-08）。
 */
data class RppgMetrics(
    val heartRateBpm: Float?,
    val breathingRatePerMin: Float?,
    val sqi: Float?,
    val timestampMs: Long,
)

/** HRV。本期不展示，仅内部验证链路是否打通。 */
data class HrvResult(
    val sdnnMs: Float?,
    val rmssdMs: Float?,
)

/**
 * 诊断信息，P03 诊断折叠区直读。
 *
 * [sdkVersion] 在桩实现里必须是能一眼看出"这是桩"的值，
 * 否则 P03 截图会被误读成"已集成真实 1.5.0"。
 */
data class SdkDiagnostics(
    val sdkVersion: String,
    val licenseDaysLeft: Int?,
    val initialized: Boolean,
    val modelAssetPresent: Boolean,
)