package com.diqi.demo.audio

/**
 * 语音脚本的**唯一来源**（需求 §5.5）。
 *
 * ## 为什么文案与音频文件名必须在一处定义
 *
 * 需求 §5.5 的 6 条文案会随内容评审反复改，音频素材要重录。
 * 如果文案在 Compose 里写一遍、文件名在播放器里写一遍，
 * 必然出现"文案已改、音频还是旧的"这种最尴尬的演示事故。
 * 这里把 `文案 + 资源名 + 生成参数` 绑成一个枚举，
 * 生成脚本 `tools/gen-voice.sh` 也直接读这里的定义。
 *
 * ## 为什么用预生成音频而不是设备 TTS（决策 D2′）
 *
 * 实测目标设备 **没有任何 TTS 引擎**（`cmd package query-services -a
 * android.intent.action.TTS_SERVICE` → No services found），
 * 所以设备 TTS 方案在验收机上根本不可用。
 * 改为构建期用 macOS 中文语音生成 AAC 素材，精确回到需求 §8.1 的原始设计
 * （预录音频 + ExoPlayer/MediaPlayer），且完全离线、可重复生成。
 *
 * ## 生成方式
 *
 * ```
 * tools/gen-voice.sh        # 读本文件定义的文案，输出 assets/voice 下的 m4a 素材
 * ```
 *
 * ## 文案红线
 *
 * 不得出现「肌力」「动作正确率」「治愈」等词（§5.6）。
 * `VoiceScriptTest` 会用 `BannedWords` 逐条扫描 —— 改文案时忘删禁词会直接红。
 */
enum class VoiceScript(
    /** 音频资源相对路径（`assets/` 下）*/
    val assetPath: String,
    /** 播放的文案。同时是生成脚本的输入。 */
    val text: String,
    /** 需求 §5.5 中的对应条目，便于追溯 */
    val source: String,
) {
    START(
        assetPath = "voice/start.m4a",
        text = "现在开始今天的练习，请坐稳，放松肩膀。",
        source = "§5.5 开始前",
    ),
    CAMERA_OPTIONAL(
        assetPath = "voice/camera_optional.m4a",
        text = "想把手机放稳，让摄像头看到你的脸；不想开也可以直接开始。",
        source = "§5.5 开启摄像头（可选）",
    ),
    PREP(
        assetPath = "voice/prep.m4a",
        text = "坐稳，放松肩膀和腹部。",
        source = "§5.3 Preparation 进入动作",
    ),
    CONTRACT(
        assetPath = "voice/contract.m4a",
        text = "轻轻提起盆底，像忍住排气一样，不要向下用力。",
        source = "§5.5 收缩",
    ),
    HOLD(
        assetPath = "voice/hold.m4a",
        text = "保持住，继续自然呼吸，不要憋气。",
        source = "§5.5 保持",
    ),
    RELAX(
        assetPath = "voice/relax.m4a",
        text = "慢慢放松，感受盆底回到原位。",
        source = "§5.5 放松",
    ),
    PAUSE(
        assetPath = "voice/pause.m4a",
        text = "训练已暂停，休息一下，准备好了再继续。",
        source = "§5.3 Paused",
    ),
    COMPLETE(
        assetPath = "voice/complete.m4a",
        text = "今天的练习完成了，做得很好。",
        source = "§5.5 结束",
    ),
    ;

    /** 该阶段进入时该播哪一句（null 表示该阶段无语音）*/
    companion object {
        fun forPhase(phase: com.diqi.demo.domain.training.TrainingPhase): VoiceScript? =
            when (phase) {
                com.diqi.demo.domain.training.TrainingPhase.IDLE -> null
                com.diqi.demo.domain.training.TrainingPhase.PREPARATION -> PREP
                com.diqi.demo.domain.training.TrainingPhase.CONTRACT -> CONTRACT
                com.diqi.demo.domain.training.TrainingPhase.HOLD -> HOLD
                com.diqi.demo.domain.training.TrainingPhase.RELAX -> RELAX
                com.diqi.demo.domain.training.TrainingPhase.COMPLETE -> COMPLETE
            }
    }
}