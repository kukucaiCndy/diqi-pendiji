package com.diqi.demo.sensor.camera

import android.graphics.Bitmap
import kotlin.math.ceil
import kotlin.math.floor

/**
 * 把裁剪后的人脸区域缩放为 FacePhys 要求的 **9×9×3 RGB** 字节数组。
 *
 * 这是需求 §11 列明的高风险点之一（「9×9×3 ROI 转换错误 → 始终无指标」），
 * 所以核心逻辑写成**不依赖 Android Bitmap 的纯函数** [toRoiBytes]，
 * 由 `RoiConverterTest` 的 golden vector 钉死；Bitmap 只是一个薄包装。
 *
 * 契约（与 `:facephys-api` 的 `RppgEngine.ROI_BYTE_COUNT` 对齐）：
 *  · 输出长度恒为 243 = 9 × 9 × 3
 *  · 通道顺序 RGB 交错（R,G,B,R,G,B,...）
 *  · 每个通道一个**无符号**字节（0–255 映射到 Kotlin Byte 的 -128–127）
 *  · 每个格子取该格覆盖区域的平均值（盒式滤波），而不是最近邻 ——
 *    最近邻会丢掉人脸区域内的微小血容量变化，正是 rPPG 需要的那点信号
 *
 * ⚠️ 若真实 SDK 要求 BGR 或平面（planar）布局，只需改 [toRoiBytes]
 * 并同步更新 golden vector 期望值。
 */
object RoiConverter {

    const val GRID = 9
    const val CHANNELS = 3
    const val BYTE_COUNT = GRID * GRID * CHANNELS // 243

    /**
     * @param argb 源像素，ARGB_8888 打包为 Int（`Color` 的 `toArgb()` 形式）
     * @param width 源宽（>0）
     * @param height 源高（>0）
     * @return 243 字节，RGB 交错
     */
    fun toRoiBytes(argb: IntArray, width: Int, height: Int): ByteArray {
        require(width > 0 && height > 0) { "width/height 必须为正: ${width}x$height" }
        require(argb.size >= width * height) {
            "像素数组长度 ${argb.size} 小于 ${width}x$height"
        }

        val out = ByteArray(BYTE_COUNT)
        var index = 0
        for (gy in 0 until GRID) {
            // 用 floor/ceil 保证窄源图（例如 5px 宽）时每格至少覆盖 1 个像素
            val y0 = floor(gy.toDouble() * height / GRID).toInt().coerceIn(0, height - 1)
            val y1 = ceil((gy + 1).toDouble() * height / GRID).toInt().coerceIn(y0 + 1, height)
            for (gx in 0 until GRID) {
                val x0 = floor(gx.toDouble() * width / GRID).toInt().coerceIn(0, width - 1)
                val x1 = ceil((gx + 1).toDouble() * width / GRID).toInt().coerceIn(x0 + 1, width)

                var sumR = 0L
                var sumG = 0L
                var sumB = 0L
                var count = 0
                for (y in y0 until y1) {
                    val rowBase = y * width
                    for (x in x0 until x1) {
                        val px = argb[rowBase + x]
                        sumR += (px shr 16) and 0xFF
                        sumG += (px shr 8) and 0xFF
                        sumB += px and 0xFF
                        count++
                    }
                }
                out[index++] = (sumR / count).toByte()
                out[index++] = (sumG / count).toByte()
                out[index++] = (sumB / count).toByte()
            }
        }
        return out
    }

    /**
     * 从 Bitmap 裁剪并缩放。相机帧链路调用此重载。
     *
     * **必须 clamp**：ML Kit 在人脸贴边时可能给出越界矩形（甚至负坐标），
     * 直接透传给 `getPixels` 会抛 `IllegalArgumentException`，
     * 而这里是相机分析线程 —— 异常会直接把分析线程打死，
     * 表现为 AC-18「连续进出训练页偶发崩溃」。宁可裁出空区域也不能崩。
     */
    fun toRoiBytes(bitmap: Bitmap, left: Int, top: Int, right: Int, bottom: Int): ByteArray {
        val bmpW = bitmap.width
        val bmpH = bitmap.height
        if (bmpW <= 0 || bmpH <= 0) return ByteArray(BYTE_COUNT)

        val l = left.coerceIn(0, bmpW - 1)
        val t = top.coerceIn(0, bmpH - 1)
        val r = right.coerceIn(l + 1, bmpW)
        val b = bottom.coerceIn(t + 1, bmpH)

        val w = r - l
        val h = b - t
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, l, t, w, h)
        return toRoiBytes(pixels, w, h)
    }
}