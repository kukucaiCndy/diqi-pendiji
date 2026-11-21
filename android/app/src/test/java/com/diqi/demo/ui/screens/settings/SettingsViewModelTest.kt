package com.diqi.demo.ui.screens.settings

import com.diqi.demo.audio.SilentVoiceGuide
import com.diqi.demo.audio.VoiceGuide
import com.diqi.demo.data.prefs.AppSettings
import com.diqi.demo.data.prefs.SafetyScreening
import com.diqi.demo.data.prefs.ScreeningAnswer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * `SettingsViewModel` 行为测试（JVM，无需设备）。
 *
 * 仿 [com.diqi.demo.ui.screens.training.TrainingViewModelTest] 的容器/fake 搭建方式：
 * 用 [FakeSettingsDeps] 注入内存状态，不依赖 Room 或 DataStore。
 *
 * 覆盖：
 * - 开演示 → 关演示会 clearSimulated
 * - clearData 清业务数据但保留同意/筛查
 */
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Fake 依赖 ────────────────────────────────────────────────────────

    /**
     * 内存版设置依赖，不依赖 Room 或 DataStore。
     */
    private class FakeSettingsDeps(
        var settings: AppSettings = AppSettings(),
    ) : SettingsDeps {

        override val voice: VoiceGuide = SilentVoiceGuide()
        private val _settingsFlow = MutableStateFlow(settings)
        override val settingsFlow: Flow<AppSettings> get() = _settingsFlow

        var scheduledTime: String? = null
        var cancelled: Boolean = false
        var clearAllSessionsCalled = false
        var clearSimulatedCalled = false
        var clearAllLeakDiaryCalled = false

        private fun update(new: AppSettings) {
            settings = new
            _settingsFlow.value = new
        }

        override suspend fun currentSettings(): AppSettings = settings

        override suspend fun setReminderEnabled(enabled: Boolean) {
            update(settings.copy(reminderEnabled = enabled))
        }

        override suspend fun setReminderTime(hhmm: String) {
            update(settings.copy(reminderTime = hhmm))
        }

        override suspend fun setAudioSpeed(speed: Float) {
            update(settings.copy(audioSpeed = speed))
        }

        override suspend fun setDemoMode(enabled: Boolean) {
            update(settings.copy(isDemoMode = enabled))
        }

        override suspend fun clearBusinessSettings() {
            // 模拟 clearBusinessSettings：清业务偏好，保留同意/筛查
            update(
                AppSettings(
                    privacyNoticeAcceptedAt = settings.privacyNoticeAcceptedAt,
                    safetyAcknowledgedAt = settings.safetyAcknowledgedAt,
                    screening = settings.screening,
                    cameraConsentAt = settings.cameraConsentAt,
                    cameraRevokedAt = settings.cameraRevokedAt,
                    shareEnabled = settings.shareEnabled,
                    shareRevokedAt = settings.shareRevokedAt,
                ),
            )
        }

        override suspend fun clearAllSessions() {
            clearAllSessionsCalled = true
        }

        override suspend fun clearSimulatedSessions() {
            clearSimulatedCalled = true
        }

        override suspend fun clearAllLeakDiary() {
            clearAllLeakDiaryCalled = true
        }

        override fun scheduleReminder(timeHHmm: String) {
            scheduledTime = timeHHmm
        }

        override fun cancelReminder() {
            cancelled = true
        }
    }

    // ── 演示模式 ──────────────────────────────────────────────────────────

    @Test
    fun 关闭演示模式会清掉演示记录() = runTest(dispatcher) {
        val deps = FakeSettingsDeps(settings = AppSettings(isDemoMode = true))
        val vm = SettingsViewModel(deps)
        testScheduler.runCurrent()

        vm.setDemoMode(false)
        testScheduler.runCurrent()

        assertFalse("演示模式应已关闭", deps.settings.isDemoMode)
        assertTrue("关闭演示应调 clearSimulated", deps.clearSimulatedCalled)
    }

    @Test
    fun 开启演示模式不清演示记录() = runTest(dispatcher) {
        val deps = FakeSettingsDeps(settings = AppSettings(isDemoMode = false))
        val vm = SettingsViewModel(deps)
        testScheduler.runCurrent()

        vm.setDemoMode(true)
        testScheduler.runCurrent()

        assertTrue("演示模式应已开启", deps.settings.isDemoMode)
        assertFalse("开启演示不应调 clearSimulated", deps.clearSimulatedCalled)
    }

    // ── 清除数据 ──────────────────────────────────────────────────────────

    @Test
    fun 清除数据会清业务数据并标记cleared() = runTest(dispatcher) {
        val deps = FakeSettingsDeps(
            settings = AppSettings(
                audioSpeed = 1.2f,
                reminderEnabled = true,
                reminderTime = "08:30",
                privacyNoticeAcceptedAt = 1_700_000_000_000L,
                safetyAcknowledgedAt = 1_700_000_001_000L,
                screening = SafetyScreening(
                    q1ProfessionalGuidance = ScreeningAnswer.YES,
                    q2NewSymptoms = ScreeningAnswer.NO,
                    q3PregnancyOrSurgery = ScreeningAnswer.UNSURE,
                    q4CanFeelContraction = ScreeningAnswer.YES,
                ),
            ),
        )
        val vm = SettingsViewModel(deps)
        testScheduler.runCurrent()

        vm.clearData()
        testScheduler.runCurrent()

        assertTrue("应标记 cleared", vm.cleared.value)
        assertTrue("应清训练记录", deps.clearAllSessionsCalled)
        assertTrue("应清日记", deps.clearAllLeakDiaryCalled)
    }

    @Test
    fun 清除数据后同意与筛查留痕保留() = runTest(dispatcher) {
        val privacyAt = 1_700_000_000_000L
        val ackAt = 1_700_000_001_000L
        val screening = SafetyScreening(
            q1ProfessionalGuidance = ScreeningAnswer.YES,
            q2NewSymptoms = ScreeningAnswer.NO,
            q3PregnancyOrSurgery = ScreeningAnswer.UNSURE,
            q4CanFeelContraction = ScreeningAnswer.YES,
        )
        val deps = FakeSettingsDeps(
            settings = AppSettings(
                audioSpeed = 1.2f,
                reminderEnabled = true,
                reminderTime = "08:30",
                privacyNoticeAcceptedAt = privacyAt,
                safetyAcknowledgedAt = ackAt,
                screening = screening,
            ),
        )
        val vm = SettingsViewModel(deps)
        testScheduler.runCurrent()

        vm.clearData()
        testScheduler.runCurrent()

        assertEquals("隐私同意时间应保留", privacyAt, deps.settings.privacyNoticeAcceptedAt)
        assertEquals("筛查确认时间应保留", ackAt, deps.settings.safetyAcknowledgedAt)
        assertEquals("筛查答案应保留", screening, deps.settings.screening)
        // 业务偏好应被清除
        assertEquals("音频速度应回到默认", 1.0f, deps.settings.audioSpeed)
        assertFalse("提醒开关应回到默认", deps.settings.reminderEnabled)
    }

    // ── 提醒 ──────────────────────────────────────────────────────────────

    @Test
    fun 开启提醒会按当前时间调度() = runTest(dispatcher) {
        val deps = FakeSettingsDeps(settings = AppSettings(reminderEnabled = false, reminderTime = "21:30"))
        val vm = SettingsViewModel(deps)
        testScheduler.runCurrent()

        vm.setReminder(true)
        testScheduler.runCurrent()

        assertTrue("应已开启", deps.settings.reminderEnabled)
        assertEquals("应按设置里的时间调度", "21:30", deps.scheduledTime)
    }

    @Test
    fun 关闭提醒会取消调度() = runTest(dispatcher) {
        val deps = FakeSettingsDeps(settings = AppSettings(reminderEnabled = true))
        val vm = SettingsViewModel(deps)
        testScheduler.runCurrent()

        vm.setReminder(false)
        testScheduler.runCurrent()

        assertFalse("应已关闭", deps.settings.reminderEnabled)
        assertTrue("应取消调度", deps.cancelled)
    }

    @Test
    fun 设置提醒时间后若已开启则重排() = runTest(dispatcher) {
        val deps = FakeSettingsDeps(settings = AppSettings(reminderEnabled = true, reminderTime = "20:00"))
        val vm = SettingsViewModel(deps)
        testScheduler.runCurrent()

        vm.setReminderTime("07:15")
        testScheduler.runCurrent()

        assertEquals("时间应已更新", "07:15", deps.settings.reminderTime)
        assertTrue("应先取消旧调度", deps.cancelled)
        assertEquals("应按新时间重新调度", "07:15", deps.scheduledTime)
    }
}
