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
    /** 已建立信号的秒数 */
    val signalSec: Int = 0,
    /** 前 30 秒的建立进度 0–1 */
    val signalProgress: Float = 0f,
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

    private var analyserStartedAtMs = 0L

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
                        analyserStartedAtMs = android.os.SystemClock.elapsedRealtime()
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
            hint = decideHint(frame, box.signalSec),
        )
    }

    /**
     * 四类反馈的判定（FR-03）。
     *
     * 顺序很重要：先判"有没有脸"，再判"大小"，再判"光照"，
     * 最后才是"建立中/就绪"。否则用户在框内但脸太小的时候
     * 会看到"信号建立中"，完全不知道要靠近。
     */
    private fun decideHint(frame: FaceFrame, signalSec: Int): CalibrationHint {
        if (!frame.faceFound) return CalibrationHint.NO_FACE
        if (frame.faceRatio < MIN_FACE_RATIO) return CalibrationHint.FACE_TOO_SMALL
        if (frame.faceRatio > MAX_FACE_RATIO) return CalibrationHint.FACE_TOO_LARGE
        if (frame.brightness < MIN_BRIGHTNESS) return CalibrationHint.LOW_LIGHT
        return if (signalSec >= HR_READY_SEC) CalibrationHint.READY else CalibrationHint.BUILDING
    }

    private fun startTicker() {
        viewModelScope.launch {
            while (isActive) {
                val state = _state.value
                if (state.initOk && state.faceFound) {
                    val elapsed =
                        ((android.os.SystemClock.elapsedRealtime() - analyserStartedAtMs) / 1000).toInt()
                    val sec = elapsed.coerceAtLeast(0)
                    val read = bridge.read()
                    // 刷新诊断：轮询协程更新了 licenseDaysLeftCache，但 bridge.lastDiagnostics
                    // 只在 init 时设过一次。这里每秒回读，让 License 剩余从「—」变成真实天数。
                    val diag = bridge.diagnostics()
                    _state.value = _state.value.copy(
                        signalSec = sec,
                        signalProgress = (sec.toFloat() / HR_READY_SEC).coerceIn(0f, 1f),
                        hint = decideHint(
                            FaceFrame(
                                faceFound = _state.value.faceFound,
                                faceRatio = _state.value.faceRatio,
                                brightness = if (_state.value.hint == CalibrationHint.LOW_LIGHT) 0 else 255,
                            ),
                            sec,
                        ),
                        readReady = sec >= HR_READY_SEC &&
                            (read is MetricsRead.NoSignal || read is MetricsRead.Ok),
                        sqi = (read as? MetricsRead.Ok)?.metrics?.sqi,
                        licenseDaysLeft = diag.licenseDaysLeft ?: state.licenseDaysLeft,
                    )
                }
                delay(1_000)
            }
        }
    }

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