package com.diqi.demo.ui.screens.calibrate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diqi.demo.AppContainer
import com.diqi.demo.BuildConfig
import com.diqi.demo.sensor.FacePhysBridge
import com.diqi.demo.sensor.SensorFailure
import com.diqi.demo.sensor.camera.FaceFrame
import com.facephys.rppg.FrameResult
import com.facephys.rppg.MetricsRead
import com.facephys.rppg.RppgEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** 校准页的四类实时反馈（FR-03）。 */
enum class CalibrationHint {
    NO_FACE,
    FACE_TOO_SMALL,
    FACE_TOO_LARGE,
    LOW_LIGHT,
    BUILDING,
    READY,
}

data class CalibrateUiState(
    val permissionAsked: Boolean = false,
    val faceFound: Boolean = false,
    val faceRatio: Float = 0f,
    /** 人脸框（相对预览的比例坐标 0–1），null 表示未检出 */
    val faceBoxLeft: Float? = null,
    val faceBoxTop: Float? = null,
    val faceBoxRight: Float? = null,
    val faceBoxBottom: Float? = null,
    val hint: CalibrationHint = CalibrationHint.BUILDING,
    /** 已建立信号的秒数 = 人脸在镜累计秒数（0..30，非墙钟；离镜 debounce 后归零）*/
    val signalSec: Int = 0,
    /** 前 30 秒的建立进度 0–1 */
    val signalProgress: Float = 0f,
    /** 就绪 = SDK 窗口地面真相（`bridge.hasEnoughWindow`），不是秒数到点 */
    val readReady: Boolean = false,
    /** SDK 诊断（折叠区）*/
    val sdkVersion: String = "—",
    val initOk: Boolean = false,
    val licenseDaysLeft: Int? = null,
    val sqi: Float? = null,
    /** 失败原因（可解释，FR-03）*/
    val failureMessage: String? = null,
    /** ROI 字节数，仅自检用 */
    val roiBytes: Int = 0,
    val feedOk: Boolean = false,
    val cameraError: String? = null,
) {
    val statusText: String
        get() = when (hint) {
            CalibrationHint.NO_FACE -> "请把脸放进框内"
            CalibrationHint.FACE_TOO_SMALL -> "请靠近一点"
            CalibrationHint.FACE_TOO_LARGE -> "请往后一点"
            CalibrationHint.LOW_LIGHT -> "光线有点暗，请面向光源"
            CalibrationHint.BUILDING -> "正在建立信号 ${signalSec}s / 约 30s"
            CalibrationHint.READY -> "已对准，可以开始了"
        }

    /** 条件不满足时主按钮禁用，但「跳过，直接训练」常驻（FR-03）*/
    val canStartWithSensor: Boolean get() = readReady
}

/**
 * 每秒一次的「人脸在镜累计」判定结果（纯数据，便于 JVM 单测）。
 *
 * @property sec 人脸在镜累计秒数（0..maxSec），**不是**从 init 起算的墙钟
 * @property absentTicks 连续"无人脸"的帧数（1 秒/帧），用于 debounce
 * @property shouldReset 本次是否应清零并调用 `bridge.reset()`
 * @property ready 就绪 = SDK 窗口地面真相（`bridge.hasEnoughWindow`），不看墙钟
 */
data class CalibrationTick(
    val sec: Int,
    val absentTicks: Int,
    val shouldReset: Boolean,
    val ready: Boolean,
)

/** 连续多少帧无人脸才判"离镜"（debounce，防单帧漏检就清零）。 */
const val ABSENT_FRAMES_TO_RESET = 2

/**
 * 校准计时/就绪的**纯函数**步进（无 Android 依赖，可直接 JVM 单测）。
 *
 * 规则（这就是修掉"对天花板也计时"的根因）：
 * 1. 有脸：累计 +1 秒，封顶 [CalibrateViewModel.HR_READY_SEC]；无人脸帧计数清零。
 * 2. 无脸：无人脸帧计数 +1；累计秒数保持不动（单帧漏检不清零）。
 * 3. 无脸连续 ≥ [ABSENT_FRAMES_TO_RESET] 帧：秒数归零，并置 `shouldReset`
 *    让调用方调用 `bridge.reset()` 清掉 SDK 内部窗口（重入镜真的从 0 开始）。
 *    只在跨过门限的那一帧返回 true，避免每帧重复 reset。
 * 4. `ready` 只认 SDK 是否攒满窗口（`windowFull`）；窗口被 reset 掉时必为 false。
 */
