package com.diqi.demo.core.contrast

import androidx.compose.ui.graphics.Color
import com.diqi.demo.ui.theme.HrColor
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FR-15「对比度 ≥ 4.5:1」的构建期强制手段。
 *
 * 评审指出：V1.0 计划里「对比度」全文 0 命中，文档侧和计划侧都无人验证这条硬标准。
 * 这个测试就是它的归属：把设计稿里"看着还行"的配色变成可计算断言。
 *
 * 覆盖原则：**只断言 UI 中真实搭配使用的前景/背景对**。
 * 不存在的搭配不断言（否则会为了过测试而乱改颜色）。
 *
 * 实测发现并已在 [HrColor] 里修正的三处缺陷：
 *   1. `InkTertiary #94A3B8` 白底仅 2.56:1 → 降级为"非文本专用"，新增 `WeakText`
 *   2. `TimelineLabel #5E7386` 深底 3.58:1 → 抬到 5.68:1
 *   3. `TimelineDuration #5F7285` 深底 3.55:1 → 抬到 5.68:1
 */
class ContrastTest {

    private data class Pair(
        val name: String,
        val fg: Color,
        val bg: Color,
        val minRatio: Double,
    )

    /** 正文级配对：必须 ≥ 4.5:1 */
    private val bodyPairs = listOf(
        Pair("Ink 正文/卡片", HrColor.Ink, HrColor.Surface, Contrast.AA_NORMAL),
        Pair("Ink 正文/页面底", HrColor.Ink, HrColor.Canvas, Contrast.AA_NORMAL),
        Pair("Ink 正文/提示卡", HrColor.Ink, HrColor.SubtleContainer, Contrast.AA_NORMAL),
        Pair("InkSecondary/卡片", HrColor.InkSecondary, HrColor.Surface, Contrast.AA_NORMAL),
        Pair("InkSecondary/页面底", HrColor.InkSecondary, HrColor.Canvas, Contrast.AA_NORMAL),
        Pair("InkSecondary/提示卡", HrColor.InkSecondary, HrColor.SubtleContainer, Contrast.AA_NORMAL),
        Pair("WeakText/卡片", HrColor.WeakText, HrColor.Surface, Contrast.AA_NORMAL),
        Pair("WeakText/页面底", HrColor.WeakText, HrColor.Canvas, Contrast.AA_NORMAL),
        Pair("WeakText/提示卡", HrColor.WeakText, HrColor.SubtleContainer, Contrast.AA_NORMAL),
        Pair("WeakText/诊断底", HrColor.WeakText, HrColor.FaintContainer, Contrast.AA_NORMAL),
        Pair("Slate800/提示卡", HrColor.Slate800, HrColor.SubtleContainer, Contrast.AA_NORMAL),
        Pair("Slate700/提示卡", HrColor.Slate700, HrColor.SubtleContainer, Contrast.AA_NORMAL),
        Pair("Brand 品牌字/卡片", HrColor.Brand, HrColor.Surface, Contrast.AA_NORMAL),
        Pair("Brand 品牌字/页面底", HrColor.Brand, HrColor.Canvas, Contrast.AA_NORMAL),
        Pair("Brand/隐私卡", HrColor.Brand, HrColor.SubtleContainer, Contrast.AA_NORMAL),
        Pair("Brand/就绪底", HrColor.Brand, HrColor.GoodContainer, Contrast.AA_NORMAL),
        Pair("Good 就绪字/就绪底", HrColor.Good, HrColor.GoodContainer, Contrast.AA_NORMAL),
        Pair("Good 就绪字/卡片", HrColor.Good, HrColor.Surface, Contrast.AA_NORMAL),
        Pair("GoodDeep/选中选项底", HrColor.GoodDeep, HrColor.GoodContainer, Contrast.AA_NORMAL),
        Pair("Warn 提示字/提示底", HrColor.Warn, HrColor.WarnContainer, Contrast.AA_NORMAL),
        Pair("Warn 提示字/页面底", HrColor.Warn, HrColor.Canvas, Contrast.AA_NORMAL),
        Pair("Warn 提示字/卡片", HrColor.Warn, HrColor.Surface, Contrast.AA_NORMAL),
        Pair("Danger/危险底", HrColor.Danger, HrColor.DangerContainer, Contrast.AA_NORMAL),
        Pair("Danger/页面底", HrColor.Danger, HrColor.Canvas, Contrast.AA_NORMAL),
        Pair("Danger/卡片", HrColor.Danger, HrColor.Surface, Contrast.AA_NORMAL),

        // 指标值可能出现在三种底色上：卡片、页面底、提示卡。
        // 首轮只断言了卡片，导致「专门证明 FR-15 的工具证明了另一个底色」。
        Pair("心率值/卡片", HrColor.HeartRate, HrColor.Surface, Contrast.AA_NORMAL),
        Pair("心率值/页面底", HrColor.HeartRate, HrColor.Canvas, Contrast.AA_NORMAL),
        Pair("心率值/提示卡", HrColor.HeartRate, HrColor.SubtleContainer, Contrast.AA_NORMAL),
        Pair("呼吸值/卡片", HrColor.BreathingRate, HrColor.Surface, Contrast.AA_NORMAL),
        Pair("呼吸值/页面底", HrColor.BreathingRate, HrColor.Canvas, Contrast.AA_NORMAL),
        Pair("呼吸值/提示卡", HrColor.BreathingRate, HrColor.SubtleContainer, Contrast.AA_NORMAL),

        // 演示模式水印（AC-19 要求"全屏水印"，必须读得清才算水印）
        Pair("演示水印字/水印底", HrColor.DemoWatermark, HrColor.DemoWatermarkContainer, Contrast.AA_NORMAL),

        // 深色面板（P04 训练页）
        Pair("白字/深面板", HrColor.Surface, HrColor.Panel, Contrast.AA_NORMAL),
        Pair("白字/深面板次层", HrColor.Surface, HrColor.PanelSecond, Contrast.AA_NORMAL),
        Pair("金句强调/深面板", HrColor.Accent, HrColor.Panel, Contrast.AA_NORMAL),
        Pair("金句强调/深面板次层", HrColor.Accent, HrColor.PanelSecond, Contrast.AA_NORMAL),
        Pair("深底强调上的字", HrColor.OnAccent, HrColor.Accent, Contrast.AA_NORMAL),
        Pair("面板次文字/深面板", HrColor.PanelTextMuted, HrColor.Panel, Contrast.AA_NORMAL),
        Pair("面板次文字/深面板次层", HrColor.PanelTextMuted, HrColor.PanelSecond, Contrast.AA_NORMAL),
        Pair("时间轴阶段名/深面板", HrColor.TimelineName, HrColor.Panel, Contrast.AA_NORMAL),
        Pair("时间轴阶段名/深面板次层", HrColor.TimelineName, HrColor.PanelSecond, Contrast.AA_NORMAL),
        Pair("时间轴小标题/深面板", HrColor.TimelineLabel, HrColor.Panel, Contrast.AA_NORMAL),
        Pair("时间轴小标题/深面板次层", HrColor.TimelineLabel, HrColor.PanelSecond, Contrast.AA_NORMAL),
        Pair("时间轴时长/深面板", HrColor.TimelineDuration, HrColor.Panel, Contrast.AA_NORMAL),
        Pair("时间轴时长/深面板次层", HrColor.TimelineDuration, HrColor.PanelSecond, Contrast.AA_NORMAL),
    )

