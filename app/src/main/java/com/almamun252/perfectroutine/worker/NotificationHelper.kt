package com.almamun252.perfectroutine.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.almamun252.perfectroutine.MainActivity
import com.almamun252.perfectroutine.R
import com.almamun252.perfectroutine.model.Task
import com.almamun252.perfectroutine.utils.Constants

object NotificationHelper {
    private const val TASK_TAG = "routine-task-deadline"
    private const val TEST_ID = 9001

    // Separate silent timer updates from audible task deadlines and focus warnings.
    fun createNotificationChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channels = listOf(
            NotificationChannel(Constants.TASK_CHANNEL_ID, "Task reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifications when a pending task reaches its deadline"
                enableVibration(true)
            },
            NotificationChannel(Constants.TIMER_CHANNEL_ID, "Focus timer", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Silent progress updates while the timer is running"
                setSound(null, null)
            },
            NotificationChannel(Constants.FOCUS_ALERT_CHANNEL_ID, "Focus reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "The existing 15-minute focus reminder"
                // The service plays the user's selected sound, once.
                setSound(null, null)
                enableVibration(true)
            }
        )
        manager.createNotificationChannels(channels)
    }

    fun canShowNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        return (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).areNotificationsEnabled()
    }

    fun canShowChannel(context: Context, channelId: String): Boolean {
        if (!canShowNotifications(context)) return false
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return manager.getNotificationChannel(channelId)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun taskNotificationsEnabled(context: Context): Boolean {
        createNotificationChannel(context)
        return canShowChannel(context, Constants.TASK_CHANNEL_ID)
    }

    fun showTaskNotification(context: Context, task: Task): Boolean {
        createNotificationChannel(context)
        if (!canShowChannel(context, Constants.TASK_CHANNEL_ID)) return false
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("perfectroutine://task/${task.id}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(context, task.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val message = "${task.subjectName} · ${task.priority}\nএই কাজের deadline হয়েছে। কাজটি এখনও pending আছে।"
        val notification = NotificationCompat.Builder(context, Constants.TASK_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_task_notification)
            .setContentTitle(task.title)
            .setContentText("${task.subjectName} · কাজের deadline হয়েছে")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .build()
        return try {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(TASK_TAG, task.id, notification)
            true
        } catch (_: SecurityException) { false }
    }

    fun cancelTaskNotification(context: Context, taskId: Int) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(TASK_TAG, taskId)
    }

    // Keep the former helper signature for callers, now with a channel and click target.
    fun showNotification(context: Context, title: String, message: String): Boolean {
        createNotificationChannel(context)
        if (!canShowChannel(context, Constants.TASK_CHANNEL_ID)) return false
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("perfectroutine://home")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(context, TEST_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, Constants.TASK_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_task_notification).setContentTitle(title).setContentText(message)
            .setContentIntent(pending).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build()
        return try {
            manager.cancel("routine-test", TEST_ID)
            manager.notify("routine-test", TEST_ID, notification)
            true
        } catch (_: SecurityException) { false }
    }
}
