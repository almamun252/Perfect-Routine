package com.almamun252.perfectroutine.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.almamun252.perfectroutine.MainActivity
import com.almamun252.perfectroutine.utils.Constants
import kotlinx.coroutines.*

/**
 * FocusTimerService: এটি একটি Foreground Service।
 * এর মূল কাজ হলো টাইমার চলাকালীন অ্যাপটিকে ব্যাকগ্রাউন্ডে কিল (kill) হওয়া থেকে বাঁচানো
 * এবং ইউজারকে নোটিফিকেশনের মাধ্যমে টাইমারের আপডেট দেওয়া।
 */
class FocusTimerService : Service() {

    // রিমাইন্ডারের জন্য একটি Coroutine Job
    private var reminderJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val taskName = intent.getStringExtra(EXTRA_TASK_NAME) ?: "ফোকাস টাইমার চলছে"
                startForegroundService(taskName)

                // টাইমার শুরু হওয়ার সাথে সাথে রিমাইন্ডার লুপ চালু হবে
                startReminderLoop()
            }
            ACTION_UPDATE -> {
                // টাইমারের লাইভ সময় আপডেট করার জন্য
                val timeString = intent.getStringExtra(EXTRA_TIME_STRING) ?: "00:00"
                val taskName = intent.getStringExtra(EXTRA_TASK_NAME) ?: "ফোকাস টাইমার"
                updateNotification(taskName, timeString)
            }
            ACTION_STOP -> {
                stopForegroundService()
            }
        }
        return START_NOT_STICKY
    }

    // ১৫ মিনিট পর পর রিমাইন্ডার দেওয়ার লজিক
    private fun startReminderLoop() {
        reminderJob?.cancel() // আগের কোনো লুপ থাকলে বন্ধ করবে
        reminderJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                // ১৫ মিনিট = 15 * 60 * 1000 মিলি-সেকেন্ড
                // (টেস্ট করার জন্য আপনি চাইলে এখানে 10 * 1000L অর্থাৎ ১০ সেকেন্ড দিয়ে দেখতে পারেন)
                delay(15 * 60 * 1000L)

                // নির্দিষ্ট সময় পর পর এই ফাংশনটি কল হবে
                sendStrictModeWarning()
                // সাথে কাস্টম সাউন্ড প্লে করবে
                playCustomSound()
            }
        }
    }

    // ইউজারের সিলেক্ট করা সাউন্ড প্লে করার ফাংশন
    private fun playCustomSound() {
        try {
            val sharedPref = getSharedPreferences("PerfectRoutinePrefs", Context.MODE_PRIVATE)
            val savedUriString = sharedPref.getString("custom_timer_sound", null)

            val soundUri = if (savedUriString != null) {
                Uri.parse(savedUriString)
            } else {
                // ইউজার সাউন্ড সিলেক্ট না করলে ডিফল্ট নোটিফিকেশন সাউন্ড বাজবে
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(this, soundUri)
            mediaPlayer.prepare()
            mediaPlayer.start()

            // প্লে শেষ হলে মেমরি ক্লিয়ার করা
            mediaPlayer.setOnCompletionListener {
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startForegroundService(taskName: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle("ফোকাস মোড চালু আছে")
            .setContentText("কাজ: $taskName")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun updateNotification(taskName: String, timeString: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle("ফোকাস মোড: $timeString")
            .setContentText("কাজ: $taskName")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true) // আপডেট হওয়ার সময় যেন বারবার সাউন্ড না হয়
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun sendStrictModeWarning() {
        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("মনোযোগ দিন!")
            .setContentText("আপনার ফোকাস টাইমার চলছে। দয়া করে কাজে ফিরে আসুন!")
            .setPriority(NotificationCompat.PRIORITY_MAX) // হাই প্রায়োরিটি যাতে সামনে আসে
            .setDefaults(NotificationCompat.DEFAULT_ALL) // ডিফল্ট সাউন্ড ও ভাইব্রেশন
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(WARNING_NOTIFICATION_ID, notification)
    }

    private fun stopForegroundService() {
        reminderJob?.cancel() // ⚠️ সার্ভিস বন্ধ হলে রিমাইন্ডার লুপও বন্ধ হবে
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        const val NOTIFICATION_ID = 101
        const val WARNING_NOTIFICATION_ID = 102

        const val ACTION_START = "ACTION_START_TIMER"
        const val ACTION_UPDATE = "ACTION_UPDATE_TIMER"
        const val ACTION_STOP = "ACTION_STOP_TIMER"
        const val ACTION_STRICT_MODE_WARNING = "ACTION_STRICT_MODE_WARNING"

        const val EXTRA_TASK_NAME = "EXTRA_TASK_NAME"
        const val EXTRA_TIME_STRING = "EXTRA_TIME_STRING"
    }
}