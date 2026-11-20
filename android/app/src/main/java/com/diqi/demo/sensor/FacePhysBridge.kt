package com.diqi.demo.sensor

import android.content.Context
import com.facephys.rppg.FrameResult
import com.facephys.rppg.InitErrorCode
import com.facephys.rppg.InitResult
import com.facephys.rppg.MetricsRead
import com.facephys.rppg.RppgConfig
import com.facephys.rppg.RppgEngine
import com.facephys.rppg.SdkDiagnostics
import com.facephys.rppg.StubRppgEngine
import java.io.IOException

/**
 * **FacePhys SDK 的唯一接触点。**
 *
 * 整个 app 只有这一个文件 import `com.facephys.rppg.*`。真实 AAR 到手时，
 * 改动就落在这里 + `app/build.gradle.kts` 一行依赖，其余代码（SensorAdapter /
 * QualityGate / UI）全部不受影响。
 *
 * 职责刻意收得很窄：
 *  1. **状态机**：保证 release 后绝不会再投喂（真实 SDK 可能原生 SIGSEGV，
 *     这是唯一能防住的地方）
 *  2. **线程归一化**：初始化回调可能来自任意线程，统一 post 回主线程
 *  3. **失败分类 → 用户可读文案**：FR-03 要求「失败原因必须可解释」，
 *     五种原因必须能明确区分，不能笼统说"初始化失败"
 *  4. **模型资源存在性检查**：`.enc` 缺失是最常见的部署事故，必须在初始化前查
 *
 * 刻意**不**做的事：不判断质量四态（那是 QualityGate 的事）、不持有相机、
 * 不做指标平滑（FR-07 明确禁止平滑造假）。
 *
 * 可测性设计：构造函数接收 [assetExists] 与 [postToUi]，因此纯 JVM 单测
 * 可以注入假引擎验证状态机，不需要 Android 环境。
 */
class FacePhysBridge(
    private val engine: RppgEngine,
    private val assetExists: (String) -> Boolean,
    private val postToUi: (() -> Unit) -> Unit,
) {

    enum class State { UNINITIALIZED, INITIALIZING, READY, FAILED, RELEASED }

    var state: State = State.UNINITIALIZED
        private set

    var lastFailure: SensorFailure? = null
        private set

    private var lastDiagnostics: SdkDiagnostics = SdkDiagnostics(
        sdkVersion = UNKNOWN_VERSION,
        licenseDaysLeft = null,
        initialized = false,
        modelAssetPresent = false,
    )

    /**
     * 初始化。
     *
     * @param onResult 一定在主线程回调一次（成功或失败）
     */
    fun init(appKey: String, encAssetName: String, onResult: (Result) -> Unit) {
        if (state == State.INITIALIZING || state == State.READY) return
        if (state == State.RELEASED) {
            postToUi { onResult(Result.Failure(SensorFailure.SDK_UNAVAILABLE)) }
            return
        }
        state = State.INITIALIZING

        val modelPresent = assetExists(encAssetName)
        lastDiagnostics = lastDiagnostics.copy(modelAssetPresent = modelPresent)

        if (!modelPresent) {
            finish(Result.Failure(SensorFailure.MODEL_MISSING), onResult)
            return
        }
        if (appKey.isBlank()) {
            finish(Result.Failure(SensorFailure.NOT_ACTIVATED), onResult)
            return
        }

        // 真实 SDK 的回调可能来自任意线程；这里不信任何实现，
        // 统一归一化到 UI 线程，避免调用方到处写 runOnUiThread。
        engine.initialize(
            RppgConfig(context = appContextOrNull(), encAssetName = encAssetName, appKey = appKey),
        ) { initResult ->
            postToUi {
                if (initResult.success) {
                    state = State.READY
                    lastFailure = null
                    refreshDiagnostics()
                    onResult(Result.Success)
                } else {
                    val failure = mapFailure(initResult.code)
                    state = State.FAILED
                    lastFailure = failure
                    refreshDiagnostics(initialized = false)
                    onResult(Result.Failure(failure))
                }
            }
        }
    }

    private fun finish(result: Result, onResult: (Result) -> Unit) {
        if (result is Result.Failure) {
            state = State.FAILED
            lastFailure = result.failure
        }
        refreshDiagnostics(initialized = state == State.READY)
        postToUi { onResult(result) }
    }

    /** 投喂一帧。非 READY 状态直接拒绝，不触碰原生层。 */
    fun feed(roi: ByteArray, dtSec: Float): FrameResult {
        if (state != State.READY) {
            return when (state) {
                State.RELEASED -> FrameResult.ERR_RELEASED
                else -> FrameResult.ERR_NOT_INITIALIZED
            }
        }
        return engine.processFrame(roi, dtSec)
    }

    /** 读指标三态。非 READY 一律 NotReady。 */
    fun read(): MetricsRead =
        if (state == State.READY) engine.readMetrics() else MetricsRead.NotReady

    val hasEnoughWindow: Boolean
        get() = state == State.READY && engine.hasEnoughWindow

    val windowAgeMs: Long
        get() = if (state == State.READY) engine.windowAgeMs else Long.MAX_VALUE

    fun hrvAsync(onResult: (Float?) -> Unit) {
        if (state != State.READY) {
            onResult(null); return
        }
        engine.getHrvAsync { onResult(it?.rmssdMs) }
    }

    fun reset() {
        if (state == State.READY) engine.reset()
    }

    /** 释放。之后任何 feed 都会被状态机挡住。 */
    fun release() {
        if (state == State.READY || state == State.INITIALIZING) {
            runCatching { engine.release() }
        }
        state = State.RELEASED
        refreshDiagnostics(initialized = false)
    }

    fun diagnostics(): SdkDiagnostics = lastDiagnostics

    private fun refreshDiagnostics(initialized: Boolean = state == State.READY) {
        lastDiagnostics = runCatching {
            engine.diagnostics().copy(initialized = initialized)
        }.getOrElse {
            lastDiagnostics.copy(initialized = initialized)
        }
    }

    private fun appContextOrNull(): Context? = contextRef

    /** 由 [create] 注入；纯 JVM 测试时为 null（桩忽略它）。 */
    private var contextRef: Context? = null

    sealed interface Result {
        data object Success : Result
        data class Failure(val failure: SensorFailure) : Result
    }

    companion object {
        const val UNKNOWN_VERSION = "—"

        /** 生产入口。 */
        fun create(context: Context): FacePhysBridge {
            val app = context.applicationContext
            val bridge = FacePhysBridge(
                engine = StubRppgEngine(),
                assetExists = { name -> assetPresent(app, name) },
                postToUi = { block -> MainPoster.post(block) },
            )
            bridge.contextRef = app
            return bridge
        }

        private fun assetPresent(context: Context, name: String): Boolean = runCatching {
            context.assets.open(name).use { true }
        }.getOrElse { e -> if (e is IOException) false else false }
    }
}

