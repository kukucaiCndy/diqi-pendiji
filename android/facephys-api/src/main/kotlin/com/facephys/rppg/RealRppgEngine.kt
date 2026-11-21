package com.facephys.rppg

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.stepmini.rppg.FrameResult as SdkFrameResult
import com.stepmini.rppg.InitResult as SdkInitResult
import com.stepmini.rppg.MetricsResult as SdkMetricsResult
import com.stepmini.rppg.HrvResult as SdkHrvResult
import com.stepmini.rppg.RppgConfig as SdkRppgConfig
import com.stepmini.rppg.RppgEngineImpl
import com.stepmini.rppg.RppgErrorCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * **真实 FacePhys rPPG SDK v1.5.0 的适配层实现。**
 *
 * 本类包装 [RppgEngineImpl]（`com.stepmini.rppg.*`），将其 API 适配到
 * 本项目的 [RppgEngine] 契约。app 侧通过 [AppContainer] 工厂拿到实例，
 * 与 [StubRppgEngine] 共享同一契约接口。
 *
 * ## 职责边界（只接心率/呼吸率/SQI）
 *
 * 真实 SDK 提供 HR/SQI/呼吸率/HRV/SpO2/BP/Psych/FaceAttr/Liveness 等多种能力，
 * 本期仅接入：
 *   · 心率（hr）+ 信号质量（sqi）—— 来自 [getMetricsAsync]
 *   · 呼吸率（rrBpm）—— 来自 [getHrvAsync]（唯一来源，见接入教程第八步）
 *
 * 不接 FaceAttr / 情绪 / 活体 / 年龄 / 性别 / SpO2 / BP / Psych。
 *
 * ## 会话超时对策（-16 / 5 分钟上限）
 *
 * SDK 对单次会话有时间上限（默认 5 分钟），超时返回 `ERR_SESSION_TIMEOUT(-16)`。
 * 本类在轮询/投喂遇到 -16 时自动 `release() + initializeAsync()` 重建窗口，
 * 重试上限 3 次，超限上报 `SDK_UNAVAILABLE` 停止循环，不无限重连。
 *
 * ## 线程模型
 *
 *   · initialize 在 [Dispatchers.IO] 协程里跑 `initializeAsync`，回调归一化到调用方线程
 *     （与桩一致：锁外回调）
 *   · processFrame 同步调用 SDK（SDK 内部非阻塞），保留串行守卫
 *   · 轮询协程在 [Dispatchers.IO] 上每 2s 取一次 hr/sqi 快照，
 *     每 5 个周期（≈10s）取一次 HRV（呼吸率来源）。
 */
class RealRppgEngine : RppgEngine {

    private val engine = RppgEngineImpl()

    private val feeding = AtomicBoolean(false)
    private val lastFeedAtMs = AtomicLong(Long.MIN_VALUE)

    @Volatile
    private var initialized = false

    @Volatile
    private var released = false

    /** 缓存的指标快照（轮询协程写入，readMetrics 读取）。 */
    @Volatile
    private var metricsSnapshot: SdkMetricsResult? = null

    /** 缓存的 HRV 快照（呼吸率来源）。 */
    @Volatile
    private var hrvSnapshot: SdkHrvResult? = null

    /** 是否窗口已攒够（来自最新一次 processFrame / getMetrics 的 hasEnoughWindow）。 */
    @Volatile
    private var windowReady = false

    /** 会话超时重试计数。 */
    private val sessionRetryCount = java.util.concurrent.atomic.AtomicInteger(0)

    private var savedConfig: SdkRppgConfig? = null
    private var savedContractConfig: RppgConfig? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pollInFlight = AtomicBoolean(false)
    @Volatile
    private var pollJob: Job? = null

    // ── initialize ──────────────────────────────────────────────────────

    override fun initialize(config: RppgConfig, callback: (InitResult) -> Unit) {
        val ctx = config.context
        if (ctx == null) {
            callback(InitResult.fail(InitErrorCode.UNKNOWN, "context is null (real SDK requires non-null Context)"))
            return
        }

        val sdkConfig = SdkRppgConfig(
            context = ctx,
            modelEncAssetPath = MODEL_ENC_ASSET,
            stateEncAssetPath = STATE_ENC_ASSET,
            appKey = config.appKey,
            sdkInitUrl = SDK_INIT_URL,
        )
        savedConfig = sdkConfig
        savedContractConfig = config

        scope.launch {
            val result: InitResult = withContext(Dispatchers.IO) {
                runCatching {
                    val sdkResult = engine.initializeAsync(sdkConfig)
                    mapInitResult(sdkResult)
                }.getOrElse { e ->
                    InitResult.fail(InitErrorCode.UNKNOWN, "initialize exception: ${e.message}")
                }
            }
            if (result.success) {
                synchronized(lock) {
                    initialized = true
                    released = false
                    sessionRetryCount.set(0)
                }
                startPolling()
            }
            // 回调归一化：锁外触发，与桩的做法一致
            callback(result)
        }
    }

