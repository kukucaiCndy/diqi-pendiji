package com.diqi.demo.ui.screens.home

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 首页感知行状态判定（JVM，无需设备）。
 *
 * ## 为什么测的是纯函数而不是整个 ViewModel
 *
 * `HomeViewModel` 的构造参数是具体的 `AppContainer`，内部直接取
 * `AppSettingsStore`（DataStore）与 `ProgressRepository`（Room），
 * JVM 单元测试里无法构造。这一段判定就是状态行的**全部**逻辑，
 * 抽成 [sensorStatusRowFor] 后可以在这里钉死，不依赖设备也不依赖数据库。
 *
 * 覆盖 brief P2：授权 → READY、未授权 → OFF。
 */
class HomeViewModelTest {

    @Test
    fun 授权有效时感知行显示已就绪() {
        assertEquals(
            "cameraConsentActive=true 应显示 READY「摄像头已就绪」",
            SensorStatusRow.READY,
            sensorStatusRowFor(cameraConsentActive = true),
        )
        assertEquals("摄像头已就绪", sensorStatusRowFor(true).label)
    }

    @Test
    fun 未授权时感知行显示未开启() {
        assertEquals(
            "cameraConsentActive=false 应显示 OFF「摄像头未开启 · 不影响练习」",
            SensorStatusRow.OFF,
            sensorStatusRowFor(cameraConsentActive = false),
        )
        assertEquals("摄像头未开启 · 不影响练习", sensorStatusRowFor(false).label)
    }
}
