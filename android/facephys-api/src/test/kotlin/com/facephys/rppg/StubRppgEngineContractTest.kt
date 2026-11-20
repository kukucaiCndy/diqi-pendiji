package com.facephys.rppg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 契约桩的行为测试。
 *
 * 这些断言的价值不在于"测试桩"（桩马上就要被真实 AAR 换掉），而在于
 * **把契约本身钉死**：真实 AAR 接入时，如果它违反了下面任何一条，
 * 这里会立刻变红，而不是等到训练时"永远没有数据"才发现。
 *
 * 尤其重要的是 [窗口攒够但无输出时必须判为 NoSignal 而不是 Ok] ——
 * 桩的行为直接决定了 QualityGate 落 NO_DATA 还是 DEGRADED，
 * 后者会在界面上显示"信号一般"并继续投喂，与需求 FR-08 相反。
 *
 * 这里不需要 mock Context：契约允许 [RppgConfig.context] 为 null，
 * 桩实现从不访问 assets。
 */
class StubRppgEngineContractTest {

    private fun config(appKey: String = "test-app-key") = RppgConfig(
        context = null,
        encAssetName = "facephys_model.enc",
        appKey = appKey,
    )

    private fun readyEngine(): StubRppgEngine {
        val engine = StubRppgEngine()
        engine.initialize(config()) { }
        return engine
    }

    private fun roi(): ByteArray = ByteArray(RppgEngine.ROI_BYTE_COUNT)

    @Test
    fun `缺 AppKey 时初始化为 NOT_ACTIVATED`() {
        val engine = StubRppgEngine()
        var captured: InitResult? = null
        engine.initialize(config(appKey = "")) { captured = it }
        val r = requireNotNull(captured)
        assertFalse(r.success)
        assertEquals(InitErrorCode.NOT_ACTIVATED, r.code)
    }

    @Test
    fun `缺模型资源名时初始化为 MODEL_MISSING`() {
        val engine = StubRppgEngine()
        var captured: InitResult? = null
        engine.initialize(
            RppgConfig(context = null, encAssetName = "", appKey = "k"),
        ) { captured = it }
        assertEquals(InitErrorCode.MODEL_MISSING, requireNotNull(captured).code)
    }

    @Test
    fun `正常配置初始化成功`() {
        var captured: InitResult? = null
        StubRppgEngine().initialize(config()) { captured = it }
        assertTrue(requireNotNull(captured).success)
    }

    @Test
    fun `ROI 长度不符被拒绝`() {
        val engine = readyEngine()
        assertEquals(FrameResult.ERR_BAD_ROI_SIZE, engine.processFrame(ByteArray(242), 0.083f))
        assertEquals(FrameResult.ERR_BAD_ROI_SIZE, engine.processFrame(ByteArray(244), 0.083f))
    }

    @Test
    fun `dtSec 非法被拒绝`() {
        val engine = readyEngine()
        assertEquals(FrameResult.ERR_BAD_DT, engine.processFrame(roi(), 0f))
        assertEquals(FrameResult.ERR_BAD_DT, engine.processFrame(roi(), -0.1f))
        assertEquals(FrameResult.ERR_BAD_DT, engine.processFrame(roi(), Float.NaN))
        // 超过上限（链路卡死信号）
        assertEquals(
            FrameResult.ERR_BAD_DT,
            engine.processFrame(roi(), RppgEngine.MAX_DT_SEC + 0.1f),
        )
    }

    @Test
    fun `未初始化时投喂返回 ERR_NOT_INITIALIZED`() {
        assertEquals(FrameResult.ERR_NOT_INITIALIZED, StubRppgEngine().processFrame(roi(), 0.083f))
    }

    @Test
    fun `release 之后投喂返回 ERR_RELEASED 而不是崩溃`() {
        val engine = readyEngine()
        engine.release()
        assertEquals(FrameResult.ERR_RELEASED, engine.processFrame(roi(), 0.083f))
    }

    @Test
    fun `并发投喂被检测为 ERR_CONCURRENT`() {
        val engine = readyEngine()
        val errors = java.util.concurrent.ConcurrentLinkedQueue<FrameResult>()
        val threads = (1..8).map {
            Thread {
                repeat(200) { errors.add(engine.processFrame(roi(), 0.083f)) }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        // 串行契约：任何一次并发都必须被拒绝，而不是被 synchronized 静默吞掉
        assertTrue(
            "并发投喂应至少被拒绝一次，实际全部为 OK —— 说明串行契约未被强制",
            errors.any { it == FrameResult.ERR_CONCURRENT },
        )
    }

    @Test
    fun `窗口未攒够时读数为 NotReady`() {
        val engine = readyEngine()
        assertEquals(MetricsRead.NotReady, engine.readMetrics())
        // 投喂 10 秒
        repeat(120) { engine.processFrame(roi(), 0.083f) }
        assertFalse(engine.hasEnoughWindow)
        assertEquals(MetricsRead.NotReady, engine.readMetrics())
    }

    @Test
    fun `窗口攒够但无输出时必须判为 NoSignal 而不是 Ok —— 合规红线`() {
        val engine = readyEngine()
        // 投喂超过 30s
        repeat(400) { engine.processFrame(roi(), 0.083f) }
        assertTrue("窗口应已攒够", engine.hasEnoughWindow)
        val read = engine.readMetrics()
        assertEquals(
            "桩必须返回 NoSignal：任何 Ok 都意味着产生了不可信的生理数值",
            MetricsRead.NoSignal,
            read,
        )
        assertNotEquals(MetricsRead.Ok::class, read::class)
    }

    @Test
    fun `HRV 通道永远返回 null`() {
        val engine = readyEngine()
        var captured: HrvResult? = HrvResult(1f, 2f)
        engine.getHrvAsync { captured = it }
        assertEquals(null, captured)
    }

    @Test
    fun `windowAgeMs 在未投喂时为极大值`() {
        val engine = readyEngine()
        assertEquals(Long.MAX_VALUE, engine.windowAgeMs)
    }

    @Test
    fun `reset 清空窗口但保持可用`() {
        val engine = readyEngine()
        repeat(400) { engine.processFrame(roi(), 0.083f) }
        assertTrue(engine.hasEnoughWindow)
        engine.reset()
        assertFalse(engine.hasEnoughWindow)
        assertEquals(FrameResult.OK, engine.processFrame(roi(), 0.083f))
    }

    @Test
    fun `诊断里的 sdkVersion 必须一眼看出是桩`() {
        val v = readyEngine().diagnostics().sdkVersion
        assertTrue(
            "sdkVersion 必须以 STUB 开头，否则 P03 截图会被误读成已集成真实 SDK",
            v.startsWith("STUB"),
        )
        // 绝不能冒充真实版本号
        assertFalse(v.contains("1.5.0"))
    }

    @Test
    fun `release 后诊断标记为未初始化且模型缺失`() {
        val engine = readyEngine()
        engine.release()
        val d = engine.diagnostics()
        assertFalse(d.initialized)
        assertFalse(d.modelAssetPresent)
    }
}