package com.facephys.rppg

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * 契约桩的唯一实现。
 *
 * 行为约定（与真实 SDK 对齐的部分）：
 *  · 缺 AppKey → [InitErrorCode.NOT_ACTIVATED]，P03 能展示出可解释原因
 *  · 帧长度不符 / dtSec 非法 → 返回对应 [FrameResult]，不崩溃
 *  · 并发投喂 → [FrameResult.ERR_CONCURRENT]（契约要求串行，出现即调用方 bug）
 *  · release() 之后再投喂 → [FrameResult.ERR_RELEASED]（真实 SDK 可能原生崩溃，
 *    Bridge 侧用状态机保证不会走到这里）
 *  · [hasEnoughWindow] 按累积帧时长真实翻转，[windowAgeMs] 真实计量
 *  · initialize 的回调在**锁外**触发，避免把异步竞态藏到调用侧
 *
 * 行为约定（**故意**与真实 SDK 不同的部分）：
 *  · [readMetrics] 在窗口攒够后返回 [MetricsRead.NoSignal] 而**不是** [MetricsRead.Ok]。
 *    没有真实推理就不可能有真实数值。任何"看起来像真的"占位值都是合规事故
 *    （需求 §5.6：无数据时显示数字 = 禁止）。
 *    这一点很重要：它意味着 QualityGate 必须把本实现判成 NO_DATA，
 *    而不是 DEGRADED（后者会在界面上显示"信号一般"并继续投喂）。
 */
class StubRppgEngine : RppgEngine {

    private val lock = Any()
    private val feeding = AtomicBoolean(false)
    private val lastFeedAtMs = AtomicReference(Long.MIN_VALUE)

    private var initialized = false
    private var released = false
    private var appKey: String = ""
    private var encAssetName: String = ""
    private var frameCount = 0L
    private var accumulatedSec = 0f

    override fun initialize(config: RppgConfig, callback: (InitResult) -> Unit) {
        val result: InitResult
        synchronized(lock) {
            encAssetName = config.encAssetName
            appKey = config.appKey
            released = false
            frameCount = 0
            accumulatedSec = 0f
            lastFeedAtMs.set(Long.MIN_VALUE)

            result = when {
                config.appKey.isBlank() ->
                    InitResult.fail(InitErrorCode.NOT_ACTIVATED, "appKey is blank")
                config.encAssetName.isBlank() ->
                    InitResult.fail(InitErrorCode.MODEL_MISSING, "encAssetName is blank")
                else -> InitResult.ok()
            }
            initialized = result.success
        }
        // 锁外回调：真实 SDK 的初始化回调必然来自工作线程，
        // 若在锁内同步回调，调用方会把"同步"当成契约，接入真实 AAR 时全盘推翻。
        callback(result)
    }

    override fun processFrame(roi: ByteArray, dtSec: Float): FrameResult {
        // 并发检测必须在 lock 之外用 CAS，否则 synchronized 会掩盖并发问题，
        // 让"必须串行"这条契约在桩里永远验不出来。
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
                frameCount++
                accumulatedSec += dtSec
                lastFeedAtMs.set(android.os.SystemClock.elapsedRealtime())
                return FrameResult.OK
            }
        } finally {
            feeding.set(false)
        }
    }

    override val hasEnoughWindow: Boolean
        get() = synchronized(lock) {
            initialized && !released && accumulatedSec >= HR_WINDOW_SEC
        }

    override val windowAgeMs: Long
        get() {
            val last = lastFeedAtMs.get()
            if (last == Long.MIN_VALUE) return Long.MAX_VALUE
            return (android.os.SystemClock.elapsedRealtime() - last).coerceAtLeast(0L)
        }

    override fun readMetrics(): MetricsRead = synchronized(lock) {
        if (!initialized || released) return MetricsRead.NotReady
        if (accumulatedSec < HR_WINDOW_SEC) return MetricsRead.NotReady
        // 窗口够了，但桩没有推理能力 —— 这是 NO_DATA，不是 DEGRADED。
        MetricsRead.NoSignal
    }

    override fun getHrvAsync(callback: (HrvResult?) -> Unit) {
        callback(null)
    }

    override fun reset() {
        synchronized(lock) {
            frameCount = 0
            accumulatedSec = 0f
            lastFeedAtMs.set(Long.MIN_VALUE)
        }
    }

    override fun release() {
        synchronized(lock) {
            released = true
            initialized = false
            frameCount = 0
            accumulatedSec = 0f
            lastFeedAtMs.set(Long.MIN_VALUE)
        }
    }

    override fun diagnostics(): SdkDiagnostics = synchronized(lock) {
        SdkDiagnostics(
            // 故意带 STUB 前缀：P03 截图必须一眼看出这是契约桩，
            // 不能被误读成"已集成真实 SDK 1.5.0"
            sdkVersion = "STUB-0.0.0-contract",
            licenseDaysLeft = null,
            initialized = initialized && !released,
            modelAssetPresent = false,
        )
    }

    /** 内部可观测计数（仅日志/诊断用，不参与任何界面数值）。 */
    fun debugStats(): Pair<Long, Float> = synchronized(lock) { frameCount to accumulatedSec }

    companion object {
        /** 心率可用窗口下限，对齐需求 §5.4「心率可用 ≥ 30s」。 */
        const val HR_WINDOW_SEC = 30f

        /** 模型资源名。真实 AAR 会下发同名资源。 */
        const val ENC_ASSET = "facephys_model.enc"
    }
}