fun nextCalibrationTick(
    faceFound: Boolean,
    prevSec: Int,
    absentTicks: Int,
    windowFull: Boolean,
    maxSec: Int = CalibrateViewModel.HR_READY_SEC,
): CalibrationTick {
    if (faceFound) {
        return CalibrationTick(
            sec = (prevSec + 1).coerceIn(0, maxSec),
            absentTicks = 0,
            shouldReset = false,
            ready = windowFull,
        )
    }
    val absent = absentTicks + 1
    val shouldReset = absent == ABSENT_FRAMES_TO_RESET
    return CalibrationTick(
        // 单帧漏检（debounce 未满）保留原累计；跨过门限才归零。
        sec = if (shouldReset) 0 else prevSec.coerceIn(0, maxSec),
        absentTicks = absent,
        shouldReset = shouldReset,
        // reset 会清空 SDK 窗口，因此这一帧不可能"就绪"。
        ready = windowFull && !shouldReset,
    )
}

/**
 * 摄像头校准（P03）。
 *
 * ## 设计取舍
 *
 * 1. **未授权也能继续**：`跳过，直接训练` 常驻，且主按钮的禁用
 *    只影响"带感知开始"，不影响"纯语音开始"（AC-04）。
 * 2. **失败原因必须可解释**：未授权 / 未激活 / 包名不匹配 / 无网络 / 配额已满，
 *    以及"相机被占用""没有前置相机"这类设备原因，都落到 [CalibrateUiState.failureMessage]。
 * 3. **不伪造**：`sdkVersion` 来自 Bridge 诊断，当前是 `STUB-*`（一眼可辨），
 *    绝不假装已集成真实 1.5.0。
 */
class CalibrateViewModel(private val container: AppContainer) : ViewModel() {

    private val rppgEngine: RppgEngine = container.createRppgEngine()

    private val bridge: FacePhysBridge = FacePhysBridge.create(container.context, rppgEngine)

    private val _state = MutableStateFlow(CalibrateUiState())
    val state: StateFlow<CalibrateUiState> = _state.asStateFlow()

    /**
     * 人脸在镜累计秒数（0..[HR_READY_SEC]）。
     *
     * **不是** `elapsedRealtime() - initAt` 那种墙钟：人脸离开不累计，
     * 离镜 debounce 后归零，所以"先对着天花板 10s"不会白送 10 秒。
     */
    private var facePresentSec = 0

    /** 连续无人脸帧数（ticker 一跳 = 一帧），用于离镜 debounce 与清零判定。 */
    private var absentTicks = 0

    /** 供页面持有：真正的帧分析器（需要 imageProxy → ROI 的链路）*/
    val analyzer = com.diqi.demo.sensor.camera.FaceAnalyzer(
        onFeed = { roi, dtSec -> feed(roi, dtSec) },
        onFrame = { frame -> onFrame(frame) },
    )

    fun onPermissionResult(granted: Boolean) {
        _state.value = _state.value.copy(permissionAsked = true)
        if (!granted) {
            _state.value = _state.value.copy(
                failureMessage = SensorFailure.NOT_AUTHORIZED.userMessage,
                hint = CalibrationHint.NO_FACE,
            )
            return
        }
        initSdk()
    }

    private fun initSdk() {
        viewModelScope.launch {
            container.settingsStore.setCameraConsent(true, System.currentTimeMillis())
            // RealRppgEngine 内部用 step_mini.tflite.enc / state_mini.bin.enc；
            // StubRppgEngine 用 facephys_model.enc。
            // 这里传引擎对应的资产名给 Bridge 做预检（RealRppgEngine 内部仍用自己的常量）。
            val encAsset = if (BuildConfig.FACEPHYS_APP_KEY.isNotBlank()) REAL_ENC_ASSET
                else com.facephys.rppg.StubRppgEngine.ENC_ASSET
            bridge.init(
                appKey = BuildConfig.FACEPHYS_APP_KEY,
                encAssetName = encAsset,
            ) { result ->
                val diag = bridge.diagnostics()
                when (result) {
                    is FacePhysBridge.Result.Success -> {
                        _state.value = _state.value.copy(
                            initOk = true,
                            sdkVersion = diag.sdkVersion,
                            licenseDaysLeft = diag.licenseDaysLeft,
                            failureMessage = null,
                        )
                        facePresentSec = 0
                        absentTicks = 0
                        startTicker()
                    }
                    is FacePhysBridge.Result.Failure -> {
                        _state.value = _state.value.copy(
                            initOk = false,
                            sdkVersion = diag.sdkVersion,
                            licenseDaysLeft = diag.licenseDaysLeft,
                            failureMessage = result.failure.userMessage,
                        )
                    }
                }
            }
        }
    }

    private fun feed(roi: ByteArray, dtSec: Float): FrameResult {
        val r = bridge.feed(roi, dtSec)
        _state.value = _state.value.copy(
            roiBytes = roi.size,
            feedOk = r == FrameResult.OK,
        )
        return r
    }

