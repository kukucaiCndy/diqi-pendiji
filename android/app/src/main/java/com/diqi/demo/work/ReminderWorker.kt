package com.diqi.demo.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.diqi.demo.MainActivity
import java.util.concurrent.TimeUnit

/**
 * 本地每日提醒（P1，需求 §2.2 与 FR-02 的提醒入口）。
 *
 * ## 为什么用 WorkManager 而不是 AlarmManager
 *
 * 提醒只需要"差不多这个点"，不需要秒级精确；WorkManager 自带
 * 跨重启持久化与省电批处理，AlarmManager 的精确闹钟在 Android 12+
 * 还需要额外权限（SCHEDULE_EXACT_ALARM），得不偿失。
 *
 * ## 通知权限
 *
 * Android 13+ 没授权时通知不会显示。这里在触发前检查一次权限，
 * 没授权就安静跳过 —— 不弹窗、不打搅，用户可以在设置里自己开。
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext

        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return Result.success()

        val manager = context.getSystemService(NotificationManager::class.java)
            ?: return Result.success()

        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "练习提醒", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply {
                        description = "每天提醒做一组练习"
                        setShowBadge(true)
                    },
            )
        }

        val intent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = android.app.Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("今天的练习还没做")
            .setContentText("花几分钟练一组，跟着语音做就好。")
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "daily_reminder"
        const val NOTIFICATION_ID = 2001
        private const val WORK_NAME = "herrhythm_daily_reminder"

        /** 开启/更新每日提醒。同一时刻只保留一条（REPLACE）。 */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(1, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}