/**
 * 感知失败的可解释分类（FR-03「失败原因必须可解释」）。
 *
 * [userMessage] 是唯一允许展示给用户的文案来源 —— 不要在别处再写一份，
 * 否则 P03 的失败原因和设置页的诊断会不一致。
 */
enum class SensorFailure(val userMessage: String) {
    NOT_AUTHORIZED("没有获得相机权限，可以在系统设置里打开"),
    NOT_ACTIVATED("还没有激活感知功能，请先联网完成一次激活"),
    PACKAGE_MISMATCH("激活信息与当前应用不匹配，请联系支持"),
    NO_NETWORK("首次激活需要联网，请检查网络"),
    QUOTA_EXCEEDED("激活次数已用完，请联系支持"),
    MODEL_MISSING("感知模型文件缺失，请重新安装应用"),
    SDK_UNAVAILABLE("感知功能当前不可用，不影响正常训练"),
}

internal fun mapFailure(code: InitErrorCode): SensorFailure = when (code) {
    InitErrorCode.NOT_AUTHORIZED -> SensorFailure.NOT_AUTHORIZED
    InitErrorCode.NOT_ACTIVATED -> SensorFailure.NOT_ACTIVATED
    InitErrorCode.PACKAGE_MISMATCH -> SensorFailure.PACKAGE_MISMATCH
    InitErrorCode.NO_NETWORK -> SensorFailure.NO_NETWORK
    InitErrorCode.QUOTA_EXCEEDED -> SensorFailure.QUOTA_EXCEEDED
    InitErrorCode.MODEL_MISSING -> SensorFailure.MODEL_MISSING
    InitErrorCode.OK, InitErrorCode.UNKNOWN -> SensorFailure.SDK_UNAVAILABLE
}

/** 极薄的线程投递抽象，便于单测替换。 */
internal object MainPoster {
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    fun post(block: () -> Unit) {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            block()
        } else {
            handler.post(block)
        }
    }
}