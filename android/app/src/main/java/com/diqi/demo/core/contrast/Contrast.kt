package com.diqi.demo.core.contrast

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * WCAG 2.1 相对亮度与对比度计算。
 *
 * 需求 FR-15 要求「对比度 ≥ 4.5:1」。这条标准在 V1.0 计划里没有任何归属
 * （评审指出全文 0 命中），本类 + `ContrastTest` 就是它的载体：
 * 把「设计稿上看着还行」变成「构建期必然通过的可计算断言」。
 *
 * 阈值（WCAG AA）：
 *  · 正文（< 18pt 常规 / < 14pt 加粗）→ 4.5:1
 *  · 大字号（≥ 18pt 常规 / ≥ 14pt 加粗）→ 3:1
 *  · 非文本 UI 边界 → 3:1
 */
object Contrast {

    /** 正文最小对比度 */
    const val AA_NORMAL = 4.5
    /** 大字号 / 非文本 UI 最小对比度 */
    const val AA_LARGE = 3.0

    private fun channelLuminance(component: Float): Double {
        val c = component.toDouble()
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    /**
     * 相对亮度。半透明前景需先与背景做 alpha 合成（见 [composite]），
     * 否则算出来的是不存在的颜色。
     */
    fun relativeLuminance(color: Color): Double {
        val r = channelLuminance(color.red)
        val g = channelLuminance(color.green)
        val b = channelLuminance(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /** 对比度比值，范围 1.0–21.0。 */
    fun ratio(foreground: Color, background: Color): Double {
        val l1 = relativeLuminance(foreground)
        val l2 = relativeLuminance(background)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    /** 把带 alpha 的前景按 source-over 合成到不透明背景上。 */
    fun composite(foreground: Color, background: Color): Color {
        val a = foreground.alpha.toDouble()
        val r = foreground.red * a + background.red * (1 - a)
        val g = foreground.green * a + background.green * (1 - a)
        val b = foreground.blue * a + background.blue * (1 - a)
        return Color(r.toFloat(), g.toFloat(), b.toFloat(), 1f)
    }

    /** 半透明前景的真实对比度（先合成再计算）。 */
    fun ratioComposite(foreground: Color, background: Color): Double =
        ratio(composite(foreground, background), background)

    fun passesNormal(foreground: Color, background: Color): Boolean =
        ratio(foreground, background) >= AA_NORMAL

    fun passesLarge(foreground: Color, background: Color): Boolean =
        ratio(foreground, background) >= AA_LARGE
}