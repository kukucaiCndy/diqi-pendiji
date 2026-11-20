package com.diqi.demo.core.time

import android.os.SystemClock

/**
 * 时钟抽象。
 *
 * 训练引擎必须接收一个 [Clock] 而不是自己读系统时间 —— 否则无法单测，
 * 也无法在测试里制造"锁屏 30s""暂停 10s"这类场景。
 */
fun interface Clock {
    /** 单调时间，单位毫秒。 */
    fun nowMs(): Long
}

/**
 * 生产用单调时钟。
 *
 * 用 `SystemClock.elapsedRealtime()` 而不是 `System.currentTimeMillis()` 或
 * `uptimeMillis()`，原因有三（决策 D11）：
 *  · `currentTimeMillis` 会被用户改时间/NTP 校时影响 → 训练时长会跳变
 *  · `uptimeMillis` **不计入深睡** → 锁屏后计时会停住，AC-05 直接失败
 *  · `elapsedRealtime` 含深睡且单调 → 锁屏 30s 后回来，时间差正好是 30s
 */
object MonotonicClock : Clock {
    override fun nowMs(): Long = SystemClock.elapsedRealtime()
}

/**
 * 测试用可控时钟。
 *
 * 注意：`current` 是公开可变字段，故意如此 —— 测试里要能随时
 * `clock.current = 30_000` 制造"跳了 30 秒"，这是验证锁屏场景的关键能力。
 */
class TestClock(var current: Long = 0L) : Clock {
    override fun nowMs(): Long = current

    fun advance(millis: Long): Long {
        current += millis
        return current
    }
}