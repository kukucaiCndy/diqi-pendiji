package com.diqi.demo.domain.training

import java.security.MessageDigest
import kotlin.math.roundToLong

/**
 * 训练参数（需求 §5.4 全表）。
 *
 * ## 为什么把参数集中成数据类并带 [hash]
 *
 * 需求 §12.1 明确「参数默认值需临床负责人确认」。临床一旦调整收缩/保持时长，
 * 历史记录的可比性就断了 —— 除非每条记录都能反查它当时用的是哪套参数。
 * 所以每次训练都写入 `planVersion = params.hash()`，调参不破坏可追溯性。
 *
 * 区间校验（[validate]）在启动时跑一次：参数写错时应该在开发期就炸，
 * 而不是让用户练了一组才发现"放松只有 1 秒"。
 */
data class TrainingParams(
    /** 准备倒计时 */
    val prepSec: Int = 5,
    /** 收缩时长 */
    val contractSec: Int = 5,
    /** 保持时长（必须自然呼吸） */
    val holdSec: Int = 5,
    /** 放松时长 */
    val relaxSec: Int = 5,
    /** 每组次数 */
    val repsPerSet: Int = 8,
    /** 组数（本期单组） */
    val sets: Int = 1,
    /** 语音速度倍率 */
    val voiceSpeed: Float = 1.0f,
    /** 相机预览高度 */
    val previewHeight: Int = 720,
    /** 分析帧率 */
    val analysisFps: Int = 12,
    /** 指标刷新间隔（毫秒） */
    val metricsIntervalMs: Long = 1_000L,
    /** 心率可用所需时长（秒）—— SDK 特性 */
    val hrMinSec: Int = 30,
    /** 呼吸率可用所需时长（秒）—— SDK 特性 */
    val brMinSec: Int = 60,
    /** SQI 显示门限，低于此判 INVALID（待真机标定） */
    val sqiThreshold: Float = 0.50f,
) {

    // ── 阶段时长（毫秒）────────────────────────────────────────────────
    val prepMs: Long get() = prepSec * 1_000L
    val contractMs: Long get() = contractSec * 1_000L
    val holdMs: Long get() = holdSec * 1_000L
    val relaxMs: Long get() = relaxSec * 1_000L

    /** 一次完整收缩循环的时长 */
    val cycleMs: Long get() = contractMs + holdMs + relaxMs

    /** 不含准备阶段的引导总时长 */
    val totalActiveMs: Long get() = prepMs + cycleMs * repsPerSet

    /**
     * 参数版本号：sha1(规范化后的参数) 前 8 位。
     *
     * 用内容哈希而不是手写版本号 —— 手写的会忘记加，内容哈希不可能忘。
     */
    fun hash(): String {
        // 必须固定 Locale.ROOT：`"%.2f".format()` 走默认 Locale，
        // 德语环境下 1.0f 会变成 "1,00"、阿拉伯语环境会变成阿拉伯数字，
        // 同一套参数在换系统语言的设备上会产出不同 planVersion ——
        // 而 planVersion 存在的意义正是"历史可比"（评审 E1）。
        fun f(v: Float) = String.format(java.util.Locale.ROOT, "%.2f", v)
        val canonical = listOf(
            prepSec, contractSec, holdSec, relaxSec, repsPerSet, sets,
            f(voiceSpeed), previewHeight, analysisFps, metricsIntervalMs,
            hrMinSec, brMinSec, f(sqiThreshold),
        ).joinToString("|")
        val digest = MessageDigest.getInstance("SHA-1").digest(canonical.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(8)
    }

    /** 返回所有越界项的可读描述；空列表表示合法。 */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        fun check(name: String, value: Int, range: IntRange) {
            if (value !in range) errors += "$name=$value 超出范围 $range"
        }
        check("准备倒计时", prepSec, 3..10)
        check("收缩时长", contractSec, 3..10)
        check("保持时长", holdSec, 3..10)
        check("放松时长", relaxSec, 5..15)
        check("每组次数", repsPerSet, 3..12)
        check("组数", sets, 1..1)
        check("分析帧率", analysisFps, 8..15)
        check("心率窗口", hrMinSec, 1..600)
        check("呼吸窗口", brMinSec, 1..600)
        if (voiceSpeed < 0.8f || voiceSpeed > 1.2f) {
            errors += "语音速度=$voiceSpeed 超出范围 0.8–1.2"
        }
        if (sqiThreshold <= 0f || sqiThreshold >= 1f) {
            errors += "SQI 门限=$sqiThreshold 必须在 (0,1) 之间"
        }
        if (metricsIntervalMs <= 0L) {
            errors += "指标刷新间隔=$metricsIntervalMs 必须 > 0"
        }
        return errors
    }

    /** 演示模式参数：次数 3（需求 §5.4「演示可用 3」）*/
    fun forDemo(): TrainingParams = copy(repsPerSet = 3)

    companion object {
        /** 需求 §5.4 默认值 */
        val DEFAULT = TrainingParams()

        /**
         * 单帧目标时长（秒），供 ROI 投喂使用。
         * 12fps → 0.0833s；必须是纯计算，不要各页各算一遍。
         */
        fun frameDtSec(fps: Int): Float = 1f / fps.coerceAtLeast(1)
    }
}

