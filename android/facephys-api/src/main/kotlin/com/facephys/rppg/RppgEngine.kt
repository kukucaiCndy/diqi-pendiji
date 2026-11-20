package com.facephys.rppg

import android.content.Context

/**
 * FacePhys 端侧 rPPG 引擎的 **API 契约桩**。
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 本模块存在的唯一理由：真实 AAR（FacePhys 公测版 v1.5.0）尚未下发，
 * 而 app 侧需要一个可编译、可运行、生命周期完整的接口面。
 *
 * 真实 AAR 到手后的替换步骤：
 *   1. 删除 `:facephys-api` 模块（settings.gradle.kts 移除 include）。
 *   2. `app/build.gradle.kts` 里
 *        implementation(project(":facephys-api"))
 *      → implementation(files("libs/facephys-v1.5.0.aar"))
 *   3. 只调整 `sensor/FacePhysBridge.kt` 一个文件（app 内唯一 SDK 接触点）。
 *
 * 替换时**必须逐项核对**（这些是最容易错、且错了不会崩溃只会"永远没有数据"的点）：
 *   · 真实 FQN 是否就是 com.facephys.rppg.*（若不同，只改 Bridge 的 import）
 *   · processFrame 的 dtSec 单位是「秒」还是「毫秒」、是帧间隔还是帧时间戳
 *   · ROI 字节序：本契约假定 9×9×3、RGB 交错、无符号字节（共 243）
 *   · ROI 是否要求画面正立（前置相机是镜像的，由 CameraController 负责翻转，
 *     不在这里处理 —— 本接口拿不到 cameraId/rotation/isMirror）
 *   · initialize 回调跑在哪个线程（Bridge 已做线程归一化）
 *   · readMetrics 是否阻塞（Bridge 一律放 Dispatchers.IO）
 *   · release() 之后再调 processFrame 的行为（真实 SDK 可能原生 SIGSEGV，
 *     Bridge 用状态机保证不会走到那一步）
 *   · InitErrorCode 的真实错误码取值
 * ─────────────────────────────────────────────────────────────────────────
 *
 * **合规红线**：本模块的任何取数路径都不得产生生理数值。
 * 见 [StubRppgEngine] —— `readMetrics()` 永远返回 [MetricsRead.NoSignal]。
 */
interface RppgEngine {

    /**
     * 异步初始化并预热。回调**只会被调用一次**，且可能在任意线程。
     * 实现方不要在持锁状态下回调（会把异步竞态藏到调用侧）。
     */
    fun initialize(config: RppgConfig, callback: (InitResult) -> Unit)

    /**
     * 投喂一帧 ROI。**必须单线程串行调用**。
     * @param roi 9×9×3 RGB 交错字节，长度必须为 [ROI_BYTE_COUNT]
     * @param dtSec 距上一帧的间隔，单位：秒
     */
    fun processFrame(roi: ByteArray, dtSec: Float): FrameResult

    /** 滑动窗口是否已攒够。攒够后才可以期待心率出现。 */
    val hasEnoughWindow: Boolean

    /**
     * 距最近一次成功投喂的毫秒数。
     *
     * 为什么需要它：`hasEnoughWindow` 只说"窗口攒够了"，不说"窗口还新鲜"。
     * 锁屏 30s 回来时窗口仍在内存里但已过期 30s，此时若仍按 VALID 渲染，
     * 就会把陈旧数值当成实时值展示 —— 违反 FR-08「数据新鲜」判定。
     */
    val windowAgeMs: Long

    /**
     * 读取最近一次指标。**三态**，不要用 null 表达多种含义：
     *  · [MetricsRead.NotReady]  窗口未攒够 —— 界面显示「信号建立中」
     *  · [MetricsRead.NoSignal]  窗口够了但无有效输出 —— 界面按 NO_DATA 置灰
     *  · [MetricsRead.Ok]        有可展示值
     */
    fun readMetrics(): MetricsRead

    /** 呼吸率/HRV 通道；约 60s 后才可能有值。本期不展示 HRV，仅内部验证。 */
    fun getHrvAsync(callback: (HrvResult?) -> Unit)

    /** 切换使用者时清空窗口。 */
    fun reset()

    /** 释放原生资源。释放后不得再投喂。 */
    fun release()

    /** 设备诊断信息（P03 诊断折叠区数据源）。 */
    fun diagnostics(): SdkDiagnostics

    companion object {
        /** 9 × 9 × 3 = 243 */
        const val ROI_BYTE_COUNT = 243

        /** dtSec 上限：12fps 下帧间隔约 0.083s，超过 2s 必为链路故障。 */
        const val MAX_DT_SEC = 2.0f
    }
}

/**
 * 一次投喂的结果。
 *
 * 用枚举而不是 Int 错误码：真实 SDK 的错误码取值未知，
 * 适配层负责映射；调用方不应该看到魔法数字。
 */
enum class FrameResult {
    OK,
    /** 尚未初始化 */
    ERR_NOT_INITIALIZED,
    /** 已 release，不得再投喂 */
    ERR_RELEASED,
    /** 并发投喂 —— 契约要求串行，出现即为调用方 bug */
    ERR_CONCURRENT,
    /** ROI 长度不符 */
    ERR_BAD_ROI_SIZE,
    /** dtSec 非法（NaN / ≤0 / 超过 [RppgEngine.MAX_DT_SEC]）*/
    ERR_BAD_DT,
    /** 其他未知错误（必须兜底）*/
    ERR_UNKNOWN,
}

/** 读数三态。见 [RppgEngine.readMetrics]。 */
sealed interface MetricsRead {
    /** 窗口未攒够 */
    data object NotReady : MetricsRead
    /** 窗口够了，但算法没有可信输出 */
    data object NoSignal : MetricsRead
    /** 有可展示值 */
    data class Ok(val metrics: RppgMetrics) : MetricsRead
}

/**
 * 初始化配置。
 *
 * 带 [context] 是因为真实 SDK 需要读取 assets 里的 `.enc` 模型文件；
 * 只传资源名字符串是做不到的（这是首版契约的一个真实缺陷）。
 *
 * 允许为 null 的唯一场景是**纯 JVM 契约测试**：桩实现从不访问 assets，
 * 真实 AAR 则要求非空。
 */
data class RppgConfig(
    val context: Context?,
    /** 模型资源名（.enc），位于 app 的 assets/ 下 */
    val encAssetName: String,
    /** AppKey。来自 local.properties → BuildConfig.FACEPHYS_APP_KEY */
    val appKey: String,
)