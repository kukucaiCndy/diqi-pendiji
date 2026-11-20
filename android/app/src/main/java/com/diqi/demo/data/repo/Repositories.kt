package com.diqi.demo.data.repo

import androidx.room.withTransaction
import com.diqi.demo.data.db.HerRhythmDb
import com.diqi.demo.data.db.LeakContext
import com.diqi.demo.data.db.LeakDiaryEntity
import com.diqi.demo.data.db.LeakFrequency
import com.diqi.demo.data.db.MissingReason
import com.diqi.demo.data.db.QualityState
import com.diqi.demo.data.db.SensorMode
import com.diqi.demo.data.db.SensorQualityEntity
import com.diqi.demo.data.db.TrainingSessionEntity
import com.diqi.demo.domain.plan.SevenDayPlan
import java.time.Instant
import java.time.ZoneId

/** 一次训练结束后要写库的全部内容。 */
data class SessionDraft(
    /**
     * 会话 id。**必须由调用方在训练开始时生成并持有**（故意无默认值）。
     *
     * 首版给了 `UUID.randomUUID()` 默认值，于是 AC-08「连点两次完成只产生一条」
     * 只在"调用方复用同一个 draft"时才成立 —— 每次点击新建 draft 就是两条
     * （评审 D4）。去掉默认值后，编译期就逼调用方持有 id。
     */
    val id: String,
    val startedAt: Long,
    val endedAt: Long,
    /**
     * 单调时钟测得的活动时长（已扣暂停）。**统计只认这个值。**
     * 不要传 `endedAt - startedAt` —— 训练中校时会污染墙钟差（评审 D2）。
     */
    val durationMs: Long,
    val planVersion: String,
    val plannedReps: Int,
    val completedReps: Int,
    val pauseCount: Int,
    val pausedMs: Long,
    val mode: SensorMode,
    /** VALID 时长 / 总时长；未启用感知为 null */
    val signalCoverage: Double? = null,
    val avgHr: Double? = null,
    val avgBreathingRate: Double? = null,
    val avgSqi: Double? = null,
    val sdkVersion: String? = null,
    val quality: QualitySummary? = null,
)

/** 质量汇总，三个 ratio 为**时长占比**（需求 §6 原文）。 */
data class QualitySummary(
    val lastState: QualityState,
    val validRatio: Double,
    val degradedRatio: Double,
    val invalidRatio: Double,
    val missingReason: MissingReason,
    val latencyMs: Long? = null,
)

/**
 * 训练记录的唯一写入入口。
 *
 * ## 三条必须在这里守住的规则
 *
 * 1. **FR-09 覆盖率门限**：VALID 覆盖率 < 60% 时 avgHr / avgBreathingRate /
 *    avgSqi 一律置空。只在 UI 层"不显示"不够 —— 周报、导出、统计都会读这些字段。
 * 2. **只有真实感知才允许带指标**：`mode != OFFLINE_SDK` 时无条件置空，
 *    否则演示/未启用会话只要 coverage 传得巧就能把数字写进去（评审 B3）。
 * 3. **事务**：一次保存写两张表，必须原子；自评回填是读-改-写，也必须原子
 *    （评审 A2）。
 */
class SessionRepository(private val db: HerRhythmDb) {

    private val sessionDao = db.trainingSessionDao()
    private val qualityDao = db.sensorQualityDao()

    suspend fun save(draft: SessionDraft): TrainingSessionEntity {
        val entity = buildEntity(draft)
        db.withTransaction {
            sessionDao.upsert(entity)
            draft.quality?.let { q -> qualityDao.upsert(q.toEntity(entity.id)) }
        }
        return entity
    }

    /**
     * 回填自评（P05）。自评发生在保存之后，且可以跳过，故单独一个方法。
     *
     * 用 `@Upsert` + 事务：`REPLACE` 会触发质量表级联删除（评审 A1）。
     */
    suspend fun attachSelfAssessment(sessionId: String, difficulty: Int?, discomfort: Boolean?) {
        db.withTransaction {
            val existing = sessionDao.findById(sessionId) ?: return@withTransaction
            sessionDao.upsert(
                existing.copy(
                    difficulty = difficulty?.coerceIn(1, 3),
                    discomfort = discomfort?.let { if (it) 1 else 0 },
                ),
            )
        }
    }

    fun finalize(draft: SessionDraft): TrainingSessionEntity = buildEntity(draft)

    suspend fun findById(id: String) = sessionDao.findById(id)
    suspend fun latestReal() = sessionDao.latestReal()
    suspend fun firstRealStartAt() = sessionDao.firstRealStartAt()
    suspend fun recentReal(limit: Int = 20) = sessionDao.recentReal(limit)
    suspend fun simulatedSessions(limit: Int = 20) = sessionDao.simulatedSessions(limit)

    suspend fun clearAll() {
        db.withTransaction {
            qualityDao.deleteAll()
            sessionDao.deleteAll()
        }
    }

    suspend fun clearSimulated() = sessionDao.deleteSimulated()

    /** 清 debug 种子（按 id 前缀）。种子是真实模式，deleteSimulated 清不掉。 */
    suspend fun clearSeeds() {
        db.withTransaction {
            qualityDao.deleteAll()
            sessionDao.deleteSeeds()
        }
    }

    companion object {
        /** FR-09：VALID 样本覆盖率 ≥ 60% 才允许展示指标 */
        const val MIN_COVERAGE_FOR_METRICS = 0.6

        /** 合理 epoch 毫秒下限（2001-09-09）。低于它必是把单调时钟塞错了地方。 */
        const val MIN_EPOCH_MS = 1_000_000_000_000L

        /**
         * 把草稿规范化为可入库实体。
         *
         * 这里是**合规强制**，不是数据清洗。
         */
        fun buildEntity(
            draft: SessionDraft,
            nowMs: Long = System.currentTimeMillis(),
        ): TrainingSessionEntity {
            require(draft.startedAt >= MIN_EPOCH_MS) {
                "startedAt 必须是墙钟 epoch 毫秒（收到 ${draft.startedAt}）—— " +
                    "引擎的 elapsedRealtime 不能直接入库"
            }
            require(draft.durationMs >= 0) { "durationMs 不能为负：${draft.durationMs}" }

            val coverage = draft.signalCoverage
                ?.takeIf { it.isFinite() }
                ?.coerceIn(0.0, 1.0)

            // 只有真实感知且覆盖率达标，指标才允许存在
            val trustworthy = draft.mode == SensorMode.OFFLINE_SDK &&
                coverage != null &&
                coverage >= MIN_COVERAGE_FOR_METRICS

            return TrainingSessionEntity(
                id = draft.id,
                startedAt = draft.startedAt,
                endedAt = draft.endedAt,
                durationMs = draft.durationMs,
                planVersion = draft.planVersion,
                plannedReps = draft.plannedReps,
                completedReps = draft.completedReps,
                pauseCount = draft.pauseCount,
                pausedMs = draft.pausedMs,
                difficulty = null,
                discomfort = null,
                sensorMode = draft.mode.storageValue,
                signalCoverage = coverage,
                avgHr = if (trustworthy) draft.avgHr else null,
                avgBreathingRate = if (trustworthy) draft.avgBreathingRate else null,
                avgSqi = if (trustworthy) draft.avgSqi else null,
                sdkVersion = draft.sdkVersion,
                createdAt = nowMs,
            )
        }
    }
}

private fun QualitySummary.toEntity(sessionId: String): SensorQualityEntity {
    // 时长占比之和必须 ≈1；写错比例会让"质量分布"变成假数据。
    val sum = validRatio + degradedRatio + invalidRatio
    require(kotlin.math.abs(sum - 1.0) <= 0.01) {
        "质量占比之和应为 1，实际 $sum（valid=$validRatio degraded=$degradedRatio invalid=$invalidRatio）"
    }
    return SensorQualityEntity(
        sessionId = sessionId,
        lastState = lastState.storageValue,
        validRatio = validRatio,
        degradedRatio = degradedRatio,
        invalidRatio = invalidRatio,
        missingReason = missingReason.storageValue,
        latencyMs = latencyMs,
    )
}

/** 近 7 天的进度汇总（P02 打卡区 / P06 统计区共用）。 */
data class WeekSummary(
    /** 有有效训练的 epochDay 集合 */
    val trainedDays: Set<Long>,
    val sessionCount: Int,
    val totalSec: Long,
    val lastTrainingAt: Long?,
)

class ProgressRepository(
    private val db: HerRhythmDb,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    private val sessionDao = db.trainingSessionDao()

    /** 以「今天」为终点、往前 6 天的窗口。 */
    suspend fun lastSevenDays(nowMs: Long): WeekSummary {
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        val fromDay = today.minusDays(6)
        // 用本地日历日边界换算，而不是 nowMs - 7*86400000 ——
        // 后者在跨夏令时/时区变更时会差一小时。
        val fromMs = fromDay.atStartOfDay(zone).toInstant().toEpochMilli()
        val toMs = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val sessions = sessionDao.realBetween(fromMs, toMs)
        val trainedDays = sessions.map {
            Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate().toEpochDay()
        }.toSet()

        return WeekSummary(
            trainedDays = trainedDays,
            sessionCount = sessions.size,
            totalSec = sessions.sumOf { it.durationSec },
            lastTrainingAt = sessionDao.latestReal()?.startedAt,
        )
    }

    suspend fun hasTrainedToday(nowMs: Long): Boolean {
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        val fromMs = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val toMs = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return sessionDao.countRealBetween(fromMs, toMs) > 0
    }

    /** 当前处于七天计划的第几天（按首次完成日起算，FR-11）*/
    suspend fun currentDayIndex(nowMs: Long): Int =
        SevenDayPlan.dayIndexFor(sessionDao.firstRealStartAt(), nowMs, zone)

    suspend fun isCycleComplete(nowMs: Long): Boolean =
        SevenDayPlan.isCycleComplete(sessionDao.firstRealStartAt(), nowMs, zone)

    /** 跨会话质量均值（JOIN 过滤 simulated）*/
    suspend fun averageValidRatio(nowMs: Long): Double? {
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        val fromMs = today.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()
        val toMs = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return db.sensorQualityDao().averageValidRatioRealBetween(fromMs, toMs)
    }
}

class LeakDiaryRepository(
    private val db: HerRhythmDb,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    private val dao = db.leakDiaryDao()

    /**
     * 作答一条日记。
     *
     * [frequency] 为 null 表示"跳过"，非 null 表示真实作答 ——
     * 两种状态在库里可区分（AC-23）。用枚举收敛取值，杜绝任意字符串入库（评审 E2）。
     */
    suspend fun save(
        epochMs: Long,
        frequency: LeakFrequency?,
        context: LeakContext? = null,
    ) {
        val day = SevenDayPlan.epochDayOf(epochMs, zone)
        dao.upsert(
            LeakDiaryEntity(
                id = "leak-$day",
                date = day,
                frequency = frequency?.storageValue,
                context = context?.storageValue,
                skipped = frequency == null,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    /** 显式"跳过"入口，语义比 save(x, null) 清楚。 */
    suspend fun skip(epochMs: Long) = save(epochMs, frequency = null)

    suspend fun findByDay(epochMs: Long): LeakDiaryEntity? =
        dao.findByDay(SevenDayPlan.epochDayOf(epochMs, zone))

    suspend fun lastSevenDays(nowMs: Long): List<LeakDiaryEntity> {
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        return dao.between(today.minusDays(6).toEpochDay(), today.toEpochDay())
    }

    suspend fun countAll() = dao.countAll()
    suspend fun countAnswered() = dao.countAnswered()
    suspend fun countSkipped() = dao.countSkipped()
    suspend fun clearAll() = dao.deleteAll()
}