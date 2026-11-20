package com.diqi.demo.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.util.Log
import com.diqi.demo.domain.training.TrainingPhase

/**
 * 语音引导接口。
 *
 * 抽象出接口是为了让训练层不依赖 MediaPlayer ——
 * 单测可以注入 [SilentVoiceGuide]，验证"状态推进不被音频阻塞"。
 */
interface VoiceGuide {
    /** 播放指定语句（异步，立即返回）*/
    fun play(script: VoiceScript)

    /** 播放某阶段进入提示 */
    fun playPhase(phase: TrainingPhase)

    /** 重播当前阶段的提示（P04 的「重复提示」按钮）*/
    fun repeatLast()

    /** 设置播放速度 0.8–1.2 */
    fun setSpeed(speed: Float)

    fun stop()

    /** 释放资源，必须幂等 */
    fun release()

    /** 音频是否可用。不可用时 UI 只依赖文字提示。 */
    val isAvailable: Boolean

    /** 当前是否正在播放（P04「语音状态」用）*/
    val isSpeaking: Boolean
}

/**
 * 音频焦点被抢占时的处理决定（纯函数，便于单测）。
 *
 * FR-05：「音频焦点被抢占：训练不中断，当前提示跳过，下一阶段正常。」
 * 这里的取值只影响**音频层**，永远不向训练层发信号 —— 训练不受影响是结构性的。
 */
enum class FocusAction {
    /** 暂停当前播放，保留焦点请求以便下一句复用 */
    PAUSE_CURRENT,
    /** 放弃焦点（其他应用要长期占用，例如通话）*/
    ABANDON_FOCUS,
    /** 什么也不做 */
    NOTHING,
}

/**
 * 基于 `MediaPlayer` + **预生成 AAC 素材**的实现（决策 D2′）。
 *
 * ## 为什么不用设备 TTS
 *
 * 验收机（NX729J / Android 15）上**没有任何 TTS 引擎**，设备 TTS 方案跑不起来。
 * 素材由 `tools/gen-voice.sh` 在开发机用 macOS 中文语音生成，
 * 文案唯一来源是 [VoiceScript]。
 *
 * ## 关键实现约束（都是评审指出的真实坑）
 *
 * 1. **不能用同步 `prepare()`**：`prepare()` 是阻塞 I/O，而 `play()` 常从
 *    Compose 的 `onClick`（主线程）调用，连点会造成 jank 甚至 ANR。
 *    改用 `prepareAsync()` + `onPrepared` 回调。
 * 2. **播完必须放弃音频焦点**：否则 `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`
 *    会从第一句一直占到 `release()`，整个训练期间把其他音乐应用压着。
 * 3. **`AUDIOFOCUS_LOSS` 要真正 abandon**：这是"其他应用长期占用"，
 *    复用同一个 focusRequest 再 request 会失败，下一句就永远播不出来。
 * 4. **素材打不开才降级为不可用**：播放期错误（例如播放中被 reset）不应
 *    把 `isAvailable` 永久置 false，否则整场语音连文字提示一起降级。
 * 5. **`repeatLast` 必须守 `released`**：否则释放后点"重复提示"会新建
 *    一个永不释放的 MediaPlayer。
 */
