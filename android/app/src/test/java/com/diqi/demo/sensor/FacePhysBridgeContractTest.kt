package com.diqi.demo.sensor

import com.facephys.rppg.FrameResult
import com.facephys.rppg.MetricsRead
import com.facephys.rppg.StubRppgEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FacePhysBridge 状态机测试（纯 JVM，不依赖 Android）。
 *
 * 这是整个感知层最该被测的地方，因为它承担三条不能出错的职责：
 *  1. release 之后绝不能再触碰原生层（真实 SDK 会 SIGSEGV，只能在这里挡住）
 *  2. 初始化失败必须**可分类**（FR-03「失败原因必须可解释」）
 *  3. 无数据时必须传 [MetricsRead.NoSignal] 而不是伪造 [MetricsRead.Ok]
 *
 * 注入式设计让这里可以完全同步执行（`postToUi = { it() }`），
 * 不必依赖 Handler 或 Robolectric。
 */
class FacePhysBridgeContractTest {

    private fun bridge(
        engine: StubRppgEngine = StubRppgEngine(),
        assetPresent: Boolean = true,
    ): FacePhysBridge = FacePhysBridge(
        engine = engine,
        assetExists = { assetPresent },
        postToUi = { it() },
    )

    private fun ready(): FacePhysBridge = bridge().also {
        it.init("key", "model.enc") { }
    }

    private fun roi() = ByteArray(com.facephys.rppg.RppgEngine.ROI_BYTE_COUNT)

    @Test
    fun `模型资源缺失时报 MODEL_MISSING 且不进入 READY`() {
        val b = bridge(assetPresent = false)
        var result: FacePhysBridge.Result? = null
        b.init("key", "model.enc") { result = it }
        val failure = (result as FacePhysBridge.Result.Failure).failure
        assertEquals(SensorFailure.MODEL_MISSING, failure)
        assertEquals(FacePhysBridge.State.FAILED, b.state)
        assertNotNull(b.lastFailure)
    }

    @Test
    fun `缺 AppKey 时报 NOT_ACTIVATED`() {
        val b = bridge()
        var result: FacePhysBridge.Result? = null
        b.init("", "model.enc") { result = it }
        assertEquals(
            SensorFailure.NOT_ACTIVATED,
            (result as FacePhysBridge.Result.Failure).failure,
        )
    }

    @Test
    fun `正常初始化进入 READY`() {
        val b = ready()
        assertEquals(FacePhysBridge.State.READY, b.state)
        assertEquals(null, b.lastFailure)
    }

    @Test
    fun `未就绪时投喂被拒绝且不触碰引擎`() {
        val engine = StubRppgEngine()
        val b = bridge(engine = engine)
        assertEquals(FrameResult.ERR_NOT_INITIALIZED, b.feed(roi(), 0.083f))
        assertEquals("引擎不应收到任何帧", 0L to 0f, engine.debugStats())
    }

    @Test
    fun `release 之后投喂返回 ERR_RELEASED —— 真实 SDK 会崩，只能在这里挡住`() {
        val b = ready()
        b.release()
        assertEquals(FrameResult.ERR_RELEASED, b.feed(roi(), 0.083f))
        assertEquals(FacePhysBridge.State.RELEASED, b.state)
    }

    @Test
    fun `release 之后再次 init 直接失败而不复活`() {
        val b = ready()
        b.release()
        var result: FacePhysBridge.Result? = null
        b.init("key", "model.enc") { result = it }
        assertEquals(
            SensorFailure.SDK_UNAVAILABLE,
            (result as FacePhysBridge.Result.Failure).failure,
        )
        assertEquals(FacePhysBridge.State.RELEASED, b.state)
    }

    @Test
    fun `未就绪时读数为 NotReady`() {
        assertEquals(MetricsRead.NotReady, bridge().read())
    }

    @Test
    fun `窗口攒够后读数必须是 NoSignal —— 绝不能是 Ok`() {
        val b = ready()
        repeat(400) { b.feed(roi(), 0.083f) }
        assertTrue(b.hasEnoughWindow)
        val read = b.read()
        assertEquals(
            "桩没有推理能力，任何 Ok 都是伪造的生理数值（FR-08 / AC-14 红线）",
            MetricsRead.NoSignal,
            read,
        )
        assertFalse(read is MetricsRead.Ok)
    }

    @Test
    fun `HRV 在未就绪时返回 null 而不是崩溃`() {
        var value: Float? = 0f
        bridge().hrvAsync { value = it }
        assertEquals(null, value)
    }

    @Test
    fun `诊断里的版本号一眼可辨为桩`() {
        val d = ready().diagnostics()
        assertTrue(d.sdkVersion.startsWith("STUB"))
        assertFalse(d.sdkVersion.contains("1.5.0"))
        assertTrue(d.initialized)
    }

    @Test
    fun `诊断在 release 后标记为未初始化`() {
        val b = ready()
        b.release()
        assertFalse(b.diagnostics().initialized)
    }

    @Test
    fun `每一种失败都有非空且互不相同的可读文案`() {
        val messages = SensorFailure.entries.map { it.userMessage }
        assertTrue("存在空文案", messages.none { it.isBlank() })
        assertEquals(
            "失败文案必须两两不同，否则 P03 的「可解释」无从谈起",
            messages.size,
            messages.toSet().size,
        )
    }

    @Test
    fun `错误码到失败分类的映射完整覆盖`() {
        // 每个 InitErrorCode 都必须映射到某个 SensorFailure，不允许漏项
        com.facephys.rppg.InitErrorCode.entries.forEach { code ->
            assertNotNull("InitErrorCode.$code 未映射", mapFailure(code))
        }
        assertEquals(SensorFailure.MODEL_MISSING, mapFailure(com.facephys.rppg.InitErrorCode.MODEL_MISSING))
        assertEquals(
            SensorFailure.SDK_UNAVAILABLE,
            mapFailure(com.facephys.rppg.InitErrorCode.UNKNOWN),
        )
    }

    @Test
    fun `reset 后窗口清空`() {
        val b = ready()
        repeat(400) { b.feed(roi(), 0.083f) }
        assertTrue(b.hasEnoughWindow)
        b.reset()
        assertFalse(b.hasEnoughWindow)
    }

    @Test
    fun `已就绪时再次_init_立刻回调成功且窗口不被重置`() {
        // 共享 bridge 的关键契约：校准页攒满窗口 → 训练页 start 时再 init 一次
        val b = ready()
        repeat(400) { b.feed(roi(), 0.083f) }
        assertTrue(b.hasEnoughWindow)

        var result: FacePhysBridge.Result? = null
        b.init("key", "model.enc") { result = it }

        assertEquals(
            "已 READY 时必须立刻回报成功，否则新页面永远等不到「可以开始」",
            FacePhysBridge.Result.Success,
            result,
        )
        assertTrue("第二次 init 绝不许清空已有窗口（否则又要重等 30s）", b.hasEnoughWindow)
    }
}