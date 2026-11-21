package com.diqi.demo.ui.screens.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diqi.demo.data.prefs.ScreeningAnswer
import com.diqi.demo.ui.UiTag
import com.diqi.demo.ui.theme.HrType

/**
 * P01-a 欢迎与隐私 / P01-b 安全筛查。
 *
 * ## 数值来源
 *
 * 全部取自 `docs/DESIGN-SPEC.md` 的 `P01-a` / `P01-b` 段落，换算 1 设计 px = 1sp / 1dp。
 *
 * ## 两条硬约束（不因改版丢失）
 *
 * 1. **隐私说明先于任何授权**（FR-01）：本页不申请相机权限，
 *    只在用户点「开启摄像头」时才请求。
 * 2. **不预选**（设计稿明确「3 题单选，不可预选」）：`canGoNext` 由是否作答决定。
 *
 * ## 一处与设计稿的差异（数据驱动）
 *
 * 设计稿进度写「1 / 3」，但需求 FR-01 是 **4 组问题**，代码里也是 4 题。
 * 这里渲染 `当前 / 实际题数`，不写死 3 —— 否则用户答到第 4 题会看到「1 / 3」。
 *
 * ## 图标（如实标注）
 *
 * `shield-check` / `video-off` / `cloud-off` 在 material-icons-core 里没有，
 * 本项目也没有 Pencil 栅格化工具导出 PNG，故按等价形状手绘三个 glyph；
 * `chevron-left` → `Icons.Filled.KeyboardArrowLeft`、`arrow-right` → `Icons.Filled.ArrowForward`。
 * **不用 Lock/Info 之类语义不符的图标顶替。**
 */
@Composable
fun WelcomeScreen(
    vm: OnboardingViewModel,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HrWhite)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag(UiTag.P01_ROOT),
    ) {
        // 可滚内容区。设计稿在「隐私卡」与「主按钮」之间放了一个 fill_container 空隙，
        // 也就是说按钮**锚定在底部**，不是紧跟卡片 —— 首版做错了，页面下方留了一大片空白。
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(26.dp),
        ) {
        // 品牌行：9dp 圆点 + 10sp 字
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(HrGood),
            )
            Text("HER RHYTHM　她的节律", style = HrType.micro, color = HrGood)
        }

        Spacer(Modifier.height(44.dp))

        // 主标题 37/700，三行
        Text(
            text = "今天，我们一起\n练习照顾\n自己的身体。",
            style = HrType.display,
            color = HrInk,
            modifier = Modifier.testTag(UiTag.P01_TITLE),
        )

        Spacer(Modifier.height(18.dp))

        Text(
            text = "每天 3 分钟，跟着语音节律练习，屏幕会实时显示你的心率与呼吸。",
            style = HrType.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Normal),
            color = HrInkSecondary,
        )

        Spacer(Modifier.height(32.dp))

        // 隐私卡：#F1F5F9 / r16 / p20 / gap16
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(HrSubtle)
                .padding(20.dp)
                .testTag(UiTag.P01_PRIVACY_CARD),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("关于摄像头，三件事先讲清楚", style = HrType.label, color = HrInk)
            PrivacyRow("画面只在手机本地处理") { c, m -> ShieldCheckGlyph(c, m) }
            PrivacyRow("不录像，不留任何画面") { c, m -> VideoOffGlyph(c, m) }
            PrivacyRow("不上传，原始帧不出手机") { c, m -> CloudOffGlyph(c, m) }
        }

        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 26.dp, end = 26.dp, bottom = 8.dp),
        ) {
            Spacer(Modifier.height(24.dp))

            // 主按钮 h56 / r28 / #0F1A24，图标 17 + 文字 17/700
            Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(HrInk)
                .clickable(role = Role.Button, onClick = onStart)
                .testTag(UiTag.P01_PRIMARY_BUTTON),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = HrWhite,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(8.dp))
                Text("我知道了，继续", style = HrType.button, color = HrWhite)
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "下一步需要开启相机权限，也可以稍后在设置里开启",
                style = HrType.footnote,
                color = HrInkTertiary,
            )
        }
    }
}

@Composable
private fun PrivacyRow(text: String, glyph: @Composable (Color, Modifier) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(HrWhite),
            contentAlignment = Alignment.Center,
        ) {
            glyph(HrGood, Modifier.size(16.dp))
        }
        Text(text, style = HrType.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = HrSlate800)
    }
}

/**
 * P01-b 安全筛查（一屏一题）。
 *
 * ⚠️ VM 由导航层传入，**不能在这里再 new 一个** —— 那样"欢迎→筛查"共享不到答案，
 * 且筛查页 pop 后 entry-scoped 实例被清空（历史评审已指出）。
 */