class AssetVoiceGuide(
    context: Context,
    initialSpeed: Float = 1.0f,
) : VoiceGuide {

    private val appContext = context.applicationContext
    private val audioManager by lazy {
        appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private var current: VoiceScript? = null

    /** 待播放的语句。`onPrepared` 回来时用它判断是否还被需要（防止连点串音）。 */
    private var pending: VoiceScript? = null

    private var speed: Float = initialSpeed.coerceIn(0.8f, 1.2f)

    @Volatile
    private var released = false

    @Volatile
    private var playing = false

    override var isAvailable: Boolean = true
        private set

    override val isSpeaking: Boolean get() = playing

    override fun play(script: VoiceScript) {
        if (released) return
        current = script
        startPlayback(script)
    }

    override fun playPhase(phase: TrainingPhase) {
        val script = VoiceScript.forPhase(phase) ?: return
        play(script)
    }

    override fun repeatLast() {
        if (released) return
        current?.let { startPlayback(it) }
    }

    override fun setSpeed(speed: Float) {
        this.speed = speed.takeIf { it.isFinite() }?.coerceIn(0.8f, 1.2f) ?: 1.0f
        applySpeedToCurrent()
    }

    override fun stop() {
        playing = false
        runCatching { player?.takeIf { it.isPlaying }?.pause() }
    }

    override fun release() {
        if (released) return
        released = true
        playing = false
        pending = null
        runCatching { player?.release() }
        player = null
        abandonFocus()
    }

    // ── 内部 ──────────────────────────────────────────────────────────────

    private fun startPlayback(script: VoiceScript) {
        if (!requestFocus()) {
            // 拿不到焦点：这句跳过，训练继续（AC-06 的语义）
            Log.i(TAG, "未获得音频焦点，跳过本句：${script.assetPath}")
            return
        }

        pending = script

        val p = obtainPlayer() ?: return

        // 打开素材：这是唯一会让 isAvailable 变 false 的地方
        val afd = try {
            appContext.assets.openFd(script.assetPath)
        } catch (e: Exception) {
            Log.w(TAG, "打开语音素材失败，该句降级为静默：${script.assetPath}", e)
            isAvailable = false
            abandonFocus()
            return
        }

        runCatching {
            p.reset()
            p.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            p.setOnPreparedListener { mp ->
                // 连点场景：只播最后一次请求的那句
                if (pending != script) {
                    runCatching { mp.reset() }
                    return@setOnPreparedListener
                }
                runCatching {
                    applySpeed(mp)
                    mp.start()
                    playing = true
                }
            }
            p.setOnCompletionListener { mp ->
                playing = false
                runCatching { mp.reset() }
                // 播完立刻归还焦点，不要占着压别人
                abandonFocus()
            }
            p.setOnErrorListener { _, what, extra ->
                Log.w(TAG, "播放出错 what=$what extra=$extra：${script.assetPath}")
                playing = false
                abandonFocus()
                true // 已处理，不要抛给上层
            }
            afd.use { fd ->
                p.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            }
            p.prepareAsync()
        }.onFailure {
            Log.w(TAG, "启动播放失败（本句跳过）：${script.assetPath}", it)
            abandonFocus()
        }
    }

    private fun obtainPlayer(): MediaPlayer? {
        player?.let { return it }
        return runCatching { MediaPlayer().also { player = it } }
            .onFailure { Log.e(TAG, "创建 MediaPlayer 失败", it) }
            .getOrNull()
    }

    private fun applySpeedToCurrent() {
        val p = player ?: return
        runCatching { applySpeed(p) }.onFailure { Log.w(TAG, "设置语速失败：$speed", it) }
    }

    private fun applySpeed(mp: MediaPlayer) {
        mp.playbackParams = PlaybackParams().setSpeed(speed).setPitch(1.0f)
    }

    private fun requestFocus(): Boolean {
        val am = audioManager ?: return true
        val request = focusRequest ?: AudioFocusRequest.Builder(
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
        )
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setOnAudioFocusChangeListener { change ->
                when (focusActionFor(change)) {
                    FocusAction.PAUSE_CURRENT -> {
                        // 只停当前这句，**训练继续**
                        playing = false
                        runCatching { player?.takeIf { it.isPlaying }?.pause() }
                    }
                    FocusAction.ABANDON_FOCUS -> {
                        playing = false
                        runCatching { player?.takeIf { it.isPlaying }?.pause() }
                        abandonFocus()
                    }
                    FocusAction.NOTHING -> Unit
                }
            }
            .build()
            .also { focusRequest = it }

        return am.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        val am = audioManager ?: return
        focusRequest?.let {
            runCatching { am.abandonAudioFocusRequest(it) }
        }
        focusRequest = null
    }

    companion object {
        private const val TAG = "VoiceGuide"

        /**
         * 焦点变化 → 动作（纯函数，可直接单测）。
         *
         * 关键语义：**没有任何分支会通知训练层**，所以"训练不中断"是
         * 结构性保证，而不是靠调用方记得处理。
         */
        fun focusActionFor(change: Int): FocusAction = when (change) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
            -> FocusAction.PAUSE_CURRENT

            // 长期失去焦点（来电接听、其他应用开始长期播放）：必须真正放弃，
            // 否则复用同一个 request 再 request 会失败，下一句永远播不出来
            AudioManager.AUDIOFOCUS_LOSS -> FocusAction.ABANDON_FOCUS

            else -> FocusAction.NOTHING
        }
    }
}

/**
 * 静默实现：测试与"语音不可用"降级时使用。
 *
 * 存在的意义：让训练链路可以在完全没有音频能力的环境下被完整测试。
 */
class SilentVoiceGuide : VoiceGuide {
    val played = mutableListOf<VoiceScript>()

    override fun play(script: VoiceScript) {
        played += script
    }

    override fun playPhase(phase: TrainingPhase) {
        VoiceScript.forPhase(phase)?.let { played += it }
    }

    override fun repeatLast() = Unit
    override fun setSpeed(speed: Float) = Unit
    override fun stop() = Unit
    override fun release() = Unit
    override val isAvailable: Boolean = false
    override val isSpeaking: Boolean = false
}