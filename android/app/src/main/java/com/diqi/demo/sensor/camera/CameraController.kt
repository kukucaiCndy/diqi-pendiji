package com.diqi.demo.sensor.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.util.Log
import android.view.ViewGroup
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.Camera
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

    /**
     * `bindToLifecycle` 返回的 Camera 实例 —— 人脸优先 3A 要经它拿
     * `cameraControl`（→ Camera2CameraControl）与 `cameraInfo`（→ Camera2CameraInfo）。
     */
    private var camera: Camera? = null

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
        // AE/AWB **全程自动**（用户裁决 2026-11-21：锁定实验移除，待 SQI 证据再议）。
        // 历史：曾尝试「首帧锁定 AE/AWB」，但锁定时实测 sqi 只有 0.15–0.49，
        // 怀疑锁死收敛值压制了 SQI，故退回全自动、交给相机自己持续收敛。

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
        val boundCamera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_FRONT_CAMERA,
            preview,
            analysis,
        )
        previewUseCase = preview
        analysisUseCase = analysis
        camera = boundCamera
        bound = true
        lastError = null
        Log.d(
            TAG,
            "[camera-trace] bind ok preview=${preview.hashCode()} analysis=${analysis.hashCode()}",
        )
        // 绑定成功后再开人脸优先测光：此时才有 Camera2CameraControl 可写。
        enableFaceAeIfSupported(boundCamera)
    }

    /**
     * 条件开启**人脸优先自动曝光**（face AE）。
     *
     * ## 这是什么、不是什么
     *
     * face AE = **人脸优先的自动曝光**：AE/AWB **仍然全自动**，只是让相机 3A
     * 把人脸当作优先测光区域，从而让「人脸」这一块曝光更准。它**不是锁定** ——
     * 本方法绝不设置 `AE_LOCK` / `AWB_LOCK`（锁定实验已因压制 SQI 被移除）。
     *
     * 用户裁决 2026-11-21：保持 auto 的同时按设备能力条件开启 face AE。
     *
     * ## 判定方式
     *
     * 读 `STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES`：含 `FULL`(2) 优先 FULL，
     * 否则 `SIMPLE`(1)；一个都不含即该设备不支持人脸检测 → 保持纯 auto 并如实记日志。
     * 设备支持与否是**运行时事实**，只能真机判定，故这里不硬编码。
     */
    private fun enableFaceAeIfSupported(camera: Camera) {
        runCatching {
            val info = Camera2CameraInfo.from(camera.cameraInfo)
            val modes = info.getCameraCharacteristic(
                CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES,
            )
            val maxFaces = info.getCameraCharacteristic(
                CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT,
            )
            val supported = modes?.toSet() ?: emptySet()
            val mode = when {
                supported.contains(CameraMetadata.STATISTICS_FACE_DETECT_MODE_FULL) ->
                    CameraMetadata.STATISTICS_FACE_DETECT_MODE_FULL
                supported.contains(CameraMetadata.STATISTICS_FACE_DETECT_MODE_SIMPLE) ->
                    CameraMetadata.STATISTICS_FACE_DETECT_MODE_SIMPLE
                else -> null
            }
            if (mode == null) {
                Log.d(
                    TAG,
                    "[camera-trace] face AE unsupported (modes=${modes?.contentToString()} maxFaceCount=$maxFaces)",
                )
                return@runCatching
            }
            val options = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(CaptureRequest.STATISTICS_FACE_DETECT_MODE, mode)
                .build()
            Camera2CameraControl.from(camera.cameraControl).addCaptureRequestOptions(options)
            Log.d(
                TAG,
                "[camera-trace] face AE enabled mode=$mode modes=${modes?.contentToString()} maxFaceCount=$maxFaces",
            )
        }.onFailure {
            // 老设备/厂商定制可能不支持 Camera2Interop：降级为纯 auto，不影响出数。
            Log.w(TAG, "[camera-trace] face AE 开启失败，保持纯 auto：${it.message}")
        }
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
        camera = null
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
    }

    /** 彻底释放。页面销毁时调用。 */
    fun release() {
        Log.d(TAG, "[camera-trace] release()")
        unbindOwn("release")
        provider = null
        bound = false
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