package com.almamun252.perfectroutine.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.almamun252.perfectroutine.utils.Constants

object NotificationHelper {

    /**
     * নোটিফিকেশন চ্যানেল তৈরি করার ফাংশন।
     * এটি অ্যাপ চালু হওয়ার সময় (যেমন MainActivity বা Application ক্লাসে) একবার কল করতে হবে।
     */
    fun createNotificationChannel(context: Context) {
        val name = Constants.NOTIFICATION_CHANNEL_NAME
        val descriptionText = "Channel for Perfect Routine Task Reminders"
        val importance = NotificationManager.IMPORTANCE_HIGH

        val channel = NotificationChannel(Constants.NOTIFICATION_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }

        // সিস্টেমের NotificationManager-এ চ্যানেলটি রেজিস্টার করা
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(channel)
    }

    /**
     * যেকোনো জায়গা থেকে নোটিফিকেশন দেখানোর জন্য হেল্পার ফাংশন।
     * এটি মূলত WorkManager থেকে কল করা হবে।
     */
    fun showNotification(context: Context, title: String, message: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (context.checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return // permission না থাকলে notification দেখাবে না
            }
        }
        val builder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            // নোটিফিকেশনের আইকন (এখানে ডিফল্ট অ্যান্ড্রয়েড আইকন দেওয়া হয়েছে, আপনি পরে আপনার অ্যাপের আইকন বসাতে পারেন)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // নোটিফিকেশনটি পপ-আপ হয়ে স্ক্রিনে আসার জন্য
            .setAutoCancel(true) // নোটিফিকেশনে ক্লিক করলে যেন সেটি ক্লিয়ার হয়ে যায়

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // প্রতিটি নোটিফিকেশনের আলাদা ID দেওয়ার জন্য বর্তমান সময়ের মিলি-সেকেন্ড ব্যবহার করা হয়েছে
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, builder.build())
    }
}