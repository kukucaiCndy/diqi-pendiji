package com.diqi.demo.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一次训练记录（需求 §6 TrainingSession）。
 *
 * ## 字段写入者（必须明确，否则 FR-09 的 60% 门限无处落地）
 * - [signalCoverage] ← `QualityGate` 在 Complete 时汇总
 * - [avgHr] / [avgBreathingRate] / [avgSqi] ← **仅当 gate 通过才写**，否则 NULL，
 *   由 `SessionRepository.buildEntity` 强制
 * - [sdkVersion] ← `FacePhysBridge.diagnostics().sdkVersion`（与 P03 诊断区同源）
 * - [planVersion] ← `TrainingParams.hash()`，使临床调参可追溯
 *
 * ## 墙钟 vs 单调时钟（评审 D2）
 *
 * [startedAt] / [endedAt] 是**墙钟**（epoch millis），用于日历归档与"哪一天练的"。
 * 但**统计时长必须读 [durationMs]**，不能拿 `endedAt - startedAt` 推导 ——
 * 训练中 NTP 校时或用户改时间会让墙钟差跳变（甚至为负）。
 * [durationMs] 由引擎用单调时钟（`elapsedActive - pausedMs`）写入，不受校时影响。
 *
 * `require` 级别的防御写在 `SessionRepository.buildEntity`：startedAt 必须是
 * 合理量级的 epoch 毫秒，否则说明有人把 `elapsedRealtime` 直接塞进来了。
 */
@Entity(
    tableName = "training_session",
    indices = [
        Index("startedAt"),
        Index("sensorMode"),
    ],
)
data class TrainingSessionEntity(
    @PrimaryKey val id: String,
    /** 墙钟 epoch millis */
    val startedAt: Long,
    /** 墙钟 epoch millis；未正常结束时为 null */
    val endedAt: Long?,
    /**
     * 单调时钟测得的活动时长（毫秒，已扣除暂停）。
     * **统计只认这个字段**，不要用 endedAt - startedAt（评审 D2）。
     */
    val durationMs: Long,
    /** 参数版本（TrainingParams.hash） */
    val planVersion: String,
    val plannedReps: Int,
    val completedReps: Int,
    val pauseCount: Int,
    val pausedMs: Long,
    /** 1=轻松 2=合适 3=困难；未自评为 null */
    val difficulty: Int?,
    /** 0=无不适 1=有不适；未自评为 null */
    val discomfort: Int?,
    /** [SensorMode.storageValue] */
    val sensorMode: String,
    /** 0–1，仅真实感知有值 */
    val signalCoverage: Double?,
    val avgHr: Double?,
    val avgBreathingRate: Double?,
    val avgSqi: Double?,
    val sdkVersion: String?,
    val createdAt: Long,
) {
    val durationSec: Long get() = durationMs / 1000

    val mode: SensorMode get() = SensorMode.from(sensorMode)

    /** 是否参与统计（时长门槛由查询层保证，这里只判模式）*/
    val countsTowardProgress: Boolean get() = !mode.isSimulated
}

/**
 * 每次训练一条的质量汇总（需求 §6 SensorQuality）。
 *
 * ## `validRatio` 等三个比值的语义
 *
 * 需求 §6 原文是「各状态**时长**占比」，故这里是时长占比，不是采样数占比。
 * 三者之和应约等于 1（允许浮点误差），`SessionRepository` 写入时校验。
 *
 * ## 为什么没有 `sensorMode` 列（评审 B1 的裁定）
 *
 * 评审给了两个选项：加冗余 `sensorMode` 列，或走 JOIN `training_session`。
 * 选 **JOIN**：冗余列会引入"两处 sensorMode 可能不一致"这个新的失效面，
 * 而质量表永远依附于会话、不存在"脱离会话的独立聚合"需求。
 * 因此**任何跨会话的质量统计必须 JOIN `training_session` 并过滤 simulated**，
 * 见 `SensorQualityDao.aggregateReal()`。
 */
@Entity(
    tableName = "sensor_quality",
    foreignKeys = [
        ForeignKey(
            entity = TrainingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class SensorQualityEntity(
    @PrimaryKey val sessionId: String,
    /** [QualityState.storageValue] */
    val lastState: String,
    /** 时长占比 0–1 */
    val validRatio: Double,
    val degradedRatio: Double,
    val invalidRatio: Double,
    /** [MissingReason.storageValue] */
    val missingReason: String,
    /** 平均端到端延迟（如可得） */
    val latencyMs: Long?,
) {
    val ratiosSum: Double get() = validRatio + degradedRatio + invalidRatio
}

/** 漏尿频次（FR-12，取值必须收敛，AC-23 要求不写脏数据）。 */
enum class LeakFrequency(val storageValue: String, val label: String) {
    NONE("none", "没有"),
    ONCE("once", "1 次"),
    TWO_TO_THREE("2_3", "2–3 次"),
    FOUR_PLUS("4_plus", "4 次及以上"),
    UNSURE("unsure", "不记得"),
    ;

    companion object {
        fun from(value: String?): LeakFrequency? =
            entries.firstOrNull { it.storageValue == value }
    }
}

/** 漏尿情境（FR-12，可空）。 */
enum class LeakContext(val storageValue: String, val label: String) {
    COUGH("cough", "咳嗽喷嚏"),
    SPORT("sport", "运动提重物"),
    URGE("urge", "突然尿急"),
    OTHER("other", "其他"),
    UNSURE("unsure", "不确定"),
    ;

    companion object {
        fun from(value: String?): LeakContext? =
            entries.firstOrNull { it.storageValue == value }
    }
}

/**
 * 漏尿简易日记（需求 §6 LeakDiaryEntry / FR-12）。
 *
 * ## 三态语义（AC-23 的判据基础）
 *
 * - **没有行** = 用户今天没接触过这个功能
 * - **[skipped] = true** = 用户点了「跳过」（合法记录，不是脏数据）
 * - **[skipped] = false** = 用户作答了，此时 [frequency] 必然非空
 *
 * 因此 [frequency] 是 **nullable**：跳过时不允许编造一个频次
 * （首版强制非空，等于逼调用方造假，评审 E2 指出）。
 */
@Entity(
    tableName = "leak_diary",
    indices = [Index(value = ["date"], unique = true)],
)
data class LeakDiaryEntity(
    @PrimaryKey val id: String,
    /** epoch day */
    val date: Long,
    /** 作答时非空；skipped=true 时为 null */
    val frequency: String?,
    /** [LeakContext.storageValue]，可空 */
    val context: String?,
    val skipped: Boolean,
    val createdAt: Long,
) {
    init {
        // 不变式：跳过 → 不得有频次；作答 → 必须有频次
        require(skipped == (frequency == null)) {
            "leak_diary 不变式被破坏：skipped=$skipped 但 frequency=$frequency"
        }
    }

    val leakFrequency: LeakFrequency? get() = LeakFrequency.from(frequency)
    val leakContext: LeakContext? get() = LeakContext.from(context)
}