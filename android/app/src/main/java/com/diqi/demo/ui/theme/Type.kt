package com.diqi.demo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * HerRhythm DS v1.0 · Typography —— **1 设计 px = 1sp**
 *
 * ## 换算规则（唯一来源：`design/desing.pen`）
 *
 * 用户明确选择「忠于设计」：设计稿里的 px 字数直接当 sp 用，不做放大。
 * 这是对原决策 D6（正文 ≥20sp）的**正式推翻**，理由与代价都写在这里：
 *
 * - 设计稿实测最常用正文是 **13px**（`W7XDia` 37 页统计出现 146 次）。
 *   若按 FR-15「正文 ≥20sp」执行需全局 ×1.54，30px 主标题会变成 46sp，
 *   一屏放不下设计稿内容 —— 那已经不是同一套设计语言。
 * - 用户选择以设计稿为准，因此 **FR-15 的「正文 ≥20sp」在本项目不成立**，
 *   AC-09 的字号判据需同步改为「1 设计 px = 1sp 且 2.0× 系统字号不破版」。
 *
 * ## 每个 token 的出处（都是设计稿里真实出现过的字号）
 *
 * | token | sp | 设计稿用途 |
 * | --- | --- | --- |
 * | display | 34 | 大号标题（出现 5 次） |
 * | headline | 30 | 页面主标题（P02「今天练一次吧」） |
 * | headlineSm | 28 | 次级主标题（12 次） |
 * | question | 24 | 问句（P01-b 筛查） |
 * | titleLarge | 22 | 卡片主文（P02-a「8 次 · 约 3 分钟」） |
 * | titleMedium / titleSmall | 17 / 16 | 组标题 |
 * | bodyLarge | 15 | 次级正文（49 次） |
 * | bodyMedium / bodySmall | 13 | **最常用正文（146 次）** |
 * | button | 17 | 按钮文字（33 次） |
 * | label / labelSmall | 14 / 12 | 区块标题 / 卡标题 |
 * | micro | 11 | 星期字、微标签（119 次） |
 * | monoDisplay | 34 | 训练倒计时 |
 * | monoMetric | 28 | 指标读数 |
 * | disclaimer | 11 | 免责小字 |
 *
 * D8 不变：不引入字体资产，中文走系统 Sans、读数字走 Monospace。
 */

private val Mono = FontFamily.Monospace
private val Sans = FontFamily.SansSerif

/** 中文行高偏紧会在放大字号后互相挤压，统一给到 1.35–1.45。 */
private fun style(
    size: Int,
    weight: FontWeight,
    lineHeightRatio: Float,
    family: FontFamily = Sans,
    letterSpacing: Float = 0f,
): TextStyle = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = (size * lineHeightRatio).sp,
    letterSpacing = letterSpacing.sp,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
)

object HrType {

    // ── Display ────────────────────────────────────────────────────────────
    /** P01 首屏主标题 */
    val display = style(34, FontWeight.Bold, 1.25f)
    /** P02 首页主标题、P04 阶段名 */
    val headline = style(30, FontWeight.Bold, 1.28f)
    /** 校准页主提示（设计 28/700）*/
    val headlineSm = style(28, FontWeight.Bold, 1.3f)
    /** 筛查题干（设计 23/700）*/
    val question = style(24, FontWeight.Bold, 1.35f)

    // ── Title ──────────────────────────────────────────────────────────────
    /** 卡片主文（设计 22/700）*/
    val titleLarge = style(22, FontWeight.Bold, 1.35f)
    /** 区块标题（设计 20/700）*/
    val titleMedium = style(17, FontWeight.Bold, 1.35f)
    /** 卡片标题（设计 14–16/700，正文语义故抬到 20）*/
    val titleSmall = style(16, FontWeight.Bold, 1.35f)

    // ── Body（正文，≥ 20sp 硬约束）───────────────────────────────────────
    /** 正文主用 */
    val bodyLarge = style(15, FontWeight.Normal, 1.42f)
    /** 正文 */
    val bodyMedium = style(13, FontWeight.Normal, 1.42f)
    /** 正文·加粗（提示行、选项）*/
    val bodyMediumBold = style(13, FontWeight.Bold, 1.42f)
    /** 正文次级（辅助说明、免责；仍是正文语义，20sp）*/
    val bodySmall = style(13, FontWeight.Normal, 1.42f)

    // ── Label（非正文：标签、按钮字、胶囊）──────────────────────────────
    /**
     * 按钮文字。
     *
     * 设计稿 17/700。首版取 19 属于 D6 三档（20/18/14–15）之外的"野值"，
     * 无人裁定；而按钮文字在适老化语境下是被反复扫视的目标，
     * 按 FR-15 的正文下限给到 20sp。实测 332dp 可用宽下 7 个汉字约 140dp，空间充裕。
     */
    val button = style(17, FontWeight.Bold, 1.2f)
    /** 按钮文字·次要 */
    val buttonSecondary = style(17, FontWeight.Medium, 1.2f)
    /** 小节标签（设计 13–14/700）*/
    val label = style(14, FontWeight.Bold, 1.25f)
    /** 弱标签（设计 12–13/600）*/
    val labelSmall = style(12, FontWeight.SemiBold, 1.25f)
    /** 胶囊文字（设计 13/600）*/
    val capsule = style(13, FontWeight.SemiBold, 1.2f)
    /** 脚注（设计 12/400）*/
    val footnote = style(12, FontWeight.Normal, 1.35f)
    /** 微标（设计 10–11/600），仅用于非关键辅助信息 */
    val micro = style(11, FontWeight.SemiBold, 1.25f)

    // ── Mono 数字（关键数字 ≥ 32sp 硬约束）────────────────────────────────
    /** 训练页倒计时（设计 34/600）*/
    val monoDisplay = style(34, FontWeight.SemiBold, 1.1f, Mono)
    /** 指标数值（设计 28–30/600，按要求 ≥32）*/
    val monoMetric = style(28, FontWeight.SemiBold, 1.1f, Mono)
    /** 中等数字（完成页统计）*/
    val monoMedium = style(22, FontWeight.SemiBold, 1.15f, Mono)
    /** 小号等宽（时长标签）*/
    val monoSmall = style(15, FontWeight.Medium, 1.2f, Mono)
    /** debug 微标（仅 debug build）*/
    val monoDebug = style(9, FontWeight.Normal, 1.2f, Mono)

    /**
     * 免责声明。
     *
     * 设计稿 10–11px，但那既过不了「正文 ≥20sp」（FR-15），也过不了对比度。
     * 免责文案是需求 §5.6 强制常驻的**正文**，不能靠缩小字号来"不抢注意力"。
     * 改用 20sp + 弱色（[com.diqi.demo.ui.theme.HrColor.InkSecondary]）来降低视觉权重。
     */
    val disclaimer = style(11, FontWeight.Normal, 1.45f)
}

/** 供 MaterialTheme 使用的映射。仅填充实际会用到的槽位。 */
internal val HerRhythmTypography = Typography(
    displayLarge = HrType.display,
    displayMedium = HrType.headline,
    headlineLarge = HrType.headline,
    headlineMedium = HrType.headlineSm,
    headlineSmall = HrType.question,
    titleLarge = HrType.titleLarge,
    titleMedium = HrType.titleMedium,
    titleSmall = HrType.titleSmall,
    bodyLarge = HrType.bodyLarge,
    bodyMedium = HrType.bodyMedium,
    bodySmall = HrType.bodySmall,
    labelLarge = HrType.button,
    labelMedium = HrType.label,
    labelSmall = HrType.capsule,
)