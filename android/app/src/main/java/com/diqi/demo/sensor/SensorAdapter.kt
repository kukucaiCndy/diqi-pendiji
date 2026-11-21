package com.diqi.demo.sensor

import com.diqi.demo.data.db.MissingReason
import com.diqi.demo.data.db.QualityState

/**
 * 一次感知读数快照。
 *
 * 字段全部可空：**任一通道为 null 表示该通道当前不可用**，界面必须按
 * "不显示"处理，不得用 0 或占位符替代（FR-08）。
 */
data class SensorSnapshot(
    val state: QualityState = QualityState.NO_DATA,
    val heartRateBpm: Float? = null,
    val breathingRatePerMin: Float? = null,
    val sqi: Float? = null,
    val missingReason: MissingReason = MissingReason.NOT_ENABLED,
    /** 权重子标记：演示数据（必须带全屏水印，AC-19）*/
    val isSimulated: Boolean = false,
    /** 已运行秒数，用于"信号建立中"进度 */
    val elapsedSec: Int = 0,
    /** 人脸是否在框内 */
    val faceInFrame: Boolean = false,
    /**
     * 人脸已连续丢失的毫秒数（0 表示在框内）。
     *
     * 这个值必须由**数据源真实累计**，不能由消费方拍一个常量。
     * 首版训练页直接用 `FACE_LOST_NO_DATA_MS` 常量判"到阈值了"，
     * 于是"3 秒"这条规则在真实链路上从来没有被真正计量过（AC-16 的形式主义）。
     */
    val faceLostMs: Long = 0L,
    /** 光照是否充足 */
    val lightOk: Boolean = true,
    /**
     * **窗口地面真相**：数据源的采集窗口是否已攒够（`bridge.hasEnoughWindow`）。
     *
     * 三态语义（[QualityGate.evaluate] 的第 2 步据此判"信号建立中"）：
     *  · `true`  → 窗口已满，**不再**认 `elapsedSec < 30` 的旧代理，立刻允许出数；
     *  · `false` → 窗口未满，建立中（哪怕会话已跑过 30 秒）；
     *  · `null`  → 数据源没有 SDK 窗口这回事（NullSensor / DemoSensor），
     *    沿用 `elapsedSec < hrReadySec` 的旧代理，行为与首版逐字节一致。
     *
     * 为什么需要它：会话 `elapsedSec` 是"这场训练跑了多久"，不是"信号攒了多久"。
     * 校准页已经攒满窗口时进入训练，会话秒数从 0 起，旧代理会误报"建立中"30 秒。
     */
    val windowReady: Boolean? = null,
) {
    companion object {
        /** 未启用感知：整块不显示，只留一行（FR-06）*/
        val NOT_ENABLED = SensorSnapshot(
            state = QualityState.NO_DATA,
            missingReason = MissingReason.NOT_ENABLED,
        )
    }
}

/**
 * 感知适配器。
 *
 * 三种实现对应需求 §2.1 的三种数据源：
 * - [NullSensor]   未授权 / SDK 不可用 / 用户选择不带摄像头
 * - [DemoSensor]   仅演示模式，输出模拟值（必须水印隔离）
 * - `RppgSensor`   真实端侧推理（S6 第二部分，经 `FacePhysBridge`）
 *
 * ## 为什么用接口而不是一个带 flag 的类
 *
 * 三种数据源的**合规约束完全不同**：Demo 必须水印、Null 必须显示"未启用"、
 * 真实源必须做四态降级。用接口让"某个数据源忘了水印"变成编译期缺实现，
 * 而不是运行期靠人记得判断 flag。
 */
interface SensorAdapter {

    /** 数据源模式，写库时据此填 `sensorMode` */
    val mode: com.diqi.demo.data.db.SensorMode

    /** 是否真实感知（只有真实源才允许带生理数值）*/
    val isReal: Boolean

    suspend fun start()

    /** 投喂一帧 ROI（仅真实源需要；Null/Demo 忽略）*/
    fun feed(roi: ByteArray, dtSec: Float)

    /** 读取当前快照。**永远不返回伪造值**（Demo 除外，且 Demo 带 isSimulated 标记）。 */
    fun snapshot(nowElapsedSec: Int): SensorSnapshot

    suspend fun stop()

    /** 页面销毁 / 切换使用者 */
    fun release()

    /** 诊断信息（P03 折叠区）*/
    fun diagnostics(): SensorDiagnostics
}

data class SensorDiagnostics(
    val sdkVersion: String,
    val initialized: Boolean,
    val licenseDaysLeft: Int?,
    val modelAssetPresent: Boolean,
    /** 失败原因的用户可读文案；null 表示无失败 */
    val failureMessage: String? = null,
)

/**
 * 空感知源。
 *
 * 用于：用户没开摄像头、未授权、SDK 不可用。
 * 它**永远**返回 [QualityState.NO_DATA] + [MissingReason.NOT_ENABLED]，
 * 界面因此在指标区只显示一行「未启用感知」。
 */
