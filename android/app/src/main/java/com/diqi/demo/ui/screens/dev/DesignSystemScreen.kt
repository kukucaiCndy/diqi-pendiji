package com.diqi.demo.ui.screens.dev

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.diqi.demo.core.contrast.Contrast
import com.diqi.demo.ui.components.HrPage
import com.diqi.demo.ui.theme.HrColor
import com.diqi.demo.ui.theme.HrRadius
import com.diqi.demo.ui.theme.HrSize
import com.diqi.demo.ui.theme.HrSpacing
import com.diqi.demo.ui.theme.HrType

/**
 * 设计系统自检页（仅 debug 入口）。
 *
 * 存在的理由不只是"看一眼颜色"：它把 `design/desing.pen` 的 token、字阶、
 * 间距、圆角渲染出来，并**实时计算对比度比值**打印在色块上。
 * 这样 S1 的端到端验证可以同时确认两件事：
 *   ① 设计 token 与设计稿一致
 *   ② FR-15 的 4.5:1 在真机渲染下确实成立（不依赖可信度存疑的估算）
 */
@Composable
fun DesignSystemScreen(modifier: Modifier = Modifier) {
    HrPage(modifier = modifier, tag = "dev.design_system.root") {
        Text("HerRhythm DS v1.0", style = HrType.headline, color = HrColor.Ink)
        Text(
            "设计系统自检 · 比值实时计算",
            style = HrType.footnote,
            color = HrColor.InkSecondary,
        )
        Spacer(Modifier.height(HrSpacing.lg))

        SectionTitle("01 色彩 Color Tokens")
        // 每行同时给出「页面底 Canvas」与「卡片 Surface」两个真实底色下的比值。
        // 首版只算了卡片底，而这一页的真实底色是 Canvas —— 等于证明了另一个底色。
        listOf(
            Triple("ink 主文字", HrColor.Ink, HrColor.Surface),
            Triple("ink-2 次文字", HrColor.InkSecondary, HrColor.Surface),
            Triple("weak 弱文字", HrColor.WeakText, HrColor.Surface),
            Triple("ink-3 弱(仅非文本)", HrColor.InkTertiary, HrColor.Surface),
            Triple("line 分割(装饰)", HrColor.Line, HrColor.Canvas),
            Triple("brand 主线", HrColor.Brand, HrColor.Surface),
            Triple("accent 深底强调", HrColor.Accent, HrColor.Panel),
            Triple("HR 心率", HrColor.HeartRate, HrColor.Surface),
            Triple("RR 呼吸", HrColor.BreathingRate, HrColor.Surface),
            Triple("warn 提示", HrColor.Warn, HrColor.WarnContainer),
            Triple("good 正常", HrColor.Good, HrColor.GoodContainer),
            Triple("danger 红线", HrColor.Danger, HrColor.DangerContainer),
            Triple("演示水印", HrColor.DemoWatermark, HrColor.DemoWatermarkContainer),
        ).forEach { (name, color, lightBg) ->
            ColorRow(name = name, color = color, lightBg = lightBg)
        }

        Spacer(Modifier.height(HrSpacing.xl))
        SectionTitle("02 字体 Typography（正文 ≥20sp / 关键数字 ≥32sp）")
        listOf(
            "display 37/700" to HrType.display,
            "headline 30/700" to HrType.headline,
            "headlineSm 28/700" to HrType.headlineSm,
            "titleLarge 22/700" to HrType.titleLarge,
            "bodyLarge 22/400" to HrType.bodyLarge,
            "bodyMedium 20/400" to HrType.bodyMedium,
            "button 19/700" to HrType.button,
            "label 15/700" to HrType.label,
            "micro 12/600" to HrType.micro,
        ).forEach { (label, style) ->
            TypeRow(label = label, style = style, sample = "保持住，自然呼吸")
        }
        TypeRow(
            label = "monoDisplay 36/600（倒计时，关键数字）",
            style = HrType.monoDisplay,
            sample = "00:03",
        )
        // 指标值真实渲染在白卡上（设计稿 波形卡 fill=#FFFFFF），
        // 放进白卡里展示才能让上方的对比度数字与真实观感对得上
        Box(
            Modifier
                .fillMaxWidth()
                .background(HrColor.Surface, HrRadius.controlShape)
                .padding(HrSpacing.sm),
        ) {
            TypeRow(
                label = "monoMetric 32/600（指标值，关键数字 · 白卡上）",
                style = HrType.monoMetric,
                sample = "72 bpm",
                color = HrColor.HeartRate,
            )
        }
        TypeRow(
            label = "monoDebug 11（仅 debug）",
            style = HrType.monoDebug,
            sample = "DEBUG SQI 0.82",
            color = HrColor.DebugGray,
        )

        Spacer(Modifier.height(HrSpacing.xl))
        SectionTitle("03 间距与圆角")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HrSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(
                4.dp to "4", 8.dp to "8", 12.dp to "12", 16.dp to "16",
                20.dp to "20", 24.dp to "24", 32.dp to "32", 40.dp to "40",
            ).forEach { (size, label) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(size.coerceAtMost(28.dp))
                            .background(HrColor.Track),
                    )
                    Text(label, style = HrType.monoDebug, color = HrColor.InkSecondary)
                }
            }
        }
        Spacer(Modifier.height(HrSpacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(HrSpacing.sm)) {
            RadiusChip("卡片 16", HrRadius.cardShape)
            RadiusChip("控件 12", HrRadius.controlShape)
            RadiusChip("胶囊", HrRadius.capsuleShape)
        }

        Spacer(Modifier.height(HrSpacing.xl))
        SectionTitle("04 触控与关键尺寸")
        Text(
            "最小热区 ${HrSize.minTouchTarget.value.toInt()}dp · " +
                "主按钮 ${HrSize.primaryButton.value.toInt()}dp · " +
                "人脸框 ${HrSize.faceBoxWidth.value.toInt()}×${HrSize.faceBoxHeight.value.toInt()}dp",
            style = HrType.bodySmall,
            color = HrColor.InkSecondary,
            modifier = Modifier.testTag("dev.design_system.touch_target"),
        )

        // 语音自检区：仅 debug 构建渲染。语音是唯一无法靠文字检索验收的通道，
        // 需要真机点击 + dumpsys audio / logcat 证据。
        if (com.diqi.demo.BuildConfig.DEBUG) {
            Spacer(Modifier.height(HrSpacing.xl))
            VoiceCheckSection()
        }

        Spacer(Modifier.height(HrSpacing.xxl))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = HrType.titleMedium,
        color = HrColor.Ink,
        modifier = Modifier.padding(bottom = HrSpacing.sm),
    )
}