/** 单个阶段的时长，用于时间轴与进度环渲染。 */
enum class TrainingPhase(val displayName: String, val voiceKey: String) {
    IDLE("等待开始", ""),
    PREPARATION("准备", "prep"),
    CONTRACT("收缩", "contract"),
    HOLD("保持呼吸", "hold"),
    RELAX("放松", "relax"),
    COMPLETE("完成", "complete");

    /** 阶段是否有倒计时（Repeat/Idle/Complete 没有） */
    val hasCountdown: Boolean
        get() = this == PREPARATION || this == CONTRACT || this == HOLD || this == RELAX

    fun durationMs(params: TrainingParams): Long = when (this) {
        PREPARATION -> params.prepMs
        CONTRACT -> params.contractMs
        HOLD -> params.holdMs
        RELAX -> params.relaxMs
        IDLE, COMPLETE -> 0L
    }

    /** 该阶段的固定中文提示（需求 §5.5）*/
    val hint: String
        get() = when (this) {
            IDLE -> "坐稳，放松肩膀和腹部"
            PREPARATION -> "坐稳，放松肩膀和腹部"
            CONTRACT -> "轻轻提起盆底"
            HOLD -> "保持住，继续自然呼吸"
            RELAX -> "慢慢放松"
            COMPLETE -> "今天的练习完成了，做得很好"
        }
}

/** 训练运行状态（需求 §5.3）。 */
enum class TrainingStatus { IDLE, RUNNING, PAUSED, COMPLETE }

/**
 * 给 UI 的一帧快照。
 *
 * 由 `TrainingEngine.frameAt()` 纯函数计算得出，不经过任何 tick 累加（D11）。
 */
data class TrainingFrame(
    val status: TrainingStatus,
    val phase: TrainingPhase,
    /** 当前阶段剩余毫秒 */
    val phaseRemainingMs: Long,
    /** 当前阶段总毫秒 */
    val phaseTotalMs: Long,
    /** 当前是第几次（1-based） */
    val repIndex: Int,
    val totalReps: Int,
    /** 整场训练的进度 0–1（含准备阶段） */
    val overallProgress: Float,
    /** 已完成次数（不补造，AC-07） */
    val completedReps: Int,
) {
    /** 倒计时显示为「0:05」形式 */
    val countdownText: String
        get() {
            val totalSec = ((phaseRemainingMs + 999) / 1000).toInt().coerceAtLeast(0)
            return "%02d:%02d".format(totalSec / 60, totalSec % 60)
        }

    /** 进度环比例 0–1 */
    val phaseProgress: Float
        get() = if (phaseTotalMs <= 0L) 0f else {
            ((phaseTotalMs - phaseRemainingMs).toFloat() / phaseTotalMs).coerceIn(0f, 1f)
        }

    companion object {
        fun idle(totalReps: Int) = TrainingFrame(
            status = TrainingStatus.IDLE,
            phase = TrainingPhase.IDLE,
            phaseRemainingMs = 0,
            phaseTotalMs = 0,
            repIndex = 1,
            totalReps = totalReps,
            overallProgress = 0f,
            completedReps = 0,
        )
    }
}

/**
 * 中断恢复快照（AC-13）。
 *
 * ## 为什么只存相对量
 *
 * [elapsedActiveMs] 是"快照时已用的活动时长"，而不是单调时钟的绝对起点。
 * 进程被杀后 `SystemClock.elapsedRealtime()` 会归零，若存绝对值，
 * 恢复后算出的已用时长会是负数（评审指出的真实坑）。
 * 恢复时用 `now - elapsedActiveMs` 倒推起点即可。
 */
data class EngineSnapshot(
    /** 训练开始的墙钟时间，仅用于写库的 startedAt */
    val startedAtWallMs: Long,
    val accumulatedPausedMs: Long,
    val pauseCount: Int,
    val planVersion: String,
    val plannedReps: Int,
    /** 快照时的已用活动时长（不含暂停）*/
    val elapsedActiveMs: Long,
    /** 快照时是否处于暂停状态 */
    val wasPaused: Boolean,
) {
    /** 是否值得提示"继续/丢弃"：至少开始了 3 秒且没跑完 */
    fun isResumable(params: TrainingParams): Boolean =
        elapsedActiveMs >= MIN_RESUMABLE_MS &&
            elapsedActiveMs < params.totalActiveMs - 1_000L

    companion object {
        const val MIN_RESUMABLE_MS = 3_000L
    }
}

/** 秒数向上取整的小工具，避免 UI 各自 ceil。 */
internal fun Long.toCeilSeconds(): Long = (this + 999) / 1000

internal fun Float.toMillisRounded(): Long = (this * 1000f).roundToLong()