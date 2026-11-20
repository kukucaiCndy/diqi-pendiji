package com.diqi.demo.training

import com.diqi.demo.audio.AssetVoiceGuide
import com.diqi.demo.audio.FocusAction
import com.diqi.demo.audio.SilentVoiceGuide
import com.diqi.demo.audio.VoiceScript
import com.diqi.demo.core.time.TestClock
import com.diqi.demo.data.prefs.SnapshotStore
import com.diqi.demo.domain.training.EngineSnapshot
import com.diqi.demo.domain.training.TrainingEngine
import com.diqi.demo.domain.training.TrainingParams
import com.diqi.demo.domain.training.TrainingPhase
import com.diqi.demo.domain.training.TrainingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 训练会话协调器测试。
 *
 * ## 为什么必须有这一层测试
 *
 * 评审指出：引擎的单测在测一个"没人使用的库" —— 把生产侧时钟换成
 * `uptimeMillis`、把引擎放进 `remember{}` 导致每次重组重建、或干脆不调 `start()`，
 * 引擎自己的测试依然全绿。协调器是引擎**唯一**的生产接入点，
 * 所以它的测试才是真正保护链路的那一层。
 */
class TrainingSessionCoordinatorTest {

    private val params = TrainingParams.DEFAULT
    private lateinit var clock: TestClock
    private lateinit var engine: TrainingEngine
    private lateinit var voice: SilentVoiceGuide
    private lateinit var snapshots: FakeSnapshotStore
    private lateinit var scope: CoroutineScope
    private lateinit var coordinator: TrainingSessionCoordinator

    private fun build() {
        clock = TestClock(5_000_000L)
        engine = TrainingEngine(clock, params)
        voice = SilentVoiceGuide()
        snapshots = FakeSnapshotStore()
        scope = CoroutineScope(Job() + Dispatchers.Default)
        coordinator = TrainingSessionCoordinator(
            engine = engine,
            clock = clock,
            voice = voice,
            snapshots = snapshots,
            scope = scope,
            params = params,
        )
    }

    @Test
    fun setUpPlaceholder() = Unit

    @After
    fun tearDown() {
        if (::scope.isInitialized) scope.cancel()
    }

    // 因为 @Before 在这里不便于控制构建顺序，统一用显式 build()
    private fun init() = build()

