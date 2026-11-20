package com.diqi.demo.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TrainingSessionEntity::class,
        SensorQualityEntity::class,
        LeakDiaryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class HerRhythmDb : RoomDatabase() {

    abstract fun trainingSessionDao(): TrainingSessionDao
    abstract fun sensorQualityDao(): SensorQualityDao
    abstract fun leakDiaryDao(): LeakDiaryDao

    companion object {
        /**
         * 数据库文件名。
         *
         * `tools/e2e.sh` 的 `dbpull()` 按 `herrhythm*.db` 优先匹配主库，
         * 就是为了绕开 WorkManager 的 `androidx.work.workdb` ——
         * 改名时请同步 `tools/e2e.sh`。
         */
        const val NAME = "herrhythm.db"

        fun create(context: Context): HerRhythmDb =
            Room.databaseBuilder(context, HerRhythmDb::class.java, NAME)
                // 需求 §7：数据只留本机。schema 有迁移（schemas/ 目录），
                // 所以**不使用**破坏性迁移 —— 否则升级会抹掉 AC-12 要验的历史。
                .build()
    }
}