package com.facephys.rppg

import com.stepmini.rppg.HrvResult as SdkHrvResult
import com.stepmini.rppg.MetricsResult as SdkMetricsResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RealRppgEngine 纯函数映射的 JVM 单测。
 *
 * 测的是 [mapInitCode]、[mapMetrics]、[validateRoi]、[validateDtSec]、
 * [parseLicenseDaysLeft] —— 这些函数不依赖 Android 环境，可在纯 JVM 跑。
 *
 * 覆盖要点：
 *  · mapInitCode 全码表含 -16/-17/-18
 *  · metrics 三态映射（hr null / sqi null / valid false 呼吸率 null）
 *  · ROI 长度校验
 *  · dtSec 校验
 *  · 串行守卫（feeding CAS 逻辑验证，通过 StubRppgEngine 间接验证契约一致）
 */
class RealRppgEngineMappersTest {

    // ── mapInitCode 全码表 ────────────────────────────────────────────────

    @Test
    fun `mapInitCode 0 映射为 OK`() {
        assertEquals(InitErrorCode.OK, mapInitCode(0))
    }

    @Test
    fun `mapInitCode -3 BAD_MODEL 映射为 MODEL_MISSING`() {
        assertEquals(InitErrorCode.MODEL_MISSING, mapInitCode(-3))
    }

    @Test
    fun `mapInitCode -20 ASSETS 映射为 MODEL_MISSING`() {
        assertEquals(InitErrorCode.MODEL_MISSING, mapInitCode(-20))
    }

    @Test
    fun `mapInitCode -10 LICENSE 映射为 NOT_AUTHORIZED`() {
        assertEquals(InitErrorCode.NOT_AUTHORIZED, mapInitCode(-10))
    }

    @Test
    fun `mapInitCode -11 LICENSE_EXPIRED 映射为 NOT_AUTHORIZED`() {
        assertEquals(InitErrorCode.NOT_AUTHORIZED, mapInitCode(-11))
    }

    @Test
    fun `mapInitCode -12 LICENSE_INVALID 映射为 NOT_ACTIVATED`() {
        assertEquals(InitErrorCode.NOT_ACTIVATED, mapInitCode(-12))
    }

    @Test
    fun `mapInitCode -13 LICENSE_NETWORK 映射为 NO_NETWORK`() {
        assertEquals(InitErrorCode.NO_NETWORK, mapInitCode(-13))
    }

    @Test
    fun `mapInitCode -14 QUOTA_EXHAUSTED 映射为 QUOTA_EXCEEDED`() {
        assertEquals(InitErrorCode.QUOTA_EXCEEDED, mapInitCode(-14))
    }

    @Test
    fun `mapInitCode -16 SESSION_TIMEOUT 映射为 SESSION_TIMEOUT`() {
        assertEquals(InitErrorCode.SESSION_TIMEOUT, mapInitCode(-16))
    }

    @Test
    fun `mapInitCode -17 BANNED 映射为 BANNED`() {
        assertEquals(InitErrorCode.BANNED, mapInitCode(-17))
    }

    @Test
    fun `mapInitCode -18 CLOCK_ROLLBACK 映射为 CLOCK_ROLLBACK`() {
        assertEquals(InitErrorCode.CLOCK_ROLLBACK, mapInitCode(-18))
    }

    @Test
    fun `mapInitCode 未知码映射为 UNKNOWN`() {
        assertEquals(InitErrorCode.UNKNOWN, mapInitCode(-999))
        assertEquals(InitErrorCode.UNKNOWN, mapInitCode(99))
    }

    @Test
    fun `mapInitCode 全码表无遗漏`() {
        // 已知 SDK 错误码全部覆盖
        val knownCodes = listOf(0, -3, -20, -10, -11, -12, -13, -14, -16, -17, -18)
        knownCodes.forEach { code ->
            val mapped = mapInitCode(code)
            assertTrue("码 $code 不应映射为 UNKNOWN（已知码表）", mapped != InitErrorCode.UNKNOWN)
        }
    }

    // ── mapMetrics 三态映射 ──────────────────────────────────────────────

