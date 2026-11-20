package com.diqi.demo.ui.screens.training

import com.diqi.demo.audio.SilentVoiceGuide
import com.diqi.demo.audio.VoiceGuide
import com.diqi.demo.core.time.TestClock
import com.diqi.demo.data.db.SensorMode
import com.diqi.demo.data.prefs.AppSettings
import com.diqi.demo.data.prefs.SnapshotStore
import com.diqi.demo.data.repo.SessionDraft
import com.diqi.demo.domain.training.EngineSnapshot
import com.diqi.demo.domain.training.TrainingFrame
import com.diqi.demo.domain.training.TrainingParams
import com.diqi.demo.domain.training.TrainingPhase
import com.diqi.demo.domain.training.TrainingStatus
import com.diqi.demo.sensor.NullSensor
import com.diqi.demo.sensor.SensorAdapter
import com.diqi.demo.training.TrainingDeps
import com.diqi.demo.training.TrainingSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * `TrainingViewModel` 行为测试（JVM，无需设备）。
 *
 * ## 关键设计：用**最小会话桩**，不用真实协调器
 *
 * 上一版注真实 `TrainingSessionCoordinator`，结果 `./gradlew test` 挂死 10 分钟：
 * 协调器内置永续 ticker（`while (isActive) { … delay(100) }`，仅训练完成才 break），
 * 而测试用 `TestClock`（时钟不随虚拟时间自增）——`advanceUntilIdle()` 无限推进时间。
 *
 * 现在 VM 依赖 `TrainingSession` 接口，这里注入 [StubSession]：
 * `frames` 由测试手动 emit，写操作只记调用。单元测试不该把协程调度器拖进来。
 */
class TrainingViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── 最小会话桩 ────────────────────────────────────────────────────────

    private class StubSession : TrainingSession {
        private val _frames = MutableStateFlow(TrainingFrame.idle(8))
        override val frames: StateFlow<TrainingFrame> = _frames

        var startedAtWall = 1_700_000_000_000L
        var started = false
        var pauseCountValue = 0
        var pausedMsValue = 0L
        var elapsedValue = 0L
        var completedReps = 0
        var finished = false
        var released = false
        /** 测试可设：让 VM 的 init 认为存在可恢复快照（AC-13）*/
        var pendingSnap: EngineSnapshot? = null

        fun emit(status: TrainingStatus, reps: Int = completedReps) {
            completedReps = reps
            _frames.value = TrainingFrame(
                status = status,
                phase = TrainingPhase.CONTRACT,
                phaseRemainingMs = 5_000L,
                phaseTotalMs = 5_000L,
                repIndex = reps + 1,
                totalReps = 8,
                overallProgress = 0.1f,
                completedReps = reps,
            )
        }

        override fun start(wallClockMs: Long) {
            started = true
            startedAtWall = wallClockMs
            emit(TrainingStatus.RUNNING)
        }

        /**
         * 必须与真实引擎语义一致：**已结束/已冻结时 pause() 是空操作**。
         *
         * 桩如果不忠实，测出来的就不是产品行为。第一版这里无条件 emit(PAUSED)，
         * 于是"结束后点暂停"的用例反而失败 —— 而真实引擎在这种情况下是不动的。
         */
        override fun pause() {
            if (finished) return
            pauseCountValue++
            emit(TrainingStatus.PAUSED)
        }

        override fun resume() = emit(TrainingStatus.RUNNING)

        override fun stop() {
            finished = true
            emit(TrainingStatus.COMPLETE)
        }

        override fun release() {
            released = true
        }

        override fun repeatVoice() = Unit

        override suspend fun restoreFrom(snapshot: EngineSnapshot): Boolean {
            started = true
            emit(TrainingStatus.RUNNING)
            return true
        }

        override suspend fun pendingSnapshot(): EngineSnapshot? = pendingSnap

        override fun currentFrame(): TrainingFrame = _frames.value

        override fun pauseCount(): Int = pauseCountValue

        override fun pausedMs(): Long = pausedMsValue

        override fun elapsedActiveMs(): Long = elapsedValue

        override fun startedAtWallMs(): Long = startedAtWall

        override fun isFinished(): Boolean = finished
    }

    private class FakeSnapshotStore(var stored: EngineSnapshot? = null) : SnapshotStore {
        override suspend fun load(): EngineSnapshot? = stored
        override suspend fun save(snapshot: EngineSnapshot) {
            stored = snapshot
        }

        override suspend fun clear() {
            stored = null
        }
    }

    private class FakeDeps(
        override val clock: TestClock,
        var settings: AppSettings = AppSettings(),
        var failSave: Boolean = false,
        override val snapshots: FakeSnapshotStore = FakeSnapshotStore(),
        var pending: EngineSnapshot? = null,
    ) : TrainingDeps {
        val saved = mutableListOf<SessionDraft>()
        var released = false
        val session = StubSession()

        override val voice: VoiceGuide = SilentVoiceGuide()

        override suspend fun currentSettings(): AppSettings = settings

        override suspend fun saveSession(draft: SessionDraft) {
            if (failSave) error("模拟写入失败")
            saved += draft
        }

        override fun sensorFor(demoMode: Boolean): SensorAdapter = NullSensor()

        override fun obtainSession(params: TrainingParams): TrainingSession {
            session.pendingSnap = pending
            return session
        }

        override fun releaseSession() {
            released = true
        }
    }

    // ── AC-08：只保存一次 ────────────────────────────────────────────────

    @Test
    fun 连点结束只保存一条记录() = runTest(dispatcher) {
        val deps = FakeDeps(TestClock(1_000_000L))
        val vm = TrainingViewModel(deps, TrainingParams.DEFAULT)
        testScheduler.runCurrent()

        vm.confirmStop()
        testScheduler.runCurrent()
        vm.confirmStop()
        testScheduler.runCurrent()

        assertEquals("AC-08：只应写入一条", 1, deps.saved.size)
        assertNotNull("应带 sessionId 去完成页", vm.state.value.savedSessionId)
    }

    @Test
    fun 保存的会话id与完成页展示的一致() = runTest(dispatcher) {
        val deps = FakeDeps(TestClock(1_000_000L))
        val vm = TrainingViewModel(deps, TrainingParams.DEFAULT)
        testScheduler.runCurrent()
        vm.confirmStop()
        testScheduler.runCurrent()
        assertEquals(vm.state.value.savedSessionId, deps.saved.first().id)
    }

    // ── AC-19：演示模式写 simulated ─────────────────────────────────────

    @Test
    fun 演示模式写入_simulated() = runTest(dispatcher) {
        val deps = FakeDeps(TestClock(1_000_000L), settings = AppSettings(isDemoMode = true))
        val vm = TrainingViewModel(deps, TrainingParams.DEFAULT)
        testScheduler.runCurrent()
        vm.confirmStop()
        testScheduler.runCurrent()
        assertEquals(
            "AC-19：演示记录必须标 simulated",
            SensorMode.SIMULATED,
            deps.saved.first().mode,
        )
    }

    @Test
    fun 非演示模式写入_off() = runTest(dispatcher) {
        val deps = FakeDeps(TestClock(1_000_000L), settings = AppSettings(isDemoMode = false))
        val vm = TrainingViewModel(deps, TrainingParams.DEFAULT)
        testScheduler.runCurrent()
        vm.confirmStop()
        testScheduler.runCurrent()
        assertEquals(SensorMode.OFF, deps.saved.first().mode)
    }

    // ── AC-13：可恢复快照 ────────────────────────────────────────────────

    @Test
    fun 有可恢复快照时弹窗且不自动开始() = runTest(dispatcher) {
        val snap = EngineSnapshot(
            startedAtWallMs = 1_700_000_000_000L,
            accumulatedPausedMs = 0,
            pauseCount = 0,
            planVersion = TrainingParams.DEFAULT.hash(),
            plannedReps = 8,
            elapsedActiveMs = 60_000L,
            wasPaused = false,
        )
        val deps = FakeDeps(TestClock(1_000_000L), pending = snap)
        val vm = TrainingViewModel(deps, TrainingParams.DEFAULT)
        testScheduler.runCurrent()

        assertNotNull("AC-13：应弹出继续/丢弃", vm.state.value.pendingRecovery)
        assertFalse("不应自动开始训练", deps.session.started)
        assertEquals(0, deps.saved.size)
    }

    // ── 保存失败 ──────────────────────────────────────────────────────────

    @Test
    fun 保存失败时不置_finished_并置_saveFailed() = runTest(dispatcher) {
        val deps = FakeDeps(TestClock(1_000_000L), failSave = true)
        val vm = TrainingViewModel(deps, TrainingParams.DEFAULT)
        testScheduler.runCurrent()
        vm.confirmStop()
        testScheduler.runCurrent()

        assertTrue("必须让用户知道失败", vm.state.value.saveFailed)
        assertFalse(
            "不能置 finished —— 置了会立刻导航，失败提示只被组合一帧",
            vm.state.value.finished,
        )
        assertEquals(0, deps.saved.size)
    }

    @Test
    fun 保存失败后点放弃才允许离开() = runTest(dispatcher) {
        val deps = FakeDeps(TestClock(1_000_000L), failSave = true)
        val vm = TrainingViewModel(deps, TrainingParams.DEFAULT)
        testScheduler.runCurrent()
        vm.confirmStop()
        testScheduler.runCurrent()

        vm.abandonOnSaveFailure()
        testScheduler.runCurrent()

        assertTrue(vm.state.value.finished)
        assertFalse(vm.state.value.saveFailed)
        assertNull(vm.state.value.savedSessionId)
        assertTrue("应释放会话", deps.released)
    }

    // ── 状态一致性 ────────────────────────────────────────────────────────

    @Test
    fun 结束后点暂停不会乐观显示已暂停() = runTest(dispatcher) {
        val deps = FakeDeps(TestClock(1_000_000L))
        val vm = TrainingViewModel(deps, TrainingParams.DEFAULT)
        testScheduler.runCurrent()

        // 会话已完成：currentFrame 为 COMPLETE
        deps.session.finished = true
        deps.session.emit(TrainingStatus.COMPLETE)

        vm.togglePause()
        testScheduler.runCurrent()

        assertFalse("引擎已结束时界面不能乐观显示已暂停", vm.state.value.paused)
    }

    @Test
    fun 暂停后界面状态与会话一致() = runTest(dispatcher) {
        val deps = FakeDeps(TestClock(1_000_000L))
        val vm = TrainingViewModel(deps, TrainingParams.DEFAULT)
        testScheduler.runCurrent()

        vm.togglePause()
        testScheduler.runCurrent()
        assertTrue(vm.state.value.paused)

        vm.togglePause()
        testScheduler.runCurrent()
        assertFalse(vm.state.value.paused)
    }
}