@Composable
private fun ColorRow(name: String, color: Color, lightBg: Color) {
    // 深色 token 用深底度量，浅色 token 用它的浅色底度量
    val isDarkToken = color.luminance() < 0.2f && lightBg.luminance() < 0.2f
    val onCanvas = Contrast.ratio(color, HrColor.Canvas)
    val onSurface = Contrast.ratio(color, HrColor.Surface)
    val shown = if (isDarkToken) Contrast.ratio(color, lightBg) else minOf(onCanvas, onSurface)
    val shownLabel = if (isDarkToken) "深底" else "min(页面/卡片)"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 44.dp, height = 32.dp)
                .background(if (isDarkToken) lightBg else HrColor.Canvas)
                .border(1.dp, HrColor.Line),
        ) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(width = 24.dp, height = 16.dp)
                    .background(color),
            )
        }
        Spacer(Modifier.width(HrSpacing.sm))
        Column(Modifier.weight(1f)) {
            Text(name, style = HrType.label, color = HrColor.Ink)
            Text(shownLabel, style = HrType.monoDebug, color = HrColor.InkSecondary)
        }
        Text(
            text = "%.2f:1".format(shown),
            style = HrType.monoSmall,
            color = if (shown >= Contrast.AA_LARGE) HrColor.Good else HrColor.Danger,
        )
    }
}

@Composable
private fun TypeRow(label: String, style: TextStyle, sample: String, color: Color = HrColor.Ink) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(label, style = HrType.monoDebug, color = HrColor.InkSecondary)
        Text(sample, style = style, color = color)
    }
}

@Composable
private fun RadiusChip(label: String, shape: Shape) {
    Box(
        Modifier
            .background(HrColor.Panel, shape)
            .padding(horizontal = HrSpacing.sm, vertical = HrSpacing.xs),
    ) {
        Text(label, style = HrType.micro, color = HrColor.Surface)
    }
}