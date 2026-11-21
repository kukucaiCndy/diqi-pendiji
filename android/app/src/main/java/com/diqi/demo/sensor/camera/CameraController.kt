package com.diqi.demo.sensor.camera

import android.content.Context
import android.hardware.camera2.CaptureRequest
import android.util.Log
import android.view.ViewGroup
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * CameraX 控制器：预览 + 分析。
 *
 * ## 三条实现要点
 *
 * 1. **分析固定单线程**（需求 §8.3 第 2 条）：`processFrame` 并发会失败。
 * 2. **`STRATEGY_KEEP_ONLY_LATEST`**：宁可丢帧也不排队 ——
 *    排队会让指标滞后，且帧积压会让延迟与内存同时上升。
 * 3. **必须 unbind，但只解绑自己的**：页面销毁时只 `unbind(自己的 preview, analysis)`
 *    并关执行器。**不要 `unbindAll()`** —— 导航转场期两个页面同时在 composition，
 *    `unbindAll()` 会把另一个页面的用例摘掉（训练页预览定格 + analyzer 停）。
 *    `stop()` / `release()` 必须能精确解绑，否则连"连续进出后相机打不开"（AC-18）也解释不了。
 *
 * 相机生命周期刻意**绑在传入的 LifecycleOwner 上**：
 * 校准页用 Activity 的 lifecycle；训练页如需常驻，由调用方给更长的 owner。
 * 这与 D12「ON_STOP 停投喂、ON_START 续」一致 —— 锁屏即 unbind，
 * 但 SDK 窗口在内存里保留，返回后恢复投喂即可继续。
 */
class CameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val analyzer: FaceAnalyzer,
) {

    private var provider: ProcessCameraProvider? = null
    private var analysisExecutor: ExecutorService? = null
    private var bound = false

    /**
     * 自己绑定的两个用例 —— **停止时必须精确解绑自己这两个，绝不能用 `unbindAll()`**。
     *
     * 踩过的坑：导航转场期校准页与训练页**同时在 composition** 里，
     * 校准页离场时 `release()` 里的 `unbindAll()` 会把训练页刚绑定的
     * preview + analysis 一并摘掉 —— 表现为「训练页预览停在最后一帧、
     * analyzer 也不再回调」。改为各自只解绑自己的用例，两个页面就互不干扰。
     */
    private var previewUseCase: Preview? = null
    private var analysisUseCase: ImageAnalysis? = null

    /** 绑定的 Camera 实例（用于运行时锁定 AE/AWB）。bindToLifecycle 后赋值。 */
    private var camera: androidx.camera.core.Camera? = null

    /** AE/AWB 是否已锁定（幂等：只锁一次）。 */
    private val aeAwbLocked = java.util.concurrent.atomic.AtomicBoolean(false)

    /** 相机不可用的原因（可解释，供 UI 展示 —— FR-03 要求失败原因必须能说清）*/
    var lastError: String? = null
        private set

    /**
     * 启动预览并把帧送给分析器。
     *
     * @param previewView 预览控件；为 null 时只跑分析（训练页不需要预览时用）
     */
    fun start(previewView: PreviewView?) {
        Log.d(TAG, "[camera-trace] start() previewView=${previewView != null}")
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val cameraProvider = future.get()
                provider = cameraProvider
                bindUseCases(cameraProvider, previewView)
            } catch (e: Exception) {
                lastError = describe(e)
                Log.e(TAG, "相机启动失败", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindUseCases(cameraProvider: ProcessCameraProvider, previewView: PreviewView?) {
        val executor = analysisExecutor ?: Executors.newSingleThreadExecutor().also {
            analysisExecutor = it
        }

        val previewBuilder = Preview.Builder()
        // AE/AWB 在启动时保持自动（原 builder 阶段锁死导致初始场景曝光/色偏），
        // 首次检测到人脸后再通过 lockAeAwb() 锁定收敛值。

        val preview = previewBuilder.build().also { p ->
            previewView?.let { p.surfaceProvider = it.surfaceProvider }
        }

        val analysisBuilder = ImageAnalysis.Builder()
            // 12fps（需求 §5.4）；KEEP_ONLY_LATEST 保证不排队
            .setTargetResolution(android.util.Size(640, 480))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)

        val analysis = analysisBuilder.build()
            .also { it.setAnalyzer(executor, analyzer) }

        try {
            // 先只解绑**自己**上一次绑的用例，绝不 unbindAll —— 见 previewUseCase 注释。
            unbindOwn("重绑前清理")
            bindTo(cameraProvider, preview, analysis)
        } catch (e: Exception) {
            // 兜底：极端情况下（另一个页面的用例仍占着相机且与之冲突）精确绑定会抛。
            // 退到 unbindAll 再绑一次 —— 这是最后手段，日志里能看到是否走到这里。
            Log.w(TAG, "[camera-trace] 精确绑定失败，回退 unbindAll 重试：${e.message}")
            try {
                cameraProvider.unbindAll()
                bindTo(cameraProvider, preview, analysis)
            } catch (e2: Exception) {
                lastError = describe(e2)
                Log.e(TAG, "绑定用例失败", e2)
            }
        }
    }

    private fun bindTo(
        cameraProvider: ProcessCameraProvider,
        preview: Preview,
        analysis: ImageAnalysis,
    ) {
        camera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_FRONT_CAMERA,
            preview,
            analysis,
        )
        previewUseCase = preview
        analysisUseCase = analysis
        bound = true
        lastError = null
        Log.d(
            TAG,
            "[camera-trace] bind ok preview=${preview.hashCode()} analysis=${analysis.hashCode()}",
        )
    }

    /**
     * 只解绑自己绑过的 preview / analysis。
     *
     * **不用 `unbindAll()`**：转场期同进程里可能还有另一个页面的 (preview, analysis)，
     * `unbindAll()` 会连它的一起摘掉，导致对方预览定格、analyzer 不再回调。
     */
    private fun unbindOwn(reason: String) {
        val useCases = listOfNotNull(previewUseCase, analysisUseCase)
        previewUseCase = null
        analysisUseCase = null
        if (useCases.isEmpty()) return
        runCatching {
            provider?.unbind(*useCases.toTypedArray())
            Log.d(TAG, "[camera-trace] unbind own (${reason}) count=${useCases.size}")
        }.onFailure { Log.w(TAG, "[camera-trace] unbind 失败（$reason）：${it.message}") }
    }

    /** 停止投喂但保留 provider（页面暂停/锁屏时调用）。 */
    fun stop() {
        Log.d(TAG, "[camera-trace] stop()")
        unbindOwn("stop")
        bound = false
        camera = null
        aeAwbLocked.set(false)
    }

    /**
     * 锁定自动曝光（AE）与自动白平衡（AWB）的当前收敛值。
     *
     * **关键区别**：
     * - `CONTROL_AWB_LOCK=true` 锁定 AWB 当前收敛值（保持已收敛的色彩）。
     * - `CONTROL_AWB_MODE=OFF` 是**禁用**白平衡（无收敛 → 色偏，干扰 blazeface 检测）。
     *
     * 调用时机：首次检测到人脸后（AWB/AE 已对当前场景收敛）调用一次。
     * 幂等：[aeAwbLocked] 保证只锁一次，重复调用无副作用。
     * 设备不支持时 runCatching 吞掉，不影响正常使用。
     */
    fun lockAeAwb() {
        if (!aeAwbLocked.compareAndSet(false, true)) return
        val cam = camera ?: run {
            Log.w(TAG, "lockAeAwb: camera 未绑定，跳过")
            return
        }
        runCatching {
            // 用 Camera2CameraControl 在运行时（repeating request 上）设捕获请求选项，
            // 而非 builder 阶段。这样启动时 AE/AWB 保持自动，首帧检测到人脸后锁定收敛值。
            val options = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, true)
                .setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, true)
                .build()
            Camera2CameraControl.from(cam.cameraControl).addCaptureRequestOptions(options)
            Log.d(TAG, "AE/AWB 已锁定（首次检测到人脸）")
        }.onFailure { e ->
            Log.w(TAG, "lockAeAwb 不可用（设备可能不支持）：${e.message}")
        }
    }

    /** 彻底释放。页面销毁时调用。 */
    fun release() {
        Log.d(TAG, "[camera-trace] release()")
        unbindOwn("release")
        provider = null
        camera = null
        bound = false
        aeAwbLocked.set(false)
        analysisExecutor?.shutdown()
        analysisExecutor = null
        analyzer.release()
    }

    val isRunning: Boolean get() = bound

    /** 把异常翻译成用户能看懂的话（FR-03「失败原因必须可解释」）。 */
    private fun describe(e: Exception): String = when {
        e is SecurityException -> "没有获得相机权限"
        e.message?.contains("in use", ignoreCase = true) == true -> "相机被其他应用占用了"
        e.message?.contains("No available camera", ignoreCase = true) == true -> "这台设备没有可用的前置相机"
        else -> "相机启动失败：${e.message ?: e.javaClass.simpleName}"
    }

    private companion object {
        const val TAG = "CameraController"
    }
}

/** 让 PreviewView 撑满父容器的快捷方法。 */
fun PreviewView.fillParent(): PreviewView = apply {
    layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
    )
}