    /**
     * 非文本 UI 配对：必须 ≥ 3:1。
     *
     * 严格按 FR-15 原文「对比度 ≥4.5:1」执行 —— 不给文本开大字号 3:1 的口子，
     * 因为需求没有豁免条款。3:1 只用于**确实不是文字**的东西（进度轨、状态点）。
     */
    private val uiPairs = listOf(
        Pair("进度轨填充/进度轨", HrColor.Brand, HrColor.Line, Contrast.AA_LARGE),
        Pair("进度轨填充/轨道深", HrColor.Brand, HrColor.Track, Contrast.AA_LARGE),
        Pair("时间轴已用节点/深面板", HrColor.Good, HrColor.Panel, Contrast.AA_LARGE),
        Pair("时间轴未到节点/深面板", HrColor.TimelinePending, HrColor.Panel, Contrast.AA_LARGE),
    )

    @Test
    fun `所有正文配对满足 WCAG AA 4_5_to_1`() {
        val failures = bodyPairs.mapNotNull { p ->
            val r = Contrast.ratio(p.fg, p.bg)
            if (r < p.minRatio) {
                "${p.name}: ${"%.2f".format(r)}:1 < ${p.minRatio}:1"
            } else null
        }
        assertTrue(
            "以下正文配色未达 FR-15 的 4.5:1 要求，必须修正 HrColor 中的 token 值：\n" +
                failures.joinToString("\n"),
            failures.isEmpty(),
        )
    }