    private fun onFrame(frame: FaceFrame) {
        val box = _state.value
        _state.value = box.copy(
            faceFound = frame.faceFound,
            faceRatio = frame.faceRatio,
            // 就绪只认 SDK 窗口（上次 ticker 回读的 readReady），不看墙钟秒数。
            hint = decideHint(frame, box.readReady),
        )
    }

    /**
     * 四类反馈的判定（FR-03）。
     *
     * 顺序很重要：先判"有没有脸"，再判"大小"，再判"光照"，
     * 最后才是"建立中/就绪"。否则用户在框内但脸太小的时候
     * 会看到"信号建立中"，完全不知道要靠近。
     *
     * @param ready [CalibrateUiState.readReady]，即 `bridge.hasEnoughWindow`
     *   —— 人脸在镜但 SDK 窗口未攒满时给 BUILDING，攒满才给 READY。
     */
    private fun decideHint(frame: FaceFrame, ready: Boolean): CalibrationHint {
        if (!frame.faceFound) return CalibrationHint.NO_FACE
        if (frame.faceRatio < MIN_FACE_RATIO) return CalibrationHint.FACE_TOO_SMALL
        if (frame.faceRatio > MAX_FACE_RATIO) return CalibrationHint.FACE_TOO_LARGE
        if (frame.brightness < MIN_BRIGHTNESS) return CalibrationHint.LOW_LIGHT
        return if (ready) CalibrationHint.READY else CalibrationHint.BUILDING
    }

    /**
     * 每秒一跳的计时/就绪推进。
     *
     * - 秒数 = **人脸在镜累计**（[nextCalibrationTick]），离镜 debounce 后归零并
     *   `bridge.reset()` 清 SDK 窗口；
     * - 就绪 = `bridge.hasEnoughWindow` 地面真相，不再用"墙钟 >= 30s"推断，
     *   所以按钮可用性与"真的能开始采集"一致。
     */
    private fun startTicker() {
        viewModelScope.launch {
            while (isActive) {
                val state = _state.value
                if (state.initOk) {
                    val tick = nextCalibrationTick(
                        faceFound = state.faceFound,
                        prevSec = facePresentSec,
                        absentTicks = absentTicks,
                        windowFull = bridge.hasEnoughWindow,
                    )
                    facePresentSec = tick.sec
                    absentTicks = tick.absentTicks
                    if (tick.shouldReset) {
                        // 离镜：清掉 SDK 内部窗口，重入镜从 0 真正重新开始攒。
                        bridge.reset()
                    }
                    val read = bridge.read()
                    // 刷新诊断：轮询协程更新了 licenseDaysLeftCache，但 bridge.lastDiagnostics
                    // 只在 init 时设过一次。这里每秒回读，让 License 剩余从「—」变成真实天数。
                    val diag = bridge.diagnostics()
                    _state.value = state.copy(
                        signalSec = tick.sec,
                        signalProgress = (tick.sec.toFloat() / HR_READY_SEC).coerceIn(0f, 1f),
                        hint = decideHint(hintFrameOf(state), tick.ready),
                        readReady = tick.ready,
                        sqi = (read as? MetricsRead.Ok)?.metrics?.sqi,
                        licenseDaysLeft = diag.licenseDaysLeft ?: state.licenseDaysLeft,
                    )
                }
                delay(1_000)
            }
        }
    }

    /**
     * ticker 里没有新帧，用上一次的帧结论重算 hint。
     * 亮度已经在 [onFrame] 那帧判过，这里只在"上一态是 LOW_LIGHT"时喂 0 保持暗态，
     * 否则喂 255 表示"亮度不是当前的否决项"。
     */
    private fun hintFrameOf(state: CalibrateUiState): FaceFrame = FaceFrame(
        faceFound = state.faceFound,
        faceRatio = state.faceRatio,
        brightness = if (state.hint == CalibrationHint.LOW_LIGHT) 0 else 255,
    )

    /** 用户跳过：撤销相机同意，训练走纯语音。 */
    fun skipCamera() {
        viewModelScope.launch {
            container.settingsStore.setCameraConsent(false, System.currentTimeMillis())
        }
    }

    override fun onCleared() {
        super.onCleared()
        bridge.release()
        analyzer.release()
    }

    companion object {
        /** 需求 §5.2 FR-03：人脸占画面 20%–60% 为合适 */
        const val MIN_FACE_RATIO = 0.20f
        const val MAX_FACE_RATIO = 0.60f
        /** 亮度门限（0–255），低于此判"光线有点暗" */
        const val MIN_BRIGHTNESS = 60
        /** 心率可用窗口（§5.4）*/
        const val HR_READY_SEC = 30
        /** 真实 SDK 主模型资产名（RealRppgEngine 内部用同一常量）*/
        const val REAL_ENC_ASSET = "step_mini.tflite.enc"
    }
}