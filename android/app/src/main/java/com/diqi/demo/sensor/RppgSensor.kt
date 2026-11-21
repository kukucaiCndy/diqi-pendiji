package com.diqi.demo.sensor

import com.diqi.demo.data.db.MissingReason
import com.diqi.demo.data.db.QualityState
import com.diqi.demo.data.db.SensorMode
import com.facephys.rppg.MetricsRead

/**
 * 真实端侧 rPPG 感知源（S6 第二部分）。
 *
 * 包装 [FacePhysBridge] + 帧状态（faceLostMs / lightOk），实现 [SensorAdapter]。
 * 这是 [SensorAdapter] 注释里预告的第三种数据源。
 *
 * ## 三态映射（[mapSnapshot]）
 *
 * | bridge.read() | SensorSnapshot |
 * |---|---|
 * | [MetricsRead.Ok] | VALID（hr/rr/sqi，isSimulated=false）|
 * | [MetricsRead.NoSignal] | NO_DATA（faceLostMs / lightOk 透传）|
 * | [MetricsRead.NotReady] | NO_DATA（信号建立中，elapsedSec 透传）|
 *
 * NoSignal 与 NotReady 都映射成 NO_DATA —— 区别只在于界面文案，
 * 由 [QualityGate] 根据 `elapsedSec < hrReadySec` 判定。
 *
 * ## 帧状态来源
 *
 * `faceLostMs` 与 `lightOk` 由 [FaceAnalyzer] 逐帧真实累计（照 CalibrateViewModel 的跟踪方式）。
 * 本类只持有最近一次帧状态，不自己累计 —— 累计逻辑在 FaceAnalyzer 里（AC-16 的真实计量）。
 */
class RppgSensor(
    private val bridge: FacePhysBridge,
    private val appKey: String,
    private val encAssetName: String,
    /**
     * 本实例是否拥有 bridge 的生命周期。
     *
     * `false` = bridge 由上游（`AppContainer` 的共享组装）持有，**进程级**：
     * 页面销毁只停投喂，绝不 `bridge.release()` —— 一旦释放，SDK 引擎窗口被清空，
     * 下个页面（训练）要从 0 帧重攒约 30 秒，"校准攒满窗→训练立即出数"就不成立了。
     *
     * 默认 `true` 保持单实例语义（自建 bridge 的调用方仍能靠 release 收干净）。
     */
    private val ownsBridge: Boolean = true,
) : SensorAdapter {

    override val mode: SensorMode = SensorMode.OFFLINE_SDK

    /** 真实源：isReal=true，允许带生理数值（与 DemoSensor 的 isSimulated 对立）。 */
    override val isReal: Boolean = true

    @Volatile
    private var started = false

    /** 最近一次帧状态（由 onFrame 回调更新）。 */
    @Volatile
    private var faceLostMs: Long = 0L

    @Volatile
    private var lightOk: Boolean = true

    override suspend fun start() {
        started = true
        bridge.init(appKey, encAssetName) { }
    }

    override fun feed(roi: ByteArray, dtSec: Float) {
        bridge.feed(roi, dtSec)
    }

    override fun snapshot(nowElapsedSec: Int): SensorSnapshot {
        if (!started) return SensorSnapshot.NOT_ENABLED

        val read = bridge.read()
        return mapSnapshot(
            read = read,
            nowElapsedSec = nowElapsedSec,
            faceLostMs = faceLostMs,
            lightOk = lightOk,
            // 窗口地面真相：训练页据此判"信号建立中"，而不是靠会话秒数
            windowReady = bridge.hasEnoughWindow,
        )
    }

    /** 由 FaceAnalyzer.onFrame 回调更新帧状态。 */
    fun updateFrameState(faceLostMs: Long, lightOk: Boolean) {
        this.faceLostMs = faceLostMs
        this.lightOk = lightOk
    }

    override suspend fun stop() {
        started = false
    }

    override fun release() {
        started = false
        // 共享 bridge（ownsBridge=false）：只断开本 sensor 的投喂，
        // **不释放引擎** —— 窗口是引擎状态，释放即清空，下一个页面要从零重攒。
        // 共享实例的生命周期归 AppContainer（进程级，随进程结束）。
        if (ownsBridge) bridge.release()
    }

    override fun diagnostics(): SensorDiagnostics {
        val diag = bridge.diagnostics()
        return SensorDiagnostics(
            sdkVersion = diag.sdkVersion,
            initialized = diag.initialized,
            licenseDaysLeft = diag.licenseDaysLeft,
            modelAssetPresent = diag.modelAssetPresent,
            failureMessage = bridge.lastFailure?.userMessage,
        )
    }
}

