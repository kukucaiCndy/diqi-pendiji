package com.diqi.demo.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.diqi.demo.domain.training.EngineSnapshot
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * 训练中断快照的持久化（AC-13）。
 *
 * ## 为什么单独一个 DataStore 文件
 *
 * 快照的写入频率远高于设置（每次暂停/结束/定期落盘），
 * 与设置混在一个文件里会让设置读路径跟着抖动，且一旦快照写坏会连带
 * 设置一起读不出来。分开文件的代价只是一次初始化。
 *
 * 注意：DataStore 必须**一个文件一个实例**，故这里用 `by preferencesDataStore`
 * 委托，且文件名与设置那个不同。
 */
private val Context.snapshotDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "training_snapshot",
)

/**
 * 快照存储抽象。
 *
 * 抽接口不是为了"将来可能换实现"，而是为了**可测性**：
 * 协调器的恢复逻辑必须能用内存假实现在 JVM 里跑，
 * 否则 AC-13 只能靠真机验证。
 */
interface SnapshotStore {
    suspend fun load(): EngineSnapshot?
    suspend fun save(snapshot: EngineSnapshot)
    suspend fun clear()
}

class DataStoreSnapshotStore(context: Context) : SnapshotStore {

    private val dataStore = context.applicationContext.snapshotDataStore

    /** 读取快照。损坏或字段缺失时返回 null（当作"没有未完成的训练"）。 */
    override suspend fun load(): EngineSnapshot? {
        val prefs = dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .first()
        val version = prefs[PLAN_VERSION] ?: return null
        val elapsed = prefs[ELAPSED_ACTIVE_MS] ?: return null
        return EngineSnapshot(
            startedAtWallMs = prefs[STARTED_AT_WALL_MS] ?: 0L,
            accumulatedPausedMs = prefs[PAUSED_MS] ?: 0L,
            pauseCount = prefs[PAUSE_COUNT] ?: 0,
            planVersion = version,
            plannedReps = prefs[PLANNED_REPS] ?: 0,
            elapsedActiveMs = elapsed,
            wasPaused = prefs[WAS_PAUSED] ?: false,
        )
    }

    override suspend fun save(snapshot: EngineSnapshot) {
        dataStore.edit { prefs ->
            prefs[STARTED_AT_WALL_MS] = snapshot.startedAtWallMs
            prefs[PAUSED_MS] = snapshot.accumulatedPausedMs
            prefs[PAUSE_COUNT] = snapshot.pauseCount
            prefs[PLAN_VERSION] = snapshot.planVersion
            prefs[PLANNED_REPS] = snapshot.plannedReps
            prefs[ELAPSED_ACTIVE_MS] = snapshot.elapsedActiveMs
            prefs[WAS_PAUSED] = snapshot.wasPaused
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val STARTED_AT_WALL_MS = longPreferencesKey("started_at_wall_ms")
        val PAUSED_MS = longPreferencesKey("paused_ms")
        val PAUSE_COUNT = intPreferencesKey("pause_count")
        val PLAN_VERSION = stringPreferencesKey("plan_version")
        val PLANNED_REPS = intPreferencesKey("planned_reps")
        val ELAPSED_ACTIVE_MS = longPreferencesKey("elapsed_active_ms")
        val WAS_PAUSED = booleanPreferencesKey("was_paused")
    }
}