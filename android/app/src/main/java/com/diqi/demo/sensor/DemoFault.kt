package com.diqi.demo.sensor

/**
 * 演示模式的**故障注入**开关。
 *
 * ## 它为什么存在
 *
 * AC-16（遮脸 3 秒 → 信号暂失）与 AC-17（强光/逆光 → 光照提示）在真机上
 * 需要人用手遮脸、拿灯照脸才能验。没有人在设备前时，这两条就只能靠
 * `QualityGate` 的纯函数单测 —— 那是"逻辑对"，不是"界面真的变了"。
 *
 * 这个注入器让演示源可以主动产出"人脸丢失 / 光照不足 / SQI 过低"三种快照，
 * 从而在**真机上**验证四态切换的界面表现。它只在演示模式下有意义，
 * 真实感知源（RppgSensor）完全不读它。
 *
 * 生产环境不会被设置（只有 debug 广播能改），所以留着它是安全的；
 * 刻意不放进 debug source set，是因为 DemoSensor 在 main 里要读它。
 */
object DemoFault {

    enum class Kind {
        NONE,
        /** 模拟持续遮脸：应判 NO_DATA「信号暂失」 */
        FACE_LOST,
        /** 模拟光线不足：应判 DEGRADED「信号一般」+ 光照提示 */
        LOW_LIGHT,
        /** 模拟 SQI 低于门限：应判 INVALID「--」+「信号弱」 */
        LOW_SQI,
    }

    @Volatile
    var current: Kind = Kind.NONE
}