    // ── processFrame ────────────────────────────────────────────────────

    override fun processFrame(roi: ByteArray, dtSec: Float): FrameResult {
        // 并发检测：CAS，与桩一致
        if (!feeding.compareAndSet(false, true)) {
            return FrameResult.ERR_CONCURRENT
        }
        try {
            synchronized(lock) {
                if (released) return FrameResult.ERR_RELEASED
                if (!initialized) return FrameResult.ERR_NOT_INITIALIZED
                if (roi.size != RppgEngine.ROI_BYTE_COUNT) return FrameResult.ERR_BAD_ROI_SIZE
                if (!dtSec.isFinite() || dtSec <= 0f || dtSec > RppgEngine.MAX_DT_SEC) {
                    return FrameResult.ERR_BAD_DT
                }
            }
            // SDK processFrame 同步调用
            val sdkResult: SdkFrameResult = runCatching {
                engine.processFrame(roi, dtSec)
            }.getOrElse { e ->
                // SDK 原生异常兜底
                return FrameResult.ERR_UNKNOWN
            }

            lastFeedAtMs.set(SystemClock.elapsedRealtime())
            windowReady = sdkResult.hasEnoughWindow

            // 检测会话超时（SDK processFrame 也可能返回 errorCode=-16）
            if (sdkResult.errorCode == RppgErrorCode.ERR_SESSION_TIMEOUT) {
                handleSessionTimeout()
                return FrameResult.ERR_UNKNOWN
            }

            return if (sdkResult.accepted) FrameResult.OK else FrameResult.ERR_UNKNOWN
        } finally {
            feeding.set(false)
        }
    }

    // ── window ──────────────────────────────────────────────────────────

    override val hasEnoughWindow: Boolean
        get() = synchronized(lock) { initialized && !released && windowReady }

    override val windowAgeMs: Long
        get() {
            val last = lastFeedAtMs.get()
            if (last == Long.MIN_VALUE) return Long.MAX_VALUE
            return (SystemClock.elapsedRealtime() - last).coerceAtLeast(0L)
        }

    // ── readMetrics ─────────────────────────────────────────────────────

    override fun readMetrics(): MetricsRead = synchronized(lock) {
        if (!initialized || released) return MetricsRead.NotReady
        val snap = metricsSnapshot ?: return MetricsRead.NotReady
        mapMetrics(snap, hrvSnapshot, SystemClock.elapsedRealtime())
    }

    // ── HRV ─────────────────────────────────────────────────────────────

    override fun getHrvAsync(callback: (HrvResult?) -> Unit) {
        scope.launch {
            val result: HrvResult? = withContext(Dispatchers.IO) {
                runCatching {
                    val hrv = engine.getHrvAsync()
                    hrvSnapshot = hrv
                    if (hrv.valid) {
                        HrvResult(
                            sdnnMs = hrv.sdnnMs,
                            rmssdMs = hrv.rmssdMs,
                        )
                    } else null
                }.getOrElse { null }
            }
            callback(result)
        }
    }

    // ── reset / release ─────────────────────────────────────────────────

    override fun reset() {
        runCatching { engine.reset() }
        synchronized(lock) {
            lastFeedAtMs.set(Long.MIN_VALUE)
            windowReady = false
            metricsSnapshot = null
            hrvSnapshot = null
        }
    }

    override fun release() {
        stopPolling()
        synchronized(lock) {
            released = true
            initialized = false
            lastFeedAtMs.set(Long.MIN_VALUE)
            windowReady = false
            metricsSnapshot = null
            hrvSnapshot = null
        }
        runCatching { engine.release() }
    }

    // ── diagnostics ─────────────────────────────────────────────────────

    override fun diagnostics(): SdkDiagnostics = synchronized(lock) {
        val ctx = savedContractConfig?.context
        val assetsPresent = ctx != null && assetsPresent(ctx, MODEL_ENC_ASSET) && assetsPresent(ctx, STATE_ENC_ASSET)
        SdkDiagnostics(
            sdkVersion = "rppg-sdk-1.5.0",
            licenseDaysLeft = licenseDaysLeftCache,
            initialized = initialized && !released,
            modelAssetPresent = assetsPresent,
        )
    }

    // ── 轮询协程 ────────────────────────────────────────────────────────

    /**
     * 初始化成功后启动轮询：每 2s 取一次 hr/sqi（getMetricsAsync）。
     *
     * 呼吸率来源：每 [BREATHING_POLL_EVERY_CYCLES] 个周期（≈10s，接入教程建议）调一次
     * [getHrvAsync] 更新 [hrvSnapshot]。`mapMetrics` 用 hrvSnapshot 门控呼吸率
     * （valid=false 也存快照，让 mapMetrics 据此把 rrBpm 给 null）。
     * 保持 [pollInFlight] 守卫：HRV 请求不在 pollInFlight 临界区内，
     * 但用 runCatching 包裹，失败不中断轮询循环。
     *
     * UI 以 1s 刷新读缓存快照，不在 UI 线程发网络/native 调用。
     */
    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            var cycle = 0
            while (true) {
                if (!pollInFlight.compareAndSet(false, true)) {
                    kotlinx.coroutines.delay(POLL_INTERVAL_MS)
                    continue
                }
                try {
                    runCatching {
                        val metrics = engine.getMetricsAsync()
                        metricsSnapshot = metrics
                        windowReady = metrics.hasEnoughWindow

                        // 会话超时检测
                        if (metrics.errorCode == RppgErrorCode.ERR_SESSION_TIMEOUT) {
                            handleSessionTimeout()
                        }

                        // licenseDaysLeft：queryQuota 只读本地缓存不发网络，解析失败给 null
                        runCatching {
                            val quota = engine.queryQuotaAsync()
                            Log.d("RppgQuota", "expireAtFormatted=${quota?.expireAtFormatted}")
                            licenseDaysLeftCache = quota?.let { parseLicenseDaysLeft(it.expireAtFormatted) }
                        }
                    }
                } finally {
                    pollInFlight.set(false)
                }

                // 呼吸率轮询：每 BREATHING_POLL_EVERY_CYCLES 周期调一次呼吸率通道更新快照。
                // 不在 pollInFlight 临界区内：呼吸率请求自身在协程里跑，不阻塞 metrics 轮询。
                cycle++
                if (cycle >= BREATHING_POLL_EVERY_CYCLES) {
                    cycle = 0
                    runCatching {
                        val hrv = engine.getHrvAsync()
                        // valid=false 也存：mapMetrics 据此把 rrBpm 给 null
                        hrvSnapshot = hrv
                    }
                }

                kotlinx.coroutines.delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    // ── 会话超时处理（-16） ─────────────────────────────────────────────

    /**
     * 会话超时对策：release() + 用原 config 重新 initializeAsync。
     * 重试上限 3 次，超限停止循环。
     */
    private fun handleSessionTimeout() {
        val cfg = savedConfig ?: return
        val count = sessionRetryCount.incrementAndGet()
        if (count > MAX_SESSION_RETRY) {
            stopPolling()
            synchronized(lock) {
                initialized = false
            }
            return
        }
        stopPolling()
        scope.launch {
            runCatching { engine.release() }
            runCatching {
                val result = engine.initializeAsync(cfg)
                if (result.success) {
                    synchronized(lock) {
                        initialized = true
                        released = false
                        windowReady = false
                        metricsSnapshot = null
                        hrvSnapshot = null
                        lastFeedAtMs.set(Long.MIN_VALUE)
                    }
                    startPolling()
                } else {
                    synchronized(lock) {
                        initialized = false
                    }
                }
            }.getOrElse {
                synchronized(lock) {
                    initialized = false
                }
            }
        }
    }

    // ── 辅助 ────────────────────────────────────────────────────────────

    @Volatile
    private var licenseDaysLeftCache: Int? = null

    private val lock = Any()

    private fun assetsPresent(context: Context, name: String): Boolean = runCatching {
        context.assets.open(name).use { true }
    }.getOrDefault(false)

    companion object {
        private const val MODEL_ENC_ASSET = "step_mini.tflite.enc"
        private const val STATE_ENC_ASSET = "state_mini.bin.enc"
        private const val SDK_INIT_URL = "https://www.facephys.com/endsdk/sdk/init"
        private const val POLL_INTERVAL_MS = 2000L
        private const val MAX_SESSION_RETRY = 3
        /** 呼吸率轮询间隔：每 5 个 metrics 周期（5×2s=10s）调一次呼吸率通道 */
        private const val BREATHING_POLL_EVERY_CYCLES = 5
    }
}

// ──────────────────────────────────────────────────────────────────────────
// 纯函数映射（可单测，不依赖 Android 环境）
// ──────────────────────────────────────────────────────────────────────────

/**
 * SDK InitResult.code（Int 原始码）→ 契约 [InitErrorCode]。
 *
 * 码表（来自接入教程错误码速查 + RppgErrorCode 常量）：
 *   0     OK
 *  -3     ERR_BAD_MODEL        → MODEL_MISSING
 *  -20    ERR_ASSETS           → MODEL_MISSING
 *  -10    ERR_LICENSE          → NOT_AUTHORIZED
 *  -11    ERR_LICENSE_EXPIRED  → NOT_AUTHORIZED
 *  -12    ERR_LICENSE_INVALID  → NOT_ACTIVATED / PACKAGE_MISMATCH
 *  -13    ERR_LICENSE_NETWORK  → NO_NETWORK
 *  -14    ERR_QUOTA_EXHAUSTED  → QUOTA_EXCEEDED
 *  -16    ERR_SESSION_TIMEOUT  → SESSION_TIMEOUT
 *  -17    ERR_BANNED           → BANNED
 *  -18    ERR_CLOCK_ROLLBACK   → CLOCK_ROLLBACK
 *  其余   UNKNOWN
 */
fun mapInitCode(code: Int): InitErrorCode = when (code) {
    0 -> InitErrorCode.OK
    -3 -> InitErrorCode.MODEL_MISSING
    -20 -> InitErrorCode.MODEL_MISSING
    -10 -> InitErrorCode.NOT_AUTHORIZED
    -11 -> InitErrorCode.NOT_AUTHORIZED
    -12 -> InitErrorCode.NOT_ACTIVATED
    -13 -> InitErrorCode.NO_NETWORK
    -14 -> InitErrorCode.QUOTA_EXCEEDED
    -16 -> InitErrorCode.SESSION_TIMEOUT
    -17 -> InitErrorCode.BANNED
    -18 -> InitErrorCode.CLOCK_ROLLBACK
    else -> InitErrorCode.UNKNOWN
}

/**
 * SDK InitResult → 契约 [InitResult]。
 */
internal fun mapInitResult(sdkResult: SdkInitResult): InitResult {
    val code = mapInitCode(sdkResult.code)
    return if (sdkResult.success && code == InitErrorCode.OK) {
        InitResult.ok(sdkResult.message)
    } else {
        InitResult.fail(code, sdkResult.message)
    }
}

/**
 * MetricsResult 三态映射（纯函数，可单测）。
 *
 * @param metrics SDK MetricsResult（可能 hr=null / sqi=null / hasEnoughWindow=false）
 * @param hrv SDK HrvResult?（呼吸率来源，valid=false 时呼吸率给 null）
 * @param timestampMs 时间戳（由调用方传入，保证纯函数）
 * @return 三态：NotReady / NoSignal / Ok
 */
fun mapMetrics(
    metrics: SdkMetricsResult,
    hrv: SdkHrvResult?,
    timestampMs: Long,
): MetricsRead {
    if (!metrics.hasEnoughWindow) return MetricsRead.NotReady
    val hr = metrics.hr ?: return MetricsRead.NoSignal
    val rrBpm = if (hrv?.valid == true) hrv.rrBpm else null
    val sqi = metrics.sqi
    return MetricsRead.Ok(
        RppgMetrics(
            heartRateBpm = hr,
            breathingRatePerMin = rrBpm,
            sqi = sqi,
            timestampMs = timestampMs,
        )
    )
}

/**
 * ROI 长度校验（纯函数，可单测）。
 */
fun validateRoi(roi: ByteArray): FrameResult =
    if (roi.size != RppgEngine.ROI_BYTE_COUNT) FrameResult.ERR_BAD_ROI_SIZE else FrameResult.OK

/**
 * dtSec 校验（纯函数，可单测）。
 */
fun validateDtSec(dtSec: Float): FrameResult =
    if (!dtSec.isFinite() || dtSec <= 0f || dtSec > RppgEngine.MAX_DT_SEC) FrameResult.ERR_BAD_DT
    else FrameResult.OK

/**
 * 从 queryQuotaAsync().expireAtFormatted 推算剩余天数（纯函数，可单测）。
 *
 * expireAtFormatted 格式不保证（SDK 文档未明确），解析失败返回 null，不发网络。
 * 常见格式如 "2025-12-31" / "2025/12/31 23:59:59" 等。
 */
fun parseLicenseDaysLeft(expireAtFormatted: String?, now: Long = System.currentTimeMillis()): Int? {
    if (expireAtFormatted.isNullOrBlank()) return null
    return runCatching {
        // 提取日期部分 yyyy-MM-dd 或 yyyy/MM/dd
        val dateStr = expireAtFormatted.substringBefore(' ').trim()
        val parts = dateStr.split('-', '/')
        if (parts.size < 3) return null
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()
        // 用 java.util.Calendar 避免依赖 java.time（纯函数测试友好）
        val cal = java.util.Calendar.getInstance()
        cal.set(year, month - 1, day, 23, 59, 59)
        val expireMs = cal.timeInMillis
        val diffMs = expireMs - now
        val days = (diffMs / (24L * 60 * 60 * 1000)).toInt()
        days.coerceAtLeast(0)
    }.getOrNull()
}
