package com.diqi.demo.audio

import com.diqi.demo.core.text.BannedWords
import com.diqi.demo.domain.training.TrainingPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 语音脚本与素材的一致性检查。
 *
 * ## 它防的是什么事故
 *
 * 1. **文案改了、音频还是旧的** —— 演示时用户听到的和屏幕上写的不一样，非常尴尬
 * 2. **素材缺失** —— `assets` 里少一个文件，运行期才在 `openFd` 报错
 * 3. **文案出现禁词** —— §5.6 红线，且语音是无法事后"文字检索"的通道
 * 4. **阶段没有映射语音** —— 某个阶段静默推进，老年用户不知道发生了什么
 *
 * 素材由 `tools/gen-voice.sh` 从 [VoiceScript] 的 `text` 生成，
 * 所以"文案与音频同源"在构建流程上成立；这里做的是**产物存在性**检查。
 */
class VoiceScriptTest {

    private val voiceDir: File by lazy {
        listOf(
            File("src/main/assets/voice"),
            File("app/src/main/assets/voice"),
        ).firstOrNull { it.isDirectory }
            ?: error("找不到 assets/voice，工作目录=${File(".").absolutePath}")
    }

    @Test
    fun 需求指定的六条语音都在脚本里() {
        // §5.5 明确列出 6 条；少一条就是漏做
        val required = listOf(
            VoiceScript.START,
            VoiceScript.CAMERA_OPTIONAL,
            VoiceScript.CONTRACT,
            VoiceScript.HOLD,
            VoiceScript.RELAX,
            VoiceScript.COMPLETE,
        )
        required.forEach { script ->
            assertTrue("缺少语音条目 $script", VoiceScript.entries.contains(script))
        }
    }

    @Test
    fun 每条语音都有非空文案与非空资源路径() {
        VoiceScript.entries.forEach { s ->
            assertTrue("$s 的文案为空", s.text.isNotBlank())
            assertTrue("$s 的资源路径为空", s.assetPath.isNotBlank())
            assertTrue("$s 的资源路径必须以 voice/ 开头", s.assetPath.startsWith("voice/"))
            assertTrue("$s 的资源必须以 .m4a 结尾", s.assetPath.endsWith(".m4a"))
        }
    }

    @Test
    fun 每条语音的音频素材都真实存在且非空() {
        VoiceScript.entries.forEach { s ->
            val f = File(voiceDir, s.assetPath.removePrefix("voice/"))
            assertTrue("缺少音频素材：${f.path}（请运行 tools/gen-voice.sh）", f.exists())
            assertTrue("音频素材为空：${f.path}", f.length() > 1_000)
        }
    }

    @Test
    fun 语音文案不得命中禁词() {
        VoiceScript.entries.forEach { s ->
            val hits = BannedWords.scan(s.text)
            assertTrue("语音 $s 命中禁词 $hits：${s.text}", hits.isEmpty())
        }
    }

    @Test
    fun 每个训练阶段都有对应语音_且都能解析到素材() {
        listOf(
            TrainingPhase.PREPARATION,
            TrainingPhase.CONTRACT,
            TrainingPhase.HOLD,
            TrainingPhase.RELAX,
            TrainingPhase.COMPLETE,
        ).forEach { phase ->
            val script = VoiceScript.forPhase(phase)
            assertNotNull("阶段 $phase 没有对应语音", script)
            val f = File(voiceDir, script!!.assetPath.removePrefix("voice/"))
            assertTrue("阶段 $phase 的素材不存在：${f.path}", f.exists())
        }
        // IDLE 不需要语音（还没开始）
        assertNull(VoiceScript.forPhase(TrainingPhase.IDLE))
    }

    @Test
    fun 资源路径全局唯一() {
        val paths = VoiceScript.entries.map { it.assetPath }
        assertEquals("存在重复的资源路径：$paths", paths.size, paths.toSet().size)
    }

    @Test
    fun 保持阶段文案必须强调自然呼吸() {
        // §5.5 与 §5.4 都明确「保持时必须自然呼吸」，这是安全要点而非文案偏好
        val text = VoiceScript.HOLD.text
        assertTrue("保持阶段的语音必须提醒自然呼吸：$text", text.contains("呼吸"))
        assertTrue("保持阶段的语音必须提醒不要憋气：$text", text.contains("憋气"))
    }

    @Test
    fun 必须有一条不开启摄像头也能开始的说明() {
        // FR-03 / AC-04：摄像头是可选的，这句话必须有
        val text = VoiceScript.CAMERA_OPTIONAL.text
        assertTrue("必须明确「不想开也可以直接开始」：$text", text.contains("不想开"))
    }
}