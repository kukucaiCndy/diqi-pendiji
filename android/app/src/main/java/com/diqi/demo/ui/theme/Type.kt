package com.diqi.demo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * HerRhythm DS v1.0 · Typography（决策 D6 / D8）
 *
 * 两条硬约束决定了这里的字阶：
 *
 * 1. **D6 字号以需求红线为准**：需求 FR-15 与 AC-09 要求「正文 ≥ 20sp、
 *    关键数字 ≥ 32sp」。设计稿 `desing.pen` 的正文字样是 13–16sp，与需求冲突；
 *    但该设计稿自身的「文案与合规红线」区块也写着「正文 ≥20sp」。
 *    故取需求为准：正文字阶从 20sp 起，视觉层级靠字重与颜色维持，不靠压字号。
 *
 * 2. **D8 不引入字体资产**：设计稿用 Inter + IBM Plex Mono，但两者都不覆盖
 *    中文，中文必然回退系统字体，引入等于白增体积。故中文走系统 Sans，
 *    数字读数走 [FontFamily.Monospace]（对应设计稿的 IBM Plex Mono 角色）。
 *
 * 字阶与设计稿「区块 02 字体 Typography」的对应关系：
 *   设计 37/700 → display（P01 主标题）
 *   设计 30/700 → headline（P02 主标题）
 *   设计 22/700 → titleLarge（卡片主文）
 *   设计 34/600 mono → monoDisplay（倒计时，关键数字 ≥32 ✓）
 *   设计 28–30/600 mono → monoMetric（指标值，按要求抬到 32 ✓）
 *   设计 15/400 → body（正文，按要求抬到 20 ✓）
 *   设计 13/600 → label（标签，非正文）
 *   设计 11/600 → micro（通道标）
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
    val display = style(37, FontWeight.Bold, 1.25f)
    /** P02 首页主标题、P04 阶段名 */
    val headline = style(30, FontWeight.Bold, 1.28f)
    /** 校准页主提示（设计 28/700）*/
    val headlineSm = style(28, FontWeight.Bold, 1.3f)
    /** 筛查题干（设计 23/700）*/
    val question = style(23, FontWeight.Bold, 1.35f)

    // ── Title ──────────────────────────────────────────────────────────────
    /** 卡片主文（设计 22/700）*/
    val titleLarge = style(22, FontWeight.Bold, 1.35f)
    /** 区块标题（设计 20/700）*/
    val titleMedium = style(20, FontWeight.Bold, 1.35f)
    /** 卡片标题（设计 14–16/700，正文语义故抬到 20）*/
    val titleSmall = style(20, FontWeight.Bold, 1.35f)

    // ── Body（正文，≥ 20sp 硬约束）───────────────────────────────────────
    /** 正文主用 */
    val bodyLarge = style(22, FontWeight.Normal, 1.42f)
    /** 正文 */
    val bodyMedium = style(20, FontWeight.Normal, 1.42f)
    /** 正文·加粗（提示行、选项）*/
    val bodyMediumBold = style(20, FontWeight.Bold, 1.42f)
    /** 正文次级（辅助说明、免责；仍是正文语义，20sp）*/
    val bodySmall = style(20, FontWeight.Normal, 1.42f)

    // ── Label（非正文：标签、按钮字、胶囊）──────────────────────────────
    /**
     * 按钮文字。
     *
     * 设计稿 17/700。首版取 19 属于 D6 三档（20/18/14–15）之外的"野值"，
     * 无人裁定；而按钮文字在适老化语境下是被反复扫视的目标，
     * 按 FR-15 的正文下限给到 20sp。实测 332dp 可用宽下 7 个汉字约 140dp，空间充裕。
     */
    val button = style(20, FontWeight.Bold, 1.2f)
    /** 按钮文字·次要 */
    val buttonSecondary = style(20, FontWeight.Medium, 1.2f)
    /** 小节标签（设计 13–14/700）*/
    val label = style(15, FontWeight.Bold, 1.25f)
    /** 弱标签（设计 12–13/600）*/
    val labelSmall = style(14, FontWeight.SemiBold, 1.25f)
    /** 胶囊文字（设计 13/600）*/
    val capsule = style(15, FontWeight.SemiBold, 1.2f)
    /** 脚注（设计 12/400）*/
    val footnote = style(14, FontWeight.Normal, 1.35f)
    /** 微标（设计 10–11/600），仅用于非关键辅助信息 */
    val micro = style(12, FontWeight.SemiBold, 1.25f)

    // ── Mono 数字（关键数字 ≥ 32sp 硬约束）────────────────────────────────
    /** 训练页倒计时（设计 34/600）*/
    val monoDisplay = style(36, FontWeight.SemiBold, 1.1f, Mono)
    /** 指标数值（设计 28–30/600，按要求 ≥32）*/
    val monoMetric = style(32, FontWeight.SemiBold, 1.1f, Mono)
    /** 中等数字（完成页统计）*/
    val monoMedium = style(26, FontWeight.SemiBold, 1.15f, Mono)
    /** 小号等宽（时长标签）*/
    val monoSmall = style(16, FontWeight.Medium, 1.2f, Mono)
    /** debug 微标（仅 debug build）*/
    val monoDebug = style(11, FontWeight.Normal, 1.2f, Mono)

    /**
     * 免责声明。
     *
     * 设计稿 10–11px，但那既过不了「正文 ≥20sp」（FR-15），也过不了对比度。
     * 免责文案是需求 §5.6 强制常驻的**正文**，不能靠缩小字号来"不抢注意力"。
     * 改用 20sp + 弱色（[com.diqi.demo.ui.theme.HrColor.InkSecondary]）来降低视觉权重。
     */
    val disclaimer = style(20, FontWeight.Normal, 1.45f)
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