package com.almamun252.perfectroutine.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException

class TaskReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val taskId = inputData.getInt(TaskReminderScheduler.TASK_ID, -1)
        val deadline = inputData.getLong(TaskReminderScheduler.DEADLINE, Long.MIN_VALUE)
        if (taskId < 0 || deadline == Long.MIN_VALUE) return Result.failure()
        return try {
            TaskReminderScheduler.deliver(applicationContext, taskId, deadline)
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e("TaskReminders", "Reminder data unavailable", error)
            Result.failure()
        }
    }
}

class ReminderRescheduleWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        TaskReminderScheduler.reconcileAll(applicationContext)
        Result.success()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Log.e("TaskReminders", "Could not restore reminders", error)
        Result.failure()
    }
}