@Composable
fun ScreeningScreen(
    vm: OnboardingViewModel,
    onFinished: () -> Unit,
    onBackFromFirst: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsState()
    val question = state.current ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HrWhite)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag(UiTag.P01_SCREENING_ROOT),
    ) {
        // 导航行 padding [2,22,10,22]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, top = 2.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clickable(role = Role.Button) {
                        if (state.currentIndex == 0) onBackFromFirst() else vm.back()
                    },
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowLeft,
                    contentDescription = "返回",
                    tint = HrInk,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                // 设计稿写「1 / 3」，但需求是 4 题：这里用真实题数
                text = "${state.currentIndex + 1} / ${state.questions.size}",
                style = HrType.labelSmall,
                color = HrInkTertiary,
                modifier = Modifier
                    .weight(1f)
                    .testTag(UiTag.P01_SCREENING_PROGRESS),
            )
        }

        // 进度条：轨 4dp #E2E8F0，进度 #0F766E
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(HrLine),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(state.progress)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(HrBrand),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 22.dp),
        ) {
            Text(
                text = question.title,
                style = HrType.question,
                color = HrInk,
                modifier = Modifier.testTag(UiTag.P01_SCREENING_QUESTION),
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = question.hint,
                style = HrType.bodyMedium,
                color = HrInkTertiary,
            )

            Spacer(Modifier.height(22.dp))

            // 选项：h64 / r14 / 内边距水平 18 / gap12
            question.options.forEach { (answer, label) ->
                OptionRow(
                    label = label,
                    selected = state.selected == answer,
                    tag = when (answer) {
                        ScreeningAnswer.YES -> UiTag.P01_OPTION_YES
                        ScreeningAnswer.NO -> UiTag.P01_OPTION_NO
                        ScreeningAnswer.UNSURE -> UiTag.P01_OPTION_UNSURE
                    },
                    onClick = { vm.answer(state.currentIndex, answer) },
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        Spacer(Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
        ) {
            // 主按钮 h56 / r28 / #0F1A24
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (state.canGoNext) HrInk else HrDisabled)
                    .clickable(enabled = state.canGoNext, role = Role.Button) {
                        if (state.isLast) vm.submit(onFinished) else vm.next()
                    }
                    .testTag(UiTag.P01_SCREENING_NEXT),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (state.isLast) "完成" else "下一题",
                    style = HrType.button,
                    color = HrWhite,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "选完后自动进入下一题，可随时返回修改",
                style = HrType.footnote,
                color = HrInkTertiary,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun OptionRow(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) HrGoodContainer else HrWhite)
            .border(
                BorderStroke(1.dp, if (selected) HrGood else HrTrack),
                RoundedCornerShape(14.dp),
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 18.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 22dp 圆：选中实心 #047857，未选描边 #94A3B8
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) HrGood else HrWhite)
                .border(1.5.dp, if (selected) HrGood else HrInkTertiary, CircleShape),
        )
        Text(
            text = label,
            style = if (selected) {
                HrType.titleSmall.copy(fontWeight = FontWeight.Bold)
            } else {
                HrType.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            },
            color = if (selected) HrGoodDeep else HrSlate800,
        )
    }
}

// ── 手绘图标（material-icons-core 无对应项）──────────────────────────────

@Composable
private fun ShieldCheckGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val p = Path().apply {
            moveTo(w * 0.5f, 0f)
            lineTo(w, h * 0.18f)
            lineTo(w, h * 0.52f)
            quadraticTo(w, h, w * 0.5f, h)
            quadraticTo(0f, h, 0f, h * 0.52f)
            lineTo(0f, h * 0.18f)
            close()
        }
        drawPath(p, color = color, style = Stroke(width = h * 0.1f))
        // 勾
        val c = Path().apply {
            moveTo(w * 0.28f, h * 0.5f)
            lineTo(w * 0.45f, h * 0.66f)
            lineTo(w * 0.74f, h * 0.36f)
        }
        drawPath(c, color = color, style = Stroke(width = h * 0.12f))
    }
}

@Composable
private fun VideoOffGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val bodyW = w * 0.62f
        val bodyH = h * 0.56f
        val top = (h - bodyH) / 2f
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, top),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(h * 0.14f),
            style = Stroke(width = h * 0.11f),
        )
        val lens = Path().apply {
            moveTo(bodyW + w * 0.04f, h * 0.38f)
            lineTo(w * 0.9f, h * 0.24f)
            lineTo(w * 0.9f, h * 0.76f)
            lineTo(bodyW + w * 0.04f, h * 0.62f)
            close()
        }
        drawPath(lens, color = color)
        // 斜杠：表示"关闭"
        drawLine(
            color = color,
            start = Offset(w * 0.1f, h * 0.9f),
            end = Offset(w * 0.9f, h * 0.1f),
            strokeWidth = h * 0.12f,
        )
    }
}

@Composable
private fun CloudOffGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        // 云：三段圆弧近似
        val cloud = Path().apply {
            moveTo(w * 0.24f, h * 0.78f)
            quadraticTo(w * 0.04f, h * 0.78f, w * 0.06f, h * 0.56f)
            quadraticTo(w * 0.08f, h * 0.38f, w * 0.26f, h * 0.4f)
            quadraticTo(w * 0.32f, h * 0.16f, w * 0.56f, h * 0.22f)
            quadraticTo(w * 0.76f, h * 0.26f, w * 0.74f, h * 0.46f)
            quadraticTo(w * 0.94f, h * 0.48f, w * 0.92f, h * 0.64f)
            quadraticTo(w * 0.9f, h * 0.78f, w * 0.72f, h * 0.78f)
            close()
        }
        drawPath(cloud, color = color, style = Stroke(width = h * 0.1f))
        drawLine(
            color = color,
            start = Offset(w * 0.12f, h * 0.9f),
            end = Offset(w * 0.88f, h * 0.08f),
            strokeWidth = h * 0.12f,
        )
    }
}

// ── 设计稿页面内联色（DESIGN-SPEC §P01-a / §P01-b）─────────────────────
private val HrWhite = Color(0xFFFFFFFF)
private val HrInk = Color(0xFF0F1A24)
private val HrInkSecondary = Color(0xFF5C6B7A)
private val HrInkTertiary = Color(0xFF94A3B8)
private val HrSubtle = Color(0xFFF1F5F9)
private val HrLine = Color(0xFFE2E8F0)
private val HrTrack = Color(0xFFCBD5E1)
private val HrBrand = Color(0xFF0F766E)
private val HrGood = Color(0xFF047857)
private val HrGoodDeep = Color(0xFF065F46)
private val HrGoodContainer = Color(0xFFE7F6EC)
private val HrSlate800 = Color(0xFF1E293B)
private val HrDisabled = Color(0xFFCBD5E1)