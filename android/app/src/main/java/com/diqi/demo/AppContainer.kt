package com.diqi.demo

import android.app.Application
import com.diqi.demo.core.time.Clock
import com.diqi.demo.core.time.MonotonicClock

/**
 * 手写依赖容器（决策 D7：不引入 Hilt）。
 *
 * 生命周期准则（这些对象有明确的归属，别混用）：
 *
 * **application-scoped（与进程同生共死，绝不泄漏 Activity）**
 *  · [clock] 时钟
 *  · 后续：Room 数据库、DataStore、SessionRepository、SensorCoordinator、VoiceGuide
 *    —— SensorCoordinator 必须是 app-scoped，否则锁屏时 Activity 销毁会连带
 *    释放 SDK 窗口，AC-05「锁屏 30s 返回指标恢复」直接失败（决策 D12）
 *
 * **screen-scoped（必须随页面销毁）**
 *  · CameraX 的 CameraProvider 绑定、ImageAnalysis 用例、ML Kit detector
 *    —— 页面销毁必须 unbind + close，否则 AC-18「连续进出 5 次」会泄漏
 *
 * **process-scoped but restartable**
 *  · TrainingEngine 本身可随 ViewModel 重建，状态从 DataStore 快照恢复
 */
class AppContainer(private val app: Application) {

    val clock: Clock = MonotonicClock

    /** 应用上下文，供后续组件注入使用（不持有 Activity）。 */
    val context: Application get() = app
}