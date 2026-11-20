package com.diqi.demo.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.diqi.demo.AppContainer
import com.diqi.demo.HerRhythmApp
import com.diqi.demo.data.db.MissingReason
import com.diqi.demo.data.db.QualityState
import com.diqi.demo.data.db.SensorMode
import com.diqi.demo.data.repo.QualitySummary
import com.diqi.demo.data.repo.SessionDraft
import com.diqi.demo.domain.training.TrainingParams
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * **仅 debug 构建存在**的造数入口。
 *
 * 为什么必须有它：设备 `ro.build.type=user`、无 root，`adb shell date` 改不了系统时间，
 * 于是 AC-12（3 天记录）与 AC-20（Day1→Day2）**没有合法的造数路径** ——
 * 总不能真的等三天。这里用广播造历史记录。
 *
 * 为什么放在 `src/debug` 而不是 `src/main` + `BuildConfig` 判断：
 * 只靠运行时判断的话，release APK 里仍存在这个 exported 接收器，
 * 任何人都能广播造数。放在 debug source set，release 包**根本没有这个类**。
 *
 * 用法：
 *   adb shell am broadcast -a com.diqi.demo.DEBUG_SEED -e days 3
 *   adb shell am broadcast -a com.diqi.demo.DEBUG_SEED -e action clear
 *   adb shell am broadcast -a com.diqi.demo.DEBUG_SEED -e action probe
 *
 * ## 两个必须做对的细节（首版都踩了）
 *
 * 1. **必须 `goAsync()`**：直接 `scope.launch` 然后从 `onReceive` 返回，
 *    系统认为广播已处理完，可能在写库完成前回收进程 —— 表现为
 *    "broadcast completed: result=0 但库里没有数据"。
 * 2. **必须挂 `CoroutineExceptionHandler`**：否则写库异常被协程静默吞掉，
 *    排查时会误以为是广播没送达。
 */
class DebugSeedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra(EXTRA_ACTION) ?: ACTION_SEED
        val days = intent.getStringExtra("days")?.toIntOrNull() ?: 3
        Log.i(TAG, "onReceive action=$action days=$days")

        val pending = goAsync()
        val handler = CoroutineExceptionHandler { _, e ->
            Log.e(TAG, "造数失败", e)
            runCatching { pending.finish() }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + handler)

        scope.launch {
            try {
                val container = HerRhythmApp.container()
                when (action) {
                    ACTION_CLEAR -> {
                        container.sessionRepository.clearAll()
                        container.leakDiaryRepository.clearAll()
                        container.settingsStore.clearEverythingForTests()
                        Log.i(TAG, "已清空全部数据")
                    }
                    ACTION_DEMO -> {
                        val on = intent.getBooleanExtra("on", true)
                        container.settingsStore.setDemoMode(on)
                        Log.i(TAG, "演示模式 = $on")
                    }
                    ACTION_PROBE -> {
                        Log.i(TAG, "probe: sessions=${container.sessionRepository.recentReal(100).size}")
                    }
                    else -> {
                        seed(container, days)
                        val written = container.sessionRepository.recentReal(100).size
                        Log.i(TAG, "已造 ${days} 天记录，当前库内真实记录 $written 条")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "造数失败", e)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun seed(container: AppContainer, days: Int) {
        val zone = ZoneId.systemDefault()
        val params = TrainingParams.DEFAULT
        val today = LocalDate.now(zone)
        val repo = container.sessionRepository

        // 按 id 前缀清理旧种子。**不能**用 clearSimulated()：
        // 种子是真实模式（SensorMode.OFF），deleteSimulated 清不掉它，
        // 于是跨天重复运行会不断累积，且那些残留行会作为"真实训练"
        // 计入打卡/周报/Day 进度（评审 D3）。
        repo.clearSeeds()

        val nowMs = System.currentTimeMillis()
        val n = days.coerceIn(1, 30)
        // 从最早一天造到今天，使 firstRealStartAt = today-(n-1) →
        // 今天自然落在 Day n（这样 AC-20 的 Day2/Day3 都能验）
        for (i in n - 1 downTo 0) {
            val day: LocalDate = today.minusDays(i.toLong())
            // 原始设计是固定 20:00。但若当前时间早于 20:00，今天的种子会落在
            // **未来**，导致 latestReal()（ORDER BY startedAt DESC）取到未来记录，
            // 破坏"上次训练"与记录页顺序（评审 D3 第二点）。
            // 故把今天的种子压到"现在前 1 分钟"。
            val planned = day.atTime(20, 0).atZone(zone).toInstant().toEpochMilli()
            val startMs = minOf(planned, nowMs - 60_000L)
            repo.save(
                SessionDraft(
                    id = "seed-day-$day",
                    startedAt = startMs,
                    endedAt = startMs + params.totalActiveMs,
                    durationMs = params.totalActiveMs,
                    planVersion = params.hash(),
                    plannedReps = params.repsPerSet,
                    completedReps = params.repsPerSet,
                    pauseCount = 0,
                    pausedMs = 0,
                    mode = SensorMode.OFF,
                    signalCoverage = null,
                    // 未启用感知 ⇒ 整场都是 NO_DATA，故 invalid=1.0。
                    // 首版写 0/0/0 配 NO_DATA，消费方无法区分
                    // "数据缺失" 与 "真的是 0"（评审 E3）。
                    quality = QualitySummary(
                        lastState = QualityState.NO_DATA,
                        validRatio = 0.0,
                        degradedRatio = 0.0,
                        invalidRatio = 1.0,
                        missingReason = MissingReason.NOT_ENABLED,
                    ),
                ),
            )
        }
    }

    companion object {
        private const val TAG = "DebugSeed"
        const val EXTRA_ACTION = "action"
        const val ACTION_SEED = "seed"
        const val ACTION_CLEAR = "clear"
        const val ACTION_PROBE = "probe"
        const val ACTION_DEMO = "demo"
    }
}