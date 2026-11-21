package com.diqi.demo.ui.screens.training

import androidx.compose.ui.geometry.Offset

/** 曲线横向槽位数：60 个 1Hz 采样位置，最新点落在最右一槽。 */
internal const val CURVE_SLOTS = 60

/** 竖直归一化的容差：range 小于它就当常量处理，画水平中线。 */
private const val EPS = 1e-4f

/**
 * 把 1Hz 历史读数映射成曲线上的一组点（**纯函数**，便于 JVM 单测）。
 *
 * ## 映射规则
 *
 * - **从右往左生长**：最新的点贴右缘 `x = width`，越旧越往左，
 *   相邻点水平间距 `step = width / (CURVE_SLOTS - 1)`（即 `width / 59`）。
 *   这样历史不足 60 点时曲线只占右侧一段，不会横向拉伸成"假满屏"。
 * - **纵向按 min–max 归一化到半高**：值域映射到画布中间一半高度
 *   （`y ∈ [height/4, 3·height/4]`），既不会顶到边界，也不浪费卡片高度。
 * - **range ≈ 0（常量）时画水平中线**：全等值时不存在"趋势"，
 *   画中线比放大噪声诚实。
 *
 * ## 为什么曲线是台阶状的（不要当 bug）
 *
 * SDK 指标约 **10 秒**刷新一次，而我们每秒采一次样 —— 同一个读数会被采到
 * 约 10 个点，于是曲线出现平台。那是**真实读数窗口**，不是采样出错。
 * 这里刻意**不插值、不平滑**：插值出来的曲线是编造的生理波形。
 *
 * @param history 1Hz 采样历史，最新的在末尾
 * @param width   曲线区宽度（px）
 * @param height  曲线区高度（px）
 */
internal fun normalizeCurvePoints(
    history: List<Float>,
    width: Float,
    height: Float,
): List<Offset> {
    if (history.isEmpty() || width <= 0f || height <= 0f) return emptyList()

    val center = height / 2f
    val band = height / 2f // 值域占竖向"半高"
    val step = if (CURVE_SLOTS > 1) width / (CURVE_SLOTS - 1) else width

    val min = history.min()
    val max = history.max()
    val range = max - min

    return history.mapIndexed { index, value ->
        // 最新点（末尾）贴右缘，逐点向左退
        val x = width - (history.lastIndex - index) * step
        val y = if (range < EPS) {
            center
        } else {
            val norm = (value - min) / (range + EPS)
            center + (0.5f - norm) * band
        }
        Offset(x, y)
    }
}