/**
 * MetricsRead → SensorSnapshot 纯函数映射（可单测，不依赖 Android 环境）。
 *
 * @param read bridge.read() 的三态结果
 * @param nowElapsedSec 已运行秒数（信号建立进度）
 * @param faceLostMs 人脸已连续丢失的毫秒数（0=在框内），来自 FaceAnalyzer
 * @param lightOk 光照是否充足，来自 FaceAnalyzer
 * @param windowReady 窗口是否已攒够（`bridge.hasEnoughWindow`）；null 表示未知/无窗口
 * @return SensorSnapshot：Ok→VALID，NoSignal/NotReady→NO_DATA
 */
fun mapSnapshot(
    read: MetricsRead,
    nowElapsedSec: Int,
    faceLostMs: Long,
    lightOk: Boolean,
    windowReady: Boolean? = null,
): SensorSnapshot = when (read) {
    is MetricsRead.Ok -> {
        val metrics = read.metrics
        SensorSnapshot(
            state = QualityState.VALID,
            heartRateBpm = metrics.heartRateBpm,
            breathingRatePerMin = metrics.breathingRatePerMin,
            sqi = metrics.sqi,
            missingReason = MissingReason.NONE,
            isSimulated = false,
            elapsedSec = nowElapsedSec,
            faceInFrame = faceLostMs == 0L,
            faceLostMs = faceLostMs,
            lightOk = lightOk,
            // Ok 必然意味着窗口已满（RealRppgEngine.mapMetrics 的门槛），
            // 但如实透传 hasEnoughWindow，不在这里替 SDK 断言。
            windowReady = windowReady,
        )
    }
    is MetricsRead.NoSignal -> SensorSnapshot(
        state = QualityState.NO_DATA,
        missingReason = MissingReason.NONE,
        isSimulated = false,
        elapsedSec = nowElapsedSec,
        faceInFrame = faceLostMs == 0L,
        faceLostMs = faceLostMs,
        lightOk = lightOk,
        windowReady = windowReady,
    )
    MetricsRead.NotReady -> SensorSnapshot(
        state = QualityState.NO_DATA,
        // 建立中：elapsedSec 与 windowReady 都透传，QualityGate 优先认 windowReady
        missingReason = MissingReason.NONE,
        isSimulated = false,
        elapsedSec = nowElapsedSec,
        faceInFrame = faceLostMs == 0L,
        faceLostMs = faceLostMs,
        lightOk = lightOk,
        windowReady = windowReady,
    )
}

/**
 * 训练页传感器三态选择标签（纯函数 [chooseSensorKind] 的返回值）。
 *
 * 四分支（FR-08 / AC-14 底线：无 key / 无相机绝不出假数）：
 *  1. demoMode → DEMO
 *  2. cameraEnabled 且 appKey 非空 → REAL（真实源）
 *  3. cameraEnabled 但 appKey 空 → NULL（无 key 不造假）
 *  4. 非 demo 且无 camera → NULL
 */
enum class SensorKind {
    DEMO,
    REAL,
    NULL,
}

/**
 * 训练页传感器三态选择（纯函数，可单测，不依赖 Android 环境）。
 *
 * @param demoMode 演示模式
 * @param cameraEnabled 用户是否启用摄像头
 * @param appKey BuildConfig.FACEPHYS_APP_KEY（空表示未配置真实 SDK）
 * @return 选择标签 [SensorKind]
 */
fun chooseSensorKind(
    demoMode: Boolean,
    cameraEnabled: Boolean,
    appKey: String,
): SensorKind = when {
    demoMode -> SensorKind.DEMO
    cameraEnabled && appKey.isNotBlank() -> SensorKind.REAL
    else -> SensorKind.NULL
}

/** 真实 SDK 主模型资产名（与 RealRppgEngine.MODEL_ENC_ASSET 对齐）。 */
const val RppgSensorEncAsset = "step_mini.tflite.enc"