    @Test
    fun `所有非文本与大字号配对满足 WCAG AA 3_to_1`() {
        val failures = uiPairs.mapNotNull { p ->
            val r = Contrast.ratio(p.fg, p.bg)
            if (r < p.minRatio) "${p.name}: ${"%.2f".format(r)}:1 < ${p.minRatio}:1" else null
        }
        assertTrue(
            "以下非文本配色未达 3:1：\n" + failures.joinToString("\n"),
            failures.isEmpty(),
        )
    }

    /**
     * 负向断言：把 `InkTertiary` 的不可用性写成测试。
     *
     * 如果有人哪天把它挪去写正文，这里不会失败（它本来就不该被用在正文），
     * 但这个测试会明确记录"它为什么不能当文字色"，防止无意识的回退。
     */
    @Test
    fun `InkTertiary 确实不能承载正文文字 —— 记录其真实比值`() {
        val onSurface = Contrast.ratio(HrColor.InkTertiary, HrColor.Surface)
        val onCanvas = Contrast.ratio(HrColor.InkTertiary, HrColor.Canvas)
        assertTrue(
            "InkTertiary 白底 ${"%.2f".format(onSurface)}:1 —— 低于 4.5，只能用于非文本",
            onSurface < Contrast.AA_NORMAL,
        )
        assertTrue(
            "InkTertiary 页面底 ${"%.2f".format(onCanvas)}:1 —— 低于 4.5，只能用于非文本",
            onCanvas < Contrast.AA_NORMAL,
        )
    }

    @Test
    fun `WeakText 是同时满足两种底色的最浅可行值`() {
        assertTrue(Contrast.passesNormal(HrColor.WeakText, HrColor.Surface))
        assertTrue(Contrast.passesNormal(HrColor.WeakText, HrColor.Canvas))
        // 比它更浅一档（设计稿在用的 #64748B）在页面底上不合格，证明这个下限不是随手取的
        val tooLight = Color(0xFF64748B)
        assertTrue(
            "#64748B 在页面底上应低于 4.5:1，否则 WeakText 取值可以再浅",
            !Contrast.passesNormal(tooLight, HrColor.Canvas),
        )
    }

    @Test
    fun `半透明覆盖合成后的对比度按真实合成色计算`() {
        // 相机角标底是 70% 的深色叠在视频画面上，白字必须仍达标
        val chip = HrColor.ScrimChip
        val composited = Contrast.composite(chip, HrColor.PanelSecond)
        val r = Contrast.ratio(HrColor.Surface, composited)
        assertTrue(
            "相机角标白字在合成底上只有 ${"%.2f".format(r)}:1",
            r >= Contrast.AA_NORMAL,
        )
    }

    /**
     * 明确记录**不再断言**的几组配色，以及理由。
     *
     * 一个测试套件的价值不仅在于它断言了什么，也在于它诚实地划出了
     * "这里为什么不断言"。否则后人会以为覆盖完整。
     */
    @Test
    fun `装饰性元素不进入断言的清单与理由`() {
        // 1) 分隔线（Line / Hairline）：纯装饰，不承载信息。
        //    实测 Line/Canvas 1.14:1、Hairline/Panel 1.52:1 —— 远低于 3:1，
        //    但 WCAG 1.4.11 只约束"识别控件与状态所必需的视觉信息"，
        //    装饰性分割线不属于此列。**因此这两色不得单独承载状态**。
        assertTrue(
            "Line 不应达到 3:1，若达到了说明配色被改过，需要重新评估装饰性定位",
            Contrast.ratio(HrColor.Line, HrColor.Canvas) < Contrast.AA_LARGE,
        )
        assertTrue(
            "Hairline 不应达到 3:1",
            Contrast.ratio(HrColor.Hairline, HrColor.Panel) < Contrast.AA_LARGE,
        )

        // 2) 禁用态文字（InkTertiary）：WCAG 1.4.3 对禁用控件有豁免。
        //    已在 `InkTertiary 确实不能承载正文文字` 中单独记录。
        // 3) 调试微标（DebugGray）：仅 debug build 可见，不进发布包的用户界面。
    }

    @Test
    fun `对比度公式本身正确 —— 黑白为 21 且同色为 1`() {
        val blackWhite = Contrast.ratio(Color.Black, Color.White)
        assertTrue("黑白对比度应为 21:1，实际 ${"%.2f".format(blackWhite)}", blackWhite > 20.9)
        val same = Contrast.ratio(HrColor.Ink, HrColor.Ink)
        assertTrue("同色对比度应为 1:1，实际 ${"%.2f".format(same)}", same < 1.001)
    }
}