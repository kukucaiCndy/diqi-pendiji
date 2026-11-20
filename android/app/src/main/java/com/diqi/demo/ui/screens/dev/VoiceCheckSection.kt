package com.diqi.demo.ui.screens.dev

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.diqi.demo.audio.AssetVoiceGuide
import com.diqi.demo.audio.VoiceScript
import com.diqi.demo.domain.training.TrainingPhase
import com.diqi.demo.ui.theme.HrColor
import com.diqi.demo.ui.theme.HrRadius
import com.diqi.demo.ui.theme.HrSize
import com.diqi.demo.ui.theme.HrSpacing
import com.diqi.demo.ui.theme.HrType

/**
 * 语音素材自检区（**仅 debug 构建**渲染）。
 *
 * 为什么需要它：语音是唯一无法靠"文字检索"验收的通道。
 * `VoiceScriptTest` 只能证明素材文件存在且非空，证明不了
 * **MediaPlayer 真的能播** —— 而 `assets.openFd` 对压缩资产失败这类问题
 * 只在运行时暴露。这里给每条语音一个播放按钮，配合
 * `adb shell dumpsys audio` 与 logcat 就能形成真机证据。
 *
 * S4 接入正式导航后，本区仍保留在 debug 设置页的「设备自检」入口里。
 */
@Composable
fun VoiceCheckSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val guide = remember { AssetVoiceGuide(context) }
    // 必须显式释放：否则离开自检页后 MediaPlayer 与音频焦点会泄漏到进程结束
    // （一直把其他音乐应用压着）。评审 V5。
    DisposableEffect(guide) {
        onDispose { guide.release() }
    }
    var lastPlayed by remember { mutableStateOf<String?>(null) }
    var available by remember { mutableStateOf(true) }

    Column(modifier = modifier) {
        Text("05 语音素材（仅 debug）", style = HrType.titleMedium, color = HrColor.Ink)
        Text(
            "点播放应能听到中文语音；素材由 tools/gen-voice.sh 生成",
            style = HrType.footnote,
            color = HrColor.WeakText,
        )
        Spacer(Modifier.height(HrSpacing.sm))

        Text(
            text = if (available) "最近播放：${lastPlayed ?: "—"}" else "语音不可用（素材缺失或被打包压缩）",
            style = HrType.monoSmall,
            color = if (available) HrColor.InkSecondary else HrColor.Danger,
            modifier = Modifier.testTag("dev.voice.status"),
        )
        Spacer(Modifier.height(HrSpacing.xs))

        VoiceScript.entries.forEach { script ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HrSpacing.xs),
            ) {
                Button(
                    onClick = {
                        guide.play(script)
                        lastPlayed = script.name
                        available = guide.isAvailable
                    },
                    modifier = Modifier
                        .heightIn(min = HrSize.minTouchTarget)
                        .width(96.dp)
                        .testTag("dev.voice.play.${script.name.lowercase()}"),
                    shape = HrRadius.controlShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HrColor.Panel,
                        contentColor = HrColor.Surface,
                    ),
                ) {
                    Text("播放", style = HrType.label)
                }
                Column(Modifier.weight(1f)) {
                    Text(script.name, style = HrType.label, color = HrColor.Ink)
                    Text(
                        script.text,
                        style = HrType.footnote,
                        color = HrColor.InkSecondary,
                    )
                }
            }
        }

        Spacer(Modifier.height(HrSpacing.sm))
        Text("阶段映射自检", style = HrType.label, color = HrColor.Ink)
        TrainingPhase.entries.forEach { phase ->
            val mapped = VoiceScript.forPhase(phase)
            Text(
                text = "$phase → ${mapped?.name ?: "（无语音）"}",
                style = HrType.monoSmall,
                color = if (mapped != null || phase == TrainingPhase.IDLE) {
                    HrColor.InkSecondary
                } else {
                    HrColor.Danger
                },
            )
        }
        Spacer(Modifier.height(HrSpacing.xs))
        Text(
            text = "播放器可用：$available",
            style = HrType.monoDebug,
            color = HrColor.InkSecondary,
            modifier = Modifier.testTag("dev.voice.available"),
        )
    }
}