class NullSensor : SensorAdapter {
    override val mode = com.diqi.demo.data.db.SensorMode.OFF
    override val isReal = false
    override suspend fun start() = Unit
    override fun feed(roi: ByteArray, dtSec: Float) = Unit
    override fun snapshot(nowElapsedSec: Int) = SensorSnapshot.NOT_ENABLED
    override suspend fun stop() = Unit
    override fun release() = Unit
    override fun diagnostics() = SensorDiagnostics(
        sdkVersion = "—",
        initialized = false,
        licenseDaysLeft = null,
        modelAssetPresent = false,
    )
}

/**
 * 演示源（FR-14 / AC-19）。
 *
 * ## 它唯一被允许"造假"的理由
 *
 * 演示模式的存在意义是让人看到"有数据时长什么样"，所以它**必须**产出数值。
 * 但合规上有三条硬约束，这里逐条守住：
 *
 * 1. 每个快照都带 `isSimulated = true`，UI 据此渲染**全屏水印**；
 * 2. 写库时 `sensorMode = 'simulated'`，数据层统一过滤，不计入打卡与周报；
 * 3. 数值是**明显的人造序列**（正弦 + 固定基线），不做"看起来像随机真实值"的伪装。
 *
 * 另外刻意**不做**平滑：需求 §5.6 禁止"平滑造假"，
 * 而演示值本身就是假的，再加平滑只会让人误以为算法在工作。
 */
class DemoSensor(
    private val hrBase: Float = 72f,
    private val brBase: Float = 16f,
) : SensorAdapter {

    override val mode = com.diqi.demo.data.db.SensorMode.SIMULATED
    override val isReal = false

    @Volatile
    private var started = false

    /** 人脸丢失的起始时刻（单调时钟），用于真实累计 faceLostMs */
    @Volatile
    private var faceLostSinceMs: Long = 0L

    override suspend fun start() {
        started = true
    }

    override fun feed(roi: ByteArray, dtSec: Float) = Unit

    override fun snapshot(nowElapsedSec: Int): SensorSnapshot {
        if (!started) return SensorSnapshot.NOT_ENABLED

        // 演示模式缩短信号建立期到 3 秒，让演示「马上有」——
        // 真实源仍保持 30 秒（AC-15 的对照）
        if (nowElapsedSec < HR_READY_SEC) {
            return SensorSnapshot(
                state = QualityState.NO_DATA,
                isSimulated = true,
                elapsedSec = nowElapsedSec,
                faceInFrame = true,
                missingReason = MissingReason.NONE,
            )
        }

        // 故障注入（仅演示模式）：让 AC-16/17 的界面切换能在真机上被验证
        when (DemoFault.current) {
            DemoFault.Kind.FACE_LOST -> {
                if (faceLostSinceMs == 0L) faceLostSinceMs = android.os.SystemClock.elapsedRealtime()
                return SensorSnapshot(
                state = QualityState.NO_DATA,
                isSimulated = true,
                elapsedSec = nowElapsedSec,
                    faceInFrame = false,
                    faceLostMs = android.os.SystemClock.elapsedRealtime() - faceLostSinceMs,
                    missingReason = MissingReason.FACE_LOST,
                )
            }
            DemoFault.Kind.LOW_LIGHT -> return SensorSnapshot(
                state = QualityState.DEGRADED,
                heartRateBpm = hrBase,
                sqi = 0.72f,
                isSimulated = true,
                elapsedSec = nowElapsedSec,
                faceInFrame = true,
                lightOk = false,
                missingReason = MissingReason.LOW_LIGHT,
            )
            DemoFault.Kind.LOW_SQI -> return SensorSnapshot(
                state = QualityState.INVALID,
                heartRateBpm = hrBase,
                sqi = 0.20f,
                isSimulated = true,
                elapsedSec = nowElapsedSec,
                faceInFrame = true,
                missingReason = MissingReason.NONE,
            )
            DemoFault.Kind.NONE -> Unit
        }

        // 故障解除：重置丢失计时
        faceLostSinceMs = 0L

        // 用正弦做可见的、可预测的变化，方便演示时讲解"数值会随呼吸起伏"
        val t = nowElapsedSec.toFloat()
        val hr = hrBase + 4f * kotlin.math.sin(t / 9f)
        val sqi = 0.82f

        return SensorSnapshot(
            state = QualityState.VALID,
            heartRateBpm = hr,
            // 呼吸率在 BR_READY_SEC 后出现（演示模式缩短到 6s）
            breathingRatePerMin = if (nowElapsedSec >= BR_READY_SEC) {
                brBase + 1.5f * kotlin.math.sin(t / 13f)
            } else {
                null
            },
            sqi = sqi,
            missingReason = MissingReason.NONE,
            isSimulated = true,
            elapsedSec = nowElapsedSec,
            faceInFrame = true,
        )
    }

    override suspend fun stop() {
        started = false
    }

    override fun release() {
        started = false
    }

    override fun diagnostics() = SensorDiagnostics(
        sdkVersion = "DEMO",
        initialized = true,
        licenseDaysLeft = null,
        modelAssetPresent = false,
    )

    companion object {
        /** 演示模式信号建立秒数（缩短到 3s，让演示「马上有」）*/
        const val HR_READY_SEC = 3
        /** 演示模式呼吸率出现秒数（缩短到 6s）*/
        const val BR_READY_SEC = 6
    }
}