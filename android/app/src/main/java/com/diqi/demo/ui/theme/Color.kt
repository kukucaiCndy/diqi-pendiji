package com.diqi.demo.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * HerRhythm DS v1.0 · Color Tokens
 *
 * 全部取自 `design/desing.pen` 的「设计系统 · HerRhythm DS v1.0 / 区块 01 色彩」，
 * token 名与设计稿一一对应，便于逐项比对。
 *
 * 对比度由 `ContrastTest` 强制：凡是在 UI 中真实搭配使用的前景/背景对，
 * 正文必须 ≥ 4.5:1，大字号（≥18sp 且加粗，或 ≥24sp）必须 ≥ 3:1（FR-15）。
 */
object HrColor {

    // ── 中性 ──────────────────────────────────────────────────────────────
    /** ink 主文字 */
    val Ink = Color(0xFF0F1A24)
    /** ink-2 次文字 */
    val InkSecondary = Color(0xFF5C6B7A)
    /**
     * ink-3 —— ⚠️ **仅限非文本元素**（图标、装饰点、分隔、禁用态）。
     *
     * 实测对比度：白底 2.56:1、canvas 底 2.37:1，**远低于 FR-15 的 4.5:1**。
     * 设计稿用它写 10–11px 的免责与标签，那是不可接受的。
     * 需要"弱文字"请用 [WeakText]，需要禁用态文字时此色可用
     * （WCAG 1.4.3 对禁用控件有豁免）。
     */
    val InkTertiary = Color(0xFF94A3B8)

    /**
     * 弱文字的真实下限色。
     *
     * 实测：白底 5.27:1、canvas 底 4.87:1 —— 是**同时**满足两种底色 ≥4.5:1
     * 的最浅可行值（再浅一档 #64748B 在 canvas 上降到 4.39:1 即不合格）。
     * 层级仍低于 [InkSecondary]（5.47:1），配合字号/字重可保持视觉层次。
     */
    val WeakText = Color(0xFF616D7B)
    /** line 分割线 */
    val Line = Color(0xFFE2E8F0)
    /** surface 卡片底 */
    val Surface = Color(0xFFFFFFFF)
    /** canvas 页面背景 */
    val Canvas = Color(0xFFF4F6F8)

    // ── 语义 ──────────────────────────────────────────────────────────────
    /**
     * HR 心率 —— 仅用于心率通道。
     *
     * 设计稿原值 #E11D48 实测白底 4.70:1（勉强过）、**canvas 底仅 4.34:1（不合格）**。
     * 指标值可能出现在任意底色上（训练页指标区、完成页、自检页），
     * 与其记住"只能在白底用"这种易忘的约束，不如把 token 本身降到两种底色都达标：
     * #D6193F → 白底 5.16:1、canvas 底 4.76:1，色相几乎无差别。
     */
    val HeartRate = Color(0xFFD6193F)
    /** RR 呼吸 —— 仅用于呼吸通道 */
    val BreathingRate = Color(0xFF2563EB)
    /** brand 主线 */
    val Brand = Color(0xFF0F766E)
    /** warn 提示 */
    val Warn = Color(0xFFB45309)
    /** good 正常 */
    val Good = Color(0xFF047857)
    /** danger 红线 */
    val Danger = Color(0xFFB91C1C)

    // ── 深色面板（P04 训练页、视频区）────────────────────────────────────
    /** panel 深色面板 */
    val Panel = Color(0xFF0F1A24)
    /** panel-2 面板次层 */
    val PanelSecond = Color(0xFF1B2A38)
    /** accent 深底强调（今日训练卡按钮、倒计时环）*/
    val Accent = Color(0xFF5EEAD4)
    /** on-accent 深底上的字 */
    val OnAccent = Color(0xFF062530)
    /** hairline 深底分割 */
    val Hairline = Color(0xFF243B4F)

    /** 深色面板上的次要文字（设计稿 卡副文 #8FA3B8）*/
    val PanelTextMuted = Color(0xFF8FA3B8)

    // ── 容器色（状态胶囊 / 提示条）──────────────────────────────────────
    /** 「摄像头工作中」「已就绪」底 */
    val GoodContainer = Color(0xFFE7F6EC)
    /** 「信号建立中」「光线不足」底 */
    val WarnContainer = Color(0xFFFEF3C7)
    /** 「信号弱」底 */
    val DangerContainer = Color(0xFFFEE2E2)
    /** 隐私卡、提示卡底 */
    val SubtleContainer = Color(0xFFF1F5F9)
    /** 诊断区底 */
    val FaintContainer = Color(0xFFF8FAFC)

    // ── 辅助 ──────────────────────────────────────────────────────────────
    /** 进度轨 / 未到节点底 */
    val Track = Color(0xFFCBD5E1)
    /** 正文深色（提示卡内文字）*/
    val Slate800 = Color(0xFF1E293B)
    val Slate700 = Color(0xFF334155)
    val Slate600 = Color(0xFF475569)
    /** 校准页「人脸未对准」相机角标点 */
    val Amber500 = Color(0xFFF59E0B)
    /** 校准页状态字 */
    val Amber300 = Color(0xFFFBBF24)
    /** 选中选项文字 */
    val GoodDeep = Color(0xFF065F46)

    // ── 训练页时间轴（深底上的三级弱化）─────────────────────────────────
    //
    // 实测修正：设计稿原值 #5E7386 / #5F7285 在 Panel 上只有 3.58:1 / 3.55:1，
    // 不满足正文 4.5:1。统一抬到 #8095A7（Panel 5.68:1、PanelSecond 4.72:1），
    // 两种深底都合格，且仍明显暗于 TimelineName（11.74:1），层次得以保留。
    val TimelineLabel = Color(0xFF8095A7)
    val TimelineName = Color(0xFFC6D5E2)
    /** 「未到」状态节点圆点（非文本，4.58:1 已达标）*/
    val TimelinePending = Color(0xFF6E8598)
    val TimelineDuration = Color(0xFF8095A7)

    /** 调试微标（仅 debug build 可见，不承载任何业务数值）*/
    val DebugGray = Color(0xFF8A8F8C)

    // ── 半透明覆盖 ────────────────────────────────────────────────────────
    //
    // ⚠️ Compose 的 Color(Int) 要求 **8 位十六进制（ARGB）**。
    // 写成 6 位（如 0xB45309）会被当成 alpha=0 的完全透明色 ——
    // 首版就是这样，lint 的 MissingColorAlphaChannel 抓到了：
    // 演示模式水印字会彻底看不见，AC-19「全屏水印」直接失效。
    /** 视频区相机角标底：panel 70% */
    val ScrimChip = Color(0xFF0F1A24).copy(alpha = 0.70f)
    /** 人脸未对准时的暗罩：panel 15% */
    val ScrimFaceMask = Color(0xFF0F1A24).copy(alpha = 0.15f)
    /** 演示模式水印字（不透明 warn） */
    val DemoWatermark = Color(0xFFB45309)
    /** 演示模式水印底（不透明 warn 容器） */
    val DemoWatermarkContainer = Color(0xFFFEF3C7)
}