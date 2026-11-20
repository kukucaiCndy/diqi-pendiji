package com.diqi.demo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.LifecycleService
import com.diqi.demo.MainActivity

/**
 * 训练前台服务：保证锁屏/切后台时计时与语音不中断（AC-05）。
 *
 * 类型用 `mediaPlayback`（本服务确实在播放语音音频），**不用 camera**：
 * 相机归属 application-scoped 的 SensorCoordinator（D12），这样锁屏停投喂
 * 但 SDK 窗口保留，返回后指标可快速恢复，同时避免 Android 14+ 对
 * camera 类型前台服务的额外审核与限制。
 *
 * S5 会把训练引擎的 tick 驱动与语音播放接进来。当前是**可实例化的完整骨架**：
 * 之所以现在就建，是因为 Manifest 已经声明了这个 service ——
 * 声明了却没有类，S5 一 `startForegroundService` 就会 ClassNotFoundException。
 */
class TrainingForegroundService : LifecycleService() {

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification(paused = false))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_UPDATE -> startForeground(
                NOTIFICATION_ID,
                buildNotification(paused = intent.getBooleanExtra(EXTRA_PAUSED, false)),
            )
            ACTION_STOP -> stopSelf()
        }
        // 非粘性：训练状态由 DataStore 快照恢复，不靠系统重投递。
        return START_NOT_STICKY
    }

    private fun createChannel() {
        // minSdk 26 ⇒ NotificationChannel 必然可用，无需版本分支
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "训练中",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "训练进行中的常驻提示"
                setShowBadge(false)
                enableVibration(false)
            },
        )
    }

    private fun buildNotification(paused: Boolean): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                // singleTop + 这些 flag：回到**同一个** Activity 实例（D13），
                // 不会新建第二个 NavHost / 第二份计时。
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_NAV_TO_TRAINING, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_headset)
            .setContentTitle(if (paused) "训练已暂停" else "正在训练")
            .setContentText(if (paused) "回到应用继续今天的练习" else "点击回到训练")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "training_foreground"
        const val NOTIFICATION_ID = 1001
        const val ACTION_UPDATE = "com.diqi.demo.action.TRAINING_UPDATE"
        const val ACTION_STOP = "com.diqi.demo.action.TRAINING_STOP"
        const val EXTRA_PAUSED = "paused"
        const val EXTRA_NAV_TO_TRAINING = "nav_to_training"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, TrainingForegroundService::class.java))
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, TrainingForegroundService::class.java)
                    .setAction(ACTION_STOP),
            )
        }
    }
}