    /**
     * 等待异步快照落盘。
     *
     * `pause()` 是非 suspend 的 UI 入口，快照写入放在 scope 里异步做 ——
     * 测试必须显式等待，否则会读到"还没写进去"的 null 而误判。
     */
    private fun awaitSnapshot(timeoutMs: Long = 2_000L): EngineSnapshot? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val s = runBlocking { snapshots.load() }
            if (s != null) return s
            Thread.sleep(20)
        }
        return null
    }

    @Test
    fun 开始训练会播开场语音并进入准备阶段() {
        init()
        coordinator.start(wallClockMs = 1_700_000_000_000L)

        assertEquals(TrainingPhase.PREPARATION, coordinator.frames.value.phase)
        assertEquals(TrainingStatus.RUNNING, coordinator.frames.value.status)
        assertTrue("应播放开场语音", voice.played.contains(VoiceScript.START))
    }

    @Test
    fun 开始后传入了墙钟时间_用于写库() {
        init()
        coordinator.start(wallClockMs = 1_700_000_000_000L)
        assertEquals(1_700_000_000_000L, coordinator.startedAtWallMs())
    }

    @Test
    fun 阶段变化时才播语音_不会每帧重复() {
        init()
        coordinator.start()
        // start() 内已同步 emit 第一帧 → 应播过一次 PREP
        assertEquals(1, voice.played.count { it == VoiceScript.PREP })

        // 仍在准备阶段内前进 1 秒并暂停：阶段没变，不应再播 PREP
        clock.advance(1_000L)
        coordinator.pause()
        assertEquals(
            "同一阶段内不得重复播放提示语音",
            1,
            voice.played.count { it == VoiceScript.PREP },
        )
        // 但暂停本身应有语音
        assertEquals(1, voice.played.count { it == VoiceScript.PAUSE })
    }

    @Test
    fun 暂停会播暂停语音且时间冻结() {
        init()
        coordinator.start()
        clock.advance(params.prepMs)
        coordinator.pause()

        assertTrue(voice.played.contains(VoiceScript.PAUSE))
        val frozen = coordinator.elapsedActiveMs()
        clock.advance(10_000L)
        assertEquals(frozen, coordinator.elapsedActiveMs())
        assertEquals(1, coordinator.pauseCount())
    }

    @Test
    fun 结束后完成次数不补造() {
        init()
        coordinator.start()
        clock.advance(params.prepMs + params.cycleMs * 2 + 2_000L)
        coordinator.stop()

        assertEquals(2, coordinator.currentFrame().completedReps)
        clock.advance(200_000L)
        assertEquals("结束后时间冻结，不得补成满组", 2, coordinator.currentFrame().completedReps)
    }

    @Test
    fun 暂停时写入快照_内容可再次读回() {
        init()
        coordinator.start(wallClockMs = 1_700_000_000_000L)
        clock.advance(params.prepMs + 1_000L)
        coordinator.pause()

        val saved = awaitSnapshot()
        assertNotNull("暂停后应有快照", saved)
        assertEquals(params.hash(), saved!!.planVersion)
        assertEquals(params.prepMs + 1_000L, saved.elapsedActiveMs)
        assertTrue(saved.wasPaused)
    }

    @Test
    fun 从快照恢复后剩余时间与快照一致() {
        init()
        coordinator.start()
        clock.advance(params.prepMs + 3_000L)
        coordinator.pause()
        val snap = awaitSnapshot() ?: error("暂停后应有快照")
        val remainingAtPause = coordinator.currentFrame().phaseRemainingMs

        // 模拟进程重启
        clock = TestClock(0L)
        engine = TrainingEngine(clock, params)
        voice = SilentVoiceGuide()
        coordinator = TrainingSessionCoordinator(
            engine = engine,
            clock = clock,
            voice = voice,
            snapshots = snapshots,
            scope = scope,
            params = params,
        )

        val ok = runBlocking { coordinator.restoreFrom(snap) }
        assertTrue(ok)
        assertEquals(remainingAtPause, coordinator.currentFrame().phaseRemainingMs)
        assertEquals(TrainingStatus.PAUSED, coordinator.currentFrame().status)
    }

    @Test
    fun 参数变更后恢复被拒绝_应提示丢弃() {
        init()
        coordinator.start()
        clock.advance(params.prepMs)
        coordinator.pause()
        val snap = awaitSnapshot() ?: error("暂停后应有快照")

        // 临床改了收缩时长
        val newParams = params.copy(contractSec = 8)
        val newEngine = TrainingEngine(TestClock(0L), newParams)
        val newCoordinator = TrainingSessionCoordinator(
            engine = newEngine,
            clock = TestClock(0L),
            voice = SilentVoiceGuide(),
            snapshots = snapshots,
            scope = scope,
            params = newParams,
        )
        assertFalse(runBlocking { newCoordinator.restoreFrom(snap) })
    }

    @Test
    fun 结束训练会清空快照_避免下次误提示继续() {
        init()
        coordinator.start()
        clock.advance(params.prepMs)
        coordinator.pause()
        assertNotNull(awaitSnapshot())

        coordinator.resume()
        coordinator.stop()
        // stop() 里的 clear 是异步的，轮询等待
        val deadline = System.currentTimeMillis() + 2_000L
        var cleared = false
        while (System.currentTimeMillis() < deadline) {
            if (runBlocking { snapshots.load() } == null) { cleared = true; break }
            Thread.sleep(20)
        }
        assertTrue("结束后应清空快照，避免下次误提示继续", cleared)
    }

    // ── AC-06 的纯函数部分 ────────────────────────────────────────────────

    @Test
    fun 音频焦点被抢占只暂停当前播放_不通知训练层() {
        assertEquals(
            FocusAction.PAUSE_CURRENT,
            AssetVoiceGuide.focusActionFor(android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT),
        )
        assertEquals(
            FocusAction.PAUSE_CURRENT,
            AssetVoiceGuide.focusActionFor(
                android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
            ),
        )
        // 长期失去焦点必须真正放弃，否则复用旧请求会永远拿不到焦点
        assertEquals(
            FocusAction.ABANDON_FOCUS,
            AssetVoiceGuide.focusActionFor(android.media.AudioManager.AUDIOFOCUS_LOSS),
        )
        assertEquals(
            FocusAction.NOTHING,
            AssetVoiceGuide.focusActionFor(android.media.AudioManager.AUDIOFOCUS_GAIN),
        )
    }

    /** 内存快照存储替身。 */
    private class FakeSnapshotStore : SnapshotStore {
        @Volatile
        private var snapshot: EngineSnapshot? = null

        override suspend fun load(): EngineSnapshot? = snapshot
        override suspend fun save(snapshot: EngineSnapshot) {
            this.snapshot = snapshot
        }

        override suspend fun clear() {
            snapshot = null
        }
    }
}