package com.diqi.demo.work

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * [ReminderWorker.nextTriggerDelayMillis] 纯函数单测（D2）。
 *
 * 四个边界：
 * - 今天未到目标时刻
 * - 今天已过目标时刻
 * - 恰等于现在（取明天）
 * - 跨午夜
 */
class ReminderWorkerDelayTest {

    // 固定时区，避免测试因跑在不同时区机器上而漂移
    private val zone = ZoneId.of("Asia/Shanghai")

    /** 构造一个固定时刻的 epochMillis */
    private fun millisAt(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone)
            .toInstant().toEpochMilli()
    }

    @Test
    fun 今天未到目标时刻_延迟为今天剩余时间() {
        // 现在 10:00，目标 20:00 → 延迟 10 小时
        val now = millisAt(2026, 3, 15, 10, 0)
        val delay = ReminderWorker.nextTriggerDelayMillis(now, "20:00", zone)

        assertEquals("应延迟到今天 20:00（10 小时）", 10 * 3600_000L, delay)
    }

    @Test
    fun 今天已过目标时刻_延迟到明天() {
        // 现在 22:00，目标 20:00 → 延迟到明天 20:00（22 小时）
        val now = millisAt(2026, 3, 15, 22, 0)
        val delay = ReminderWorker.nextTriggerDelayMillis(now, "20:00", zone)

        assertEquals("应延迟到明天 20:00（22 小时）", 22 * 3600_000L, delay)
    }

    @Test
    fun 恰好等于现在_取明天不立刻触发() {
        // 现在 20:00，目标 20:00 → 恰好等于，取明天（24 小时）
        val now = millisAt(2026, 3, 15, 20, 0)
        val delay = ReminderWorker.nextTriggerDelayMillis(now, "20:00", zone)

        assertTrue("恰好等于现在时延迟应大于 0（取明天）", delay > 0)
        assertEquals("应延迟到明天 20:00（24 小时）", 24 * 3600_000L, delay)
    }

    @Test
    fun 跨午夜_今天23点目标明天6点() {
        // 现在 23:00，目标 06:00 → 延迟到明天 6:00（7 小时）
        val now = millisAt(2026, 3, 15, 23, 0)
        val delay = ReminderWorker.nextTriggerDelayMillis(now, "06:00", zone)

        assertEquals("应延迟到明天 06:00（7 小时）", 7 * 3600_000L, delay)
    }
}