    private fun metrics(
        hr: Float? = null,
        sqi: Float? = null,
        hasEnoughWindow: Boolean = false,
    ): SdkMetricsResult = SdkMetricsResult(
        hr, sqi, 0, 0f, hasEnoughWindow, 0, null, null,
    )

    private fun hrv(
        rrBpm: Float? = null,
        valid: Boolean = true,
    ): SdkHrvResult = SdkHrvResult(
        rrBpm, null, null, 0, valid, 0,
        0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f,
    )

    @Test
    fun `窗口未攒够时映射为 NotReady`() {
        val m = metrics(hr = 72f, sqi = 0.8f, hasEnoughWindow = false)
        assertEquals(MetricsRead.NotReady, mapMetrics(m, null, 1000L))
    }

    @Test
    fun `窗口够了但 hr 为 null 时映射为 NoSignal`() {
        val m = metrics(hr = null, sqi = null, hasEnoughWindow = true)
        assertEquals(MetricsRead.NoSignal, mapMetrics(m, null, 1000L))
    }

    @Test
    fun `窗口够了且 hr 有值但 hrv 为 null 时映射为 Ok 且呼吸率为 null`() {
        val m = metrics(hr = 72f, sqi = 0.85f, hasEnoughWindow = true)
        val result = mapMetrics(m, null, 1000L)
        assertTrue(result is MetricsRead.Ok)
        val ok = result as MetricsRead.Ok
        assertEquals(72f, ok.metrics.heartRateBpm)
        assertEquals(0.85f, ok.metrics.sqi)
        assertNull("hrv 为 null 时呼吸率应为 null", ok.metrics.breathingRatePerMin)
        assertEquals(1000L, ok.metrics.timestampMs)
    }

    @Test
    fun `窗口够了且 hrv valid=true 时呼吸率取 rrBpm`() {
        val m = metrics(hr = 72f, sqi = 0.85f, hasEnoughWindow = true)
        val h = hrv(rrBpm = 16f, valid = true)
        val result = mapMetrics(m, h, 1000L)
        assertTrue(result is MetricsRead.Ok)
        val ok = result as MetricsRead.Ok
        assertEquals(16f, ok.metrics.breathingRatePerMin)
    }

    @Test
    fun `hrv valid=false 时呼吸率为 null 即使 rrBpm 有值`() {
        val m = metrics(hr = 72f, sqi = 0.85f, hasEnoughWindow = true)
        val h = hrv(rrBpm = 16f, valid = false)
        val result = mapMetrics(m, h, 1000L)
        assertTrue(result is MetricsRead.Ok)
        val ok = result as MetricsRead.Ok
        assertEquals(72f, ok.metrics.heartRateBpm)
        assertNull("valid=false 时呼吸率应为 null", ok.metrics.breathingRatePerMin)
    }

    @Test
    fun `sqi 为 null 时 Ok 载荷的 sqi 也为 null`() {
        val m = metrics(hr = 72f, sqi = null, hasEnoughWindow = true)
        val result = mapMetrics(m, null, 1000L)
        assertTrue(result is MetricsRead.Ok)
        val ok = result as MetricsRead.Ok
        assertEquals(72f, ok.metrics.heartRateBpm)
        assertNull(ok.metrics.sqi)
    }

    // ── ROI 长度校验 ─────────────────────────────────────────────────────

    @Test
    fun `ROI 长度 243 通过校验`() {
        assertEquals(FrameResult.OK, validateRoi(ByteArray(243)))
    }

    @Test
    fun `ROI 长度 242 被拒绝`() {
        assertEquals(FrameResult.ERR_BAD_ROI_SIZE, validateRoi(ByteArray(242)))
    }

    @Test
    fun `ROI 长度 244 被拒绝`() {
        assertEquals(FrameResult.ERR_BAD_ROI_SIZE, validateRoi(ByteArray(244)))
    }

    @Test
    fun `ROI 长度 0 被拒绝`() {
        assertEquals(FrameResult.ERR_BAD_ROI_SIZE, validateRoi(ByteArray(0)))
    }

