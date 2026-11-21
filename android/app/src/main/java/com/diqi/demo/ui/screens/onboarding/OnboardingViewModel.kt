package com.diqi.demo.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diqi.demo.AppContainer
import com.diqi.demo.data.prefs.ScreeningAnswer
import com.diqi.demo.data.prefs.SafetyScreening
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 一屏一题（FR-01 的 4 组问题）。 */
data class ScreeningQuestion(
    val index: Int,
    val title: String,
    val hint: String,
    val options: List<Pair<ScreeningAnswer, String>>,
)

/**
 * 引导与安全筛查（P01）。
 *
 * ## 文案依据
 *
 * 首屏主标题直接取需求 FR-01 原文：「今天，我们一起练习照顾自己的身体。」
 * 隐私说明必须在**任何授权之前**出现，且要写清三件事：
 * 画面只在手机本地处理、不保存、不上传；选择不带摄像头也能完整训练。
 *
 * ## 分流（需求 FR-01 明文）
 *
 * - 第 2、3 题任一「是」→ 就医/专业评估提示 + 训练页常驻提示条，允许继续但不建议加量
 * - 第 1、4 题「否/不确定」→ 建议先接受专业评估，允许继续，完成页不给「已学会」结论
 *
 * 分流结果由 `SafetyScreening` 的派生属性统一给出，页面只读不算。
 */
data class OnboardingUiState(
    val questions: List<ScreeningQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val answers: Map<Int, ScreeningAnswer> = emptyMap(),
    val saving: Boolean = false,
    val saved: Boolean = false,
) {
    val current: ScreeningQuestion? get() = questions.getOrNull(currentIndex)
    val progress: Float
        get() = if (questions.isEmpty()) 0f else (currentIndex + 1f) / questions.size
    val selected: ScreeningAnswer? get() = answers[currentIndex]
    val canGoNext: Boolean get() = selected != null
    val isLast: Boolean get() = currentIndex == questions.lastIndex

    /** 组装成筛查结果，交给数据层存储。 */
    fun toScreening(): SafetyScreening = SafetyScreening(
        q1ProfessionalGuidance = answers[0],
        q2NewSymptoms = answers[1],
        q3PregnancyOrSurgery = answers[2],
        q4CanFeelContraction = answers[3],
    )
}

class OnboardingViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState(questions = QUESTIONS))
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun answer(questionIndex: Int, answer: ScreeningAnswer) {
        _state.value = _state.value.copy(
            answers = _state.value.answers + (questionIndex to answer),
        )
    }

    fun next() {
        val s = _state.value
        if (!s.canGoNext) return
        _state.value = if (s.isLast) s else s.copy(currentIndex = s.currentIndex + 1)
    }

    fun back() {
        val s = _state.value
        if (s.currentIndex > 0) _state.value = s.copy(currentIndex = s.currentIndex - 1)
    }

    /** 保存筛查结果并标记已确认。 */
    fun submit(onDone: () -> Unit) {
        val screening = _state.value.toScreening()
        if (!screening.isComplete) return
        _state.value = _state.value.copy(saving = true)
        viewModelScope.launch {
            container.settingsStore.setScreening(screening, System.currentTimeMillis())
            _state.value = _state.value.copy(saving = false, saved = true)
            onDone()
        }
    }

    /**
     * 跳过筛查。
     *
     * 必须落库：否则下一次冷启动会重新弹引导（首屏流失），
     * 而且用户答完几题后再按跳过会静默丢弃答案。
     */
    fun skip(onDone: () -> Unit) {
        viewModelScope.launch {
            container.settingsStore.acknowledgeScreeningSkipped(System.currentTimeMillis())
            onDone()
        }
    }

    /** 记录隐私说明已读（在任何授权之前）。 */
    fun acceptPrivacy() {
        viewModelScope.launch {
            container.settingsStore.acceptPrivacyNotice(System.currentTimeMillis())
        }
    }

    companion object {
        /** 4 组问题，均提供「不确定」（需求 FR-01）。 */
        val QUESTIONS: List<ScreeningQuestion> = listOf(
            ScreeningQuestion(
                index = 0,
                // 题干与辅助文案取自设计稿 P01-b 原文
                title = "是否有医生或专业治疗师指导过你做过盆底训练？",
                hint = "这会影响我们给你的建议强度，可以如实回答。",
                options = listOf(
                    ScreeningAnswer.YES to "是，有专业指导",
                    ScreeningAnswer.NO to "否，没有指导过",
                    ScreeningAnswer.UNSURE to "不确定",
                ),
            ),
            ScreeningQuestion(
                index = 1,
                title = "最近是否有盆腔疼痛、血尿或排尿困难？",
                hint = "这些是需要先看医生的情况。",
                options = listOf(
                    ScreeningAnswer.YES to "是，有这些情况",
                    ScreeningAnswer.NO to "否，没有",
                    ScreeningAnswer.UNSURE to "不确定",
                ),
            ),
            ScreeningQuestion(
                index = 2,
                title = "是否处于孕期，或有盆底疼痛、近期相关手术？",
                hint = "包括正在恢复期的情况。",
                options = listOf(
                    ScreeningAnswer.YES to "是",
                    ScreeningAnswer.NO to "否",
                    ScreeningAnswer.UNSURE to "不确定",
                ),
            ),
            ScreeningQuestion(
                index = 3,
                title = "能感觉到盆底肌的收缩吗？",
                hint = "感觉不到也很常见，可以选不确定。",
                options = listOf(
                    ScreeningAnswer.YES to "能感觉到",
                    ScreeningAnswer.NO to "感觉不到",
                    ScreeningAnswer.UNSURE to "不确定",
                ),
            ),
        )
    }
}