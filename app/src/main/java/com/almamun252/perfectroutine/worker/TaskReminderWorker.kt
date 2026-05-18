package com.almamun252.perfectroutine.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class TaskReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // WorkManager-এ শিডিউল করার সময় আমরা যে ডেটাগুলো পাঠাব, তা এখান থেকে রিসিভ করা হচ্ছে
        val taskTitle = inputData.getString("TASK_TITLE") ?: "কাজের রিমাইন্ডার"
        val message = inputData.getString("TASK_MESSAGE") ?: "আপনার একটি কাজ সম্পন্ন করার সময় হয়েছে!"

        // আমাদের তৈরি করা Helper ক্লাসের মাধ্যমে নোটিফিকেশন ফায়ার করা
        NotificationHelper.showNotification(applicationContext, taskTitle, message)

        return Result.success()
    }
}