    // ── dtSec 校验 ──────────────────────────────────────────────────────

    @Test
    fun `dtSec 正常值通过校验`() {
        assertEquals(FrameResult.OK, validateDtSec(0.083f))
        assertEquals(FrameResult.OK, validateDtSec(0.1f))
    }

    @Test
    fun `dtSec 为 0 被拒绝`() {
        assertEquals(FrameResult.ERR_BAD_DT, validateDtSec(0f))
    }

    @Test
    fun `dtSec 为负数被拒绝`() {
        assertEquals(FrameResult.ERR_BAD_DT, validateDtSec(-0.1f))
    }

    @Test
    fun `dtSec 为 NaN 被拒绝`() {
        assertEquals(FrameResult.ERR_BAD_DT, validateDtSec(Float.NaN))
    }

    @Test
    fun `dtSec 超过上限被拒绝`() {
        assertEquals(FrameResult.ERR_BAD_DT, validateDtSec(RppgEngine.MAX_DT_SEC + 0.1f))
    }

    // ── 串行守卫 ─────────────────────────────────────────────────────────
    // 串行守卫逻辑与 StubRppgEngine 一致（CAS），通过 StubRppgEngine 验证契约一致性

    @Test
    fun `并发投喂被检测为 ERR_CONCURRENT（串行守卫契约一致）`() {
        val engine = StubRppgEngine()
        engine.initialize(RppgConfig(context = null, encAssetName = "m", appKey = "k")) { }
        val errors = java.util.concurrent.ConcurrentLinkedQueue<FrameResult>()
        val threads = (1..8).map {
            Thread {
                repeat(200) { errors.add(engine.processFrame(ByteArray(243), 0.083f)) }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        assertTrue(
            "并发投喂应至少被拒绝一次",
            errors.any { it == FrameResult.ERR_CONCURRENT },
        )
    }

    // ── parseLicenseDaysLeft ────────────────────────────────────────────

    @Test
    fun `expireAtFormatted 为 null 时返回 null`() {
        assertNull(parseLicenseDaysLeft(null))
    }

    @Test
    fun `expireAtFormatted 为空串时返回 null`() {
        assertNull(parseLicenseDaysLeft(""))
    }

    @Test
    fun `expireAtFormatted 为日期格式时返回天数`() {
        // 用固定 now 值测试：2025-06-01 距 2025-12-31 约 213 天
        val cal = java.util.Calendar.getInstance()
        cal.set(2025, 5, 1, 0, 0, 0)
        val now = cal.timeInMillis
        val days = parseLicenseDaysLeft("2025-12-31", now)
        assertTrue("应返回正天数", days != null && days > 0)
    }

    @Test
    fun `expireAtFormatted 含时间部分时也能解析日期`() {
        val cal = java.util.Calendar.getInstance()
        cal.set(2025, 5, 1, 0, 0, 0)
        val now = cal.timeInMillis
        val days = parseLicenseDaysLeft("2025/12/31 23:59:59", now)
        assertTrue(days != null && days > 0)
    }

    @Test
    fun `过期日期已过时返回 0`() {
        val cal = java.util.Calendar.getInstance()
        cal.set(2026, 0, 1, 0, 0, 0)
        val now = cal.timeInMillis
        val days = parseLicenseDaysLeft("2025-01-01", now)
        assertEquals(0, days)
    }

    @Test
    fun `无法解析的格式返回 null`() {
        assertNull(parseLicenseDaysLeft("invalid"))
    }

    @Test
    fun `真实 SDK 格式 yyyy-MM-dd 能解析出正天数`() {
        // 真机 logcat 抓到 expireAtFormatted=2026-12-11（yyyy-MM-dd，无时间部分）
        // 用固定 now=2025-11-21 验证：距 2026-12-11 约 385 天
        val cal = java.util.Calendar.getInstance()
        cal.set(2025, 10, 21, 0, 0, 0)
        val now = cal.timeInMillis
        val days = parseLicenseDaysLeft("2026-12-11", now)
        assertTrue("应返回正天数（约 385 天）", days != null && days > 380 && days < 390)
    }
}
