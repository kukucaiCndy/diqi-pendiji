package com.diqi.demo.data.db

/**
 * 训练会话的感知模式。
 *
 * 为什么用枚举 + 显式字符串而不是布尔：需求 §6 要求 `sensorMode` 取
 * `offline_sdk` / `null` / `simulated` 三值，且**统计查询必须默认过滤 simulated**
 * （FR-14 / AC-19）。用枚举能让"忘记过滤"变成编译期可见的问题，
 * 用散落的字符串则不能。
 *
 * 注意 `NULL` 这个名字与 Kotlin 的空值语义冲突，故常量名取 [OFF]，
 * 但**持久化字符串仍是规范要求的 "null"**（对外契约不因为我们内部命名而改变）。
 */
enum class SensorMode(val storageValue: String) {
    /** 真实端侧 SDK */
    OFFLINE_SDK("offline_sdk"),
    /** 未授权 / SDK 不可用 / 用户选择不带摄像头 */
    OFF("null"),
    /** 演示模式（AC-19：不计入打卡、不出现在周报） */
    SIMULATED("simulated");

    val isSimulated: Boolean get() = this == SIMULATED

    companion object {
        fun from(value: String?): SensorMode =
            entries.firstOrNull { it.storageValue == value } ?: OFF
    }
}

/** 感知质量四态（FR-08）。 */
enum class QualityState(val storageValue: String) {
    VALID("VALID"),
    DEGRADED("DEGRADED"),
    INVALID("INVALID"),
    NO_DATA("NO_DATA");

    companion object {
        fun from(value: String?): QualityState =
            entries.firstOrNull { it.storageValue == value } ?: NO_DATA
    }
}

/** 无数据的原因（FR-08 要求可解释）。 */
enum class MissingReason(val storageValue: String) {
    NONE("none"),
    FACE_LOST("face_lost"),
    LOW_LIGHT("low_light"),
    TIMEOUT("timeout"),
    SDK_FAILURE("sdk_failure"),
    NOT_ENABLED("not_enabled");

    companion object {
        fun from(value: String?): MissingReason =
            entries.firstOrNull { it.storageValue == value } ?: NONE
    }
}