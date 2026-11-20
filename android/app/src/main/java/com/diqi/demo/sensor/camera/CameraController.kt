package com.diqi.demo.sensor.camera

import android.content.Context
import android.util.Log
import android.view.ViewGroup
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
 * 3. **必须 unbind**：页面销毁时 `unbindAll()` + 关执行器。
 *    不做会表现为"连续进出后相机打不开"（AC-18）。
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

    /** 相机不可用的原因（可解释，供 UI 展示 —— FR-03 要求失败原因必须能说清）*/
    var lastError: String? = null
        private set

    /**
     * 启动预览并把帧送给分析器。
     *
     * @param previewView 预览控件；为 null 时只跑分析（训练页不需要预览时用）
     */
    fun start(previewView: PreviewView?) {
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

        val preview = Preview.Builder().build().also { p ->
            previewView?.let { p.surfaceProvider = it.surfaceProvider }
        }

        val analysis = ImageAnalysis.Builder()
            // 12fps（需求 §5.4）；KEEP_ONLY_LATEST 保证不排队
            .setTargetResolution(android.util.Size(640, 480))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(executor, analyzer) }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                analysis,
            )
            bound = true
            lastError = null
        } catch (e: Exception) {
            lastError = describe(e)
            Log.e(TAG, "绑定用例失败", e)
        }
    }

    /** 停止投喂但保留 provider（页面暂停/锁屏时调用）。 */
    fun stop() {
        runCatching { provider?.unbindAll() }
        bound = false
    }

    /** 彻底释放。页面销毁时调用。 */
    fun release() {
        runCatching { provider?.unbindAll() }
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