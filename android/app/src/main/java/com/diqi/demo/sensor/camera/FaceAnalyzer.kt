package com.diqi.demo.sensor.camera

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.facephys.rppg.FrameResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.atomic.AtomicBoolean

/** 一帧分析的结果，供校准页与质量判定使用。 */
data class FaceFrame(
    /** 是否检出人脸 */
    val faceFound: Boolean,
    /** 人脸占画面比例（0–1），需求要求 20%–60% 为合适 */
    val faceRatio: Float = 0f,
    /** 平均亮度（0–255）*/
    val brightness: Int = 0,
    /** 投喂结果（仅当确实投喂了 ROI 时有意义）*/
    val feedResult: FrameResult? = null,
    /** ROI 字节数（恒为 243，用于日志与自检）*/
    val roiBytes: Int = 0,
    /** 人脸中心相对画面中心的偏移（-1..1），用于"请靠近/后退"提示 */
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    /** 人脸已连续丢失的毫秒数（0 表示在框内）。由分析器逐帧真实累计。 */
    val faceLostMs: Long = 0L,
)

/**
 * 相机帧分析器：CameraX `ImageAnalysis` → ML Kit 人脸 → 裁剪 9×9×3 ROI → 投喂 SDK。
 *
 * ## 三个必须做对的点（都是需求 §8.3 明确写的约束）
 *
 * 1. **单线程串行**：`processFrame` 并发会失败，所以分析器固定跑在单线程执行器上，
 *    并用 [busy] 做二次防抖（CameraX 已配 `STRATEGY_KEEP_ONLY_LATEST`，丢弃过期帧）。
 * 2. **ImageProxy 必须关闭**：不关会耗尽缓冲区，表现为"画面卡住"或相机异常（AC-18）。
 * 3. **镜像**：前置相机画面是镜像的，ROI 若按镜像坐标裁剪，与真实人脸左右相反。
 *    需求只要求 9×9 网格的肤色/血容量分布，左右镜像不影响统计特性，
 *    但**会**影响"人脸中心偏移"的方向提示。所以这里对 offsetX 取反，
 *    让"往左挪"的提示方向与用户的直觉一致。
 */
class FaceAnalyzer(
    /** 投喂回调。返回 SDK 的结果，便于校准页展示失败原因。 */
    private val onFeed: (roi: ByteArray, dtSec: Float) -> FrameResult,
    private val onFrame: (FaceFrame) -> Unit,
) : ImageAnalysis.Analyzer {

    private val detector: FaceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            // 12fps 就够了（需求 §5.4）；FAST 模式更省电
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build(),
    )

    private val busy = AtomicBoolean(false)
    private var lastFrameAtMs = 0L

    /** 最近一次"检到人脸"的时刻（单调时钟）。用于真实累计 faceLostMs。 */
    @Volatile
    private var lastFaceSeenAtMs = 0L

    /** 分析器启动后的首帧时刻 */
    @Volatile
    private var firstFrameAtMs = 0L

    @Volatile
    private var released = false

    override fun analyze(image: ImageProxy) {
        if (released || !busy.compareAndSet(false, true)) {
            image.close()
            return
        }

        try {
            val now = android.os.SystemClock.elapsedRealtime()
            val dtSec = if (lastFrameAtMs == 0L) {
                0.083f
            } else {
                ((now - lastFrameAtMs) / 1000f).coerceAtMost(0.5f).coerceAtLeast(0.01f)
            }
            lastFrameAtMs = now

            val bitmap = image.toBitmap()
            val input = InputImage.fromBitmap(bitmap, image.imageInfo.rotationDegrees)

            detector.process(input)
                .addOnSuccessListener { faces -> handleFaces(bitmap, faces, dtSec) }
                .addOnFailureListener { e ->
                    Log.w(TAG, "人脸检测失败", e)
                    onFrame(FaceFrame(faceFound = false))
                }
                .addOnCompleteListener {
                    busy.set(false)
                    image.close()
                }
        } catch (e: Exception) {
            Log.w(TAG, "帧处理异常", e)
            busy.set(false)
            image.close()
        }
    }

    private fun handleFaces(bitmap: Bitmap, faces: List<Face>, dtSec: Float) {
        // 取**最大人脸**：多人入镜时以最近的那个人为准（需求 §5.2 FR-07）
        val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
        if (face == null) {
            val now = android.os.SystemClock.elapsedRealtime()
            // 首次丢失时记下起点；持续丢失则累计
            val lostMs = if (lastFaceSeenAtMs == 0L) {
                // 从来没检到过脸：从开始分析算起
                if (firstFrameAtMs == 0L) firstFrameAtMs = now
                now - firstFrameAtMs
            } else {
                now - lastFaceSeenAtMs
            }
            onFrame(
                FaceFrame(
                    faceFound = false,
                    brightness = averageBrightness(bitmap),
                    faceLostMs = lostMs,
                ),
            )
            return
        }

        // 检到脸：重置丢失计时
        lastFaceSeenAtMs = android.os.SystemClock.elapsedRealtime()

        val box = face.boundingBox
        val frameArea = (bitmap.width * bitmap.height).toFloat().coerceAtLeast(1f)
        val faceArea = (box.width() * box.height()).toFloat()
        val ratio = (faceArea / frameArea).coerceIn(0f, 1f)

        val cx = box.exactCenterX() / bitmap.width - 0.5f
        val cy = box.exactCenterY() / bitmap.height - 0.5f

        // 裁剪并缩放为 9×9×3。RoiConverter 内部已 clamp 越界矩形，
        // 人脸贴边时不会把分析线程打死（AC-18）。
        val roi = RoiConverter.toRoiBytes(bitmap, box.left, box.top, box.right, box.bottom)
        val result = onFeed(roi, dtSec)

        onFrame(
            FaceFrame(
                faceFound = true,
                faceRatio = ratio,
                brightness = averageBrightness(bitmap),
                feedResult = result,
                roiBytes = roi.size,
                // 前置镜像是左右翻转的，提示方向要取反才符合用户直觉
                offsetX = -cx,
                offsetY = cy,
                faceLostMs = 0L,
            ),
        )
    }

    /** 抽样估算亮度：整图逐像素太贵（12fps 下每帧都要算），按 16 像素步长抽样。 */
    private fun averageBrightness(bitmap: Bitmap): Int {
        val step = 16
        var sum = 0L
        var count = 0
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val p = bitmap.getPixel(x, y)
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                sum += (r * 299 + g * 587 + b * 114) / 1000
                count++
                x += step
            }
            y += step
        }
        return if (count == 0) 0 else (sum / count).toInt()
    }

    fun release() {
        released = true
        runCatching { detector.close() }
    }

    private companion object {
        const val TAG = "FaceAnalyzer"
    }
}