package com.diqi.demo.ui.screens.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diqi.demo.AppContainer
import com.diqi.demo.data.db.SensorMode
import com.diqi.demo.data.repo.SessionDraft
import com.diqi.demo.domain.training.EngineSnapshot
import com.diqi.demo.domain.training.TrainingEngine
import com.diqi.demo.domain.training.TrainingFrame
import com.diqi.demo.domain.training.TrainingParams
import com.diqi.demo.domain.training.TrainingStatus
import com.diqi.demo.training.TrainingSessionCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/** 训练页 UI 状态。 */
data class TrainingUiState(
    val frame: TrainingFrame = TrainingFrame.idle(TrainingParams.DEFAULT.repsPerSet),
    val params: TrainingParams = TrainingParams.DEFAULT,
    val paused: Boolean = false,
    /** 进入页面时发现的未完成训练（AC-13）*/
    val pendingRecovery: EngineSnapshot? = null,
    val showStopConfirm: Boolean = false,
    val finished: Boolean = false,
    val savedSessionId: String? = null,
    /**
     * 指标区状态。
     *
     * 首版是一个 `sensorEnabled: Boolean` 假开关（恒 false 且无赋值点）。
     * 用枚举替代：S6 接入时必须**显式**给出四态之一，
     * 不可能出现"S6 随手翻转 true 就显示假状态"的情况。
     */
    val metric: MetricUiState = MetricUiState.NotEnabled,
    /** FR-01 分流：训练页常驻就医提示条 */
    val clinicalNotice: Boolean = false,
    val voiceAvailable: Boolean = true,
    /** 保存失败（例如磁盘满）。UI 必须提示，不能静默。 */
    val saveFailed: Boolean = false,
)

/**
 * 训练页 ViewModel：**协调器唯一的 UI 接入点**。
 *
 * ## 三个容易出错的地方，都在这里守住
 *
 * 1. **会话 id 在开始时生成并持有**（评审 D4）：`SessionDraft.id` 故意没有默认值，
 *    所以"连点两次完成只产生一条记录"（AC-08）在数据层成立的前提是
 *    这里只生成一次 id。若每次点保存都 new 一个 UUID，AC-08 直接失效。
 * 2. **保存只做一次**：自然完成与用户点结束都会走到 [finishSession]，
 *    用 `savedSessionId` 做闸门，避免两条记录。
 * 3. **中断恢复**：进入页面先查快照；有就弹「继续 / 丢弃」，
 *    用户选继续则用快照恢复，选丢弃则清掉快照从零开始（AC-13）。
 */
class TrainingViewModel(
    private val container: AppContainer,
    private val params: TrainingParams = TrainingParams.DEFAULT,
) : ViewModel() {

    private val sessionId: String = UUID.randomUUID().toString()

    /**
     * 训练会话来自**容器**（application-scoped），不是 ViewModel 自有。
     *
     * 这样锁屏/重建时引擎状态不丢，AC-05「进度误差 ≤1s」才成立。
     */
    private val coordinator: TrainingSessionCoordinator = container.obtainTrainingSession(params)

    private val _state = MutableStateFlow(TrainingUiState(params = params))
    val state: StateFlow<TrainingUiState> = _state.asStateFlow()

    private var started = false
    private var savedId: String? = null
    /** 演示模式（FR-14）。决定写库时的 sensorMode，绝不能被当成真实记录 */
    private var demoMode: Boolean = false

    init {
        // 进页面先查有没有未完成的训练（AC-13），没有就立刻开始新的一场。
        // 首版漏了"立刻开始"这一步，页面永远停在「等待开始」——
        // 真机验证时才发现（倒计时一直是 00:00）。
        viewModelScope.launch {
            val settings = container.settingsStore.current()
            val clinical = settings.screening.hasRiskAnswers
            demoMode = settings.isDemoMode
            // 先看内存里是否已有**正在进行**的会话（锁屏/重建后重新进入）
            val running = coordinator.currentFrame()
            val hasRunning = running.status == TrainingStatus.RUNNING ||
                running.status == TrainingStatus.PAUSED
            if (hasRunning) {
                // 认领进行中的会话：不弹恢复对话框、不重置进度
                started = true
                _state.value = _state.value.copy(clinicalNotice = clinical)
                observeFrames()
                return@launch
            }
            val pending = coordinator.pendingSnapshot()?.takeIf { it.isResumable(params) }
            _state.value = _state.value.copy(
                pendingRecovery = pending,
                clinicalNotice = clinical,
                voiceAvailable = container.voiceGuide.isAvailable,
            )
            if (pending == null) start()
        }
    }

    // ── 启动与恢复 ────────────────────────────────────────────────────────

    /** 用户选择「继续上次」*/
    fun continueFromSnapshot() {
        val snap = _state.value.pendingRecovery ?: return
        viewModelScope.launch {
            val ok = coordinator.restoreFrom(snap)
            started = true
            if (!ok) {
                // 参数变了：不能继续，只能丢弃
                container.snapshotStore.clear()
                coordinator.start()
            }
            _state.value = _state.value.copy(pendingRecovery = null)
            observeFrames()
        }
    }

    /** 用户选择「丢弃」：清掉快照后立刻重开一场。 */
    fun discardSnapshot() {
        viewModelScope.launch {
            container.snapshotStore.clear()
            _state.value = _state.value.copy(pendingRecovery = null)
            start()
        }
    }

    /** 正常开始一场新训练。 */
    fun start() {
        if (started) return
        started = true
        coordinator.start()
        observeFrames()
    }

    private var observing = false

    private fun observeFrames() {
        if (observing) return
        observing = true
        viewModelScope.launch {
            coordinator.frames.collect { frame ->
                _state.value = _state.value.copy(
                    frame = frame,
                    paused = frame.status == TrainingStatus.PAUSED,
                )
                if (frame.status == TrainingStatus.COMPLETE && savedId == null) {
                    finishSession(frame)
                }
            }
        }
    }

    // ── 交互 ──────────────────────────────────────────────────────────────

    fun togglePause() {
        if (_state.value.paused) coordinator.resume() else coordinator.pause()
        _state.value = _state.value.copy(paused = _state.value.paused.not())
    }

    fun requestStop() {
        _state.value = _state.value.copy(showStopConfirm = true)
    }

    fun cancelStop() {
        _state.value = _state.value.copy(showStopConfirm = false)
    }

    /** 二次确认后结束：保存已完成次数，不补造整组（AC-07）。 */
    fun confirmStop() {
        _state.value = _state.value.copy(showStopConfirm = false)
        coordinator.stop()
        if (savedId == null) {
            finishSession(coordinator.currentFrame())
        }
    }

    fun repeatVoice() {
        coordinator.repeatVoice()
        _state.value = _state.value.copy(voiceAvailable = container.voiceGuide.isAvailable)
    }

    /**
     * 离开页面。
     *
     * **不能 release 会话**：会话是 application-scoped 的，训练中锁屏或重建
     * 后还要继续用它。真正的释放发生在训练结束（[confirmStop] / 自然完成）
     * 与容器销毁时。
     */
    fun onClearedComposition() {
        // 故意留空：只保留一个明确的调用点，避免以后有人误以为这里该 release
    }

    override fun onCleared() {
        super.onCleared()
        // 同上：不在这里 release。若用户杀掉进程，快照会兜住（AC-13）。
    }

    // ── 保存（唯一入口）──────────────────────────────────────────────────

    private fun finishSession(frame: TrainingFrame) {
        if (savedId != null) return
        savedId = sessionId

        val nowWall = System.currentTimeMillis()
        val startedWall = coordinator.startedAtWallMs()
        val elapsed = coordinator.elapsedActiveMs()
        val draft = SessionDraft(
            // 关键：用 init 里生成并一直持有的 id，不在保存时新生成（AC-08）
            id = sessionId,
            startedAt = startedWall,
            endedAt = nowWall,
            // 时长读单调时钟，不用墙钟差（训练中校时会污染）
            durationMs = elapsed,
            planVersion = params.hash(),
            plannedReps = params.repsPerSet,
            completedReps = frame.completedReps,
            pauseCount = coordinator.pauseCount(),
            pausedMs = coordinator.pausedMs(),
            // FR-14：演示模式产生的记录必须标 simulated，
            // 否则 `countsTowardProgress = !mode.isSimulated` 会把它
            // 计入打卡与周报，等于演示数据污染真实统计（AC-19）。
            mode = if (demoMode) SensorMode.SIMULATED else SensorMode.OFF,
            // 未接真实感知，覆盖率无从谈起 —— 如实为 null，
            // 由 SessionRepository 的 60% 门限保证不会带出假指标。
            signalCoverage = null,
        )

        viewModelScope.launch {
            runCatching { container.sessionRepository.save(draft) }
                .onSuccess {
                    _state.value = _state.value.copy(finished = true, savedSessionId = sessionId)
                    // 训练已结束，可以释放会话了
                    container.releaseTrainingSession()
                }
                .onFailure {
                    // 保存失败必须让用户知道，并且释放会话 ——
                    // 否则 `_activeTraining` 留在"已 finished"状态，
                    // 下次 obtainTrainingSession 会静默丢弃它（记录静默丢失）。
                    _state.value = _state.value.copy(
                        finished = true,
                        savedSessionId = null,
                        saveFailed = true,
                    )
                    container.releaseTrainingSession()
                }
        }
    }
}