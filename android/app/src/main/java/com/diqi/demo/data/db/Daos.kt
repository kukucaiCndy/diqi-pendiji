package com.diqi.demo.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * 训练记录 DAO。
 *
 * ## 硬规则一：统计查询必须带 `sensorMode != 'simulated'`
 *
 * FR-14 / AC-19 要求演示数据不计入打卡、不出现在周报。规则由
 * `SimulatedFilterTest` **扫描 src/main 下全部 Kotlin 源码**逐条断言。
 *
 * ## 硬规则二：统计只认 `durationMs`，且有 60 秒时长门槛
 *
 * - 时长一律读 `durationMs`（单调时钟），**不要**用 `endedAt - startedAt`：
 *   训练中校时会污染墙钟差（评审 D2）。
 * - 时长 < 60s 的会话视为误触，不进打卡/周报/Day 进度。
 *   首版这段规则只写在注释里、SQL 里没有，属于"注释与实现不一致"（评审 D1）。
 *
 * ## 硬规则三：写训练记录用 [Upsert]，**不用 REPLACE**
 *
 * `OnConflictStrategy.REPLACE` 在 SQLite 里是 `DELETE` + `INSERT`，
 * 会触发 `sensor_quality` 的 `ON DELETE CASCADE` —— 自评回填时
 * 会静默删掉刚写好的质量汇总行（评审 A1，已用 sqlite3 复现）。
 */
@Dao
interface TrainingSessionDao {

    /** 幂等写入。**必须用 @Upsert**（见类注释硬规则三）。 */
    @Upsert
    suspend fun upsert(session: TrainingSessionEntity)

    @Query("SELECT * FROM training_session WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): TrainingSessionEntity?

    /** 最近一条真实训练（首页「上次训练」）。不含未来时间戳（见种子逻辑）。 */
    @Query(
        "SELECT * FROM training_session WHERE sensorMode != 'simulated' " +
            "ORDER BY startedAt DESC LIMIT 1",
    )
    suspend fun latestReal(): TrainingSessionEntity?

    @Query(
        "SELECT * FROM training_session WHERE sensorMode != 'simulated' " +
            "ORDER BY startedAt DESC LIMIT :limit",
    )
    fun observeRecentReal(limit: Int): Flow<List<TrainingSessionEntity>>

    @Query(
        "SELECT * FROM training_session WHERE sensorMode != 'simulated' " +
            "ORDER BY startedAt DESC LIMIT :limit",
    )
    suspend fun recentReal(limit: Int): List<TrainingSessionEntity>

    /** 区间内有效真实训练条数（有时长门槛）*/
    @Query(
        "SELECT COUNT(*) FROM training_session " +
            "WHERE sensorMode != 'simulated' AND startedAt >= :fromMs AND startedAt < :toMs " +
            "AND durationMs >= " + TrainingSessionDao.MIN_COUNTED_DURATION_MS,
    )
    suspend fun countRealBetween(fromMs: Long, toMs: Long): Int

    /** 区间内有效真实训练总时长（毫秒，读 durationMs）*/
    @Query(
        "SELECT COALESCE(SUM(durationMs), 0) FROM training_session " +
            "WHERE sensorMode != 'simulated' AND startedAt >= :fromMs AND startedAt < :toMs " +
            "AND durationMs >= " + TrainingSessionDao.MIN_COUNTED_DURATION_MS,
    )
    suspend fun totalDurationMsRealBetween(fromMs: Long, toMs: Long): Long

    /** 有效真实训练的开始时间列表（用于日历打卡点）*/
    @Query(
        "SELECT startedAt FROM training_session " +
            "WHERE sensorMode != 'simulated' AND startedAt >= :fromMs AND startedAt < :toMs " +
            "AND durationMs >= " + TrainingSessionDao.MIN_COUNTED_DURATION_MS +
            " ORDER BY startedAt ASC",
    )
    suspend fun realStartTimesBetween(fromMs: Long, toMs: Long): List<Long>

    /** 最早一条**有效**真实训练的起始时间（Day 进度起点）*/
    @Query(
        "SELECT MIN(startedAt) FROM training_session " +
            "WHERE sensorMode != 'simulated' " +
            "AND durationMs >= " + TrainingSessionDao.MIN_COUNTED_DURATION_MS,
    )
    suspend fun firstRealStartAt(): Long?

    /** 区间内的有效真实训练（七天页逐日明细）*/
    @Query(
        "SELECT * FROM training_session " +
            "WHERE sensorMode != 'simulated' AND startedAt >= :fromMs AND startedAt < :toMs " +
            "AND durationMs >= " + TrainingSessionDao.MIN_COUNTED_DURATION_MS +
            " ORDER BY startedAt ASC",
    )
    suspend fun realBetween(fromMs: Long, toMs: Long): List<TrainingSessionEntity>

    /**
     * 演示记录（**唯一**允许查出 simulated 的接口）。
     * 仅在记录页「演示记录」区使用，且必须带水印展示（AC-19）。
     */
    @Query(
        "SELECT * FROM training_session WHERE sensorMode = 'simulated' " +
            "ORDER BY startedAt DESC LIMIT :limit",
    )
    suspend fun simulatedSessions(limit: Int): List<TrainingSessionEntity>

    @Query("SELECT COUNT(*) FROM training_session")
    suspend fun countAll(): Int

    @Query("DELETE FROM training_session")
    suspend fun deleteAll()

    @Query("DELETE FROM training_session WHERE sensorMode = 'simulated'")
    suspend fun deleteSimulated()

    /** 删除 debug 种子（按 id 前缀）。种子用的是真实模式，不能靠 deleteSimulated 清。 */
    @Query("DELETE FROM training_session WHERE id LIKE 'seed-day-%'")
    suspend fun deleteSeeds()

    /**
     * 计入统计的最小时长。
     *
     * 抽成常量并直接拼进 SQL：如果只写在注释里而不进 SQL，
     * 就变成"文档说 60 秒、实现没门槛"的假规则（评审 D1）。
     */
    companion object {
        const val MIN_COUNTED_DURATION_MS = 60_000L
    }
}

@Dao
interface SensorQualityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(quality: SensorQualityEntity)

    /** 按会话主键读取。调用方已持有 session，不涉及演示隔离。 */
    @Query("SELECT * FROM sensor_quality WHERE sessionId = :sessionId LIMIT 1")
    suspend fun findBySession(sessionId: String): SensorQualityEntity?

    /**
     * 跨会话的质量聚合。
     *
     * **必须 JOIN training_session 才能过滤 simulated** ——
     * 质量表本身没有 sensorMode（见 `SensorQualityEntity` 的裁定说明）。
     * 这是 AC-19 在质量维度上的唯一防线。
     */
    @Query(
        "SELECT AVG(q.validRatio) FROM sensor_quality q " +
            "INNER JOIN training_session s ON s.id = q.sessionId " +
            "WHERE s.sensorMode != 'simulated' AND s.startedAt >= :fromMs AND s.startedAt < :toMs",
    )
    suspend fun averageValidRatioRealBetween(fromMs: Long, toMs: Long): Double?

    @Query("DELETE FROM sensor_quality")
    suspend fun deleteAll()
}

@Dao
interface LeakDiaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: LeakDiaryEntity)

    @Query("SELECT * FROM leak_diary WHERE date = :epochDay LIMIT 1")
    suspend fun findByDay(epochDay: Long): LeakDiaryEntity?

    @Query(
        "SELECT * FROM leak_diary WHERE date >= :fromDay AND date <= :toDay ORDER BY date ASC",
    )
    suspend fun between(fromDay: Long, toDay: Long): List<LeakDiaryEntity>

    @Query("SELECT COUNT(*) FROM leak_diary")
    suspend fun countAll(): Int

    /** 真正作答过的条数（AC-23：全跳过时应为 0）*/
    @Query("SELECT COUNT(*) FROM leak_diary WHERE skipped = 0")
    suspend fun countAnswered(): Int

    /** 跳过条数：与 countAnswered 一起构成三态判据 */
    @Query("SELECT COUNT(*) FROM leak_diary WHERE skipped = 1")
    suspend fun countSkipped(): Int

    @Query("DELETE FROM leak_diary")
    suspend fun deleteAll()
}