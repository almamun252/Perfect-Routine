package com.almamun252.perfectroutine.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.almamun252.perfectroutine.data.AppDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object TaskReminderScheduler {
    const val TASK_ID = "TASK_ID"
    const val DEADLINE = "EXPECTED_DEADLINE"
    private val reminderMutex = Mutex()

    fun canUseExactAlarms(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()

    fun requestReconcile(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork("routine-reminder-reconcile",
            ExistingWorkPolicy.KEEP, OneTimeWorkRequestBuilder<ReminderRescheduleWorker>().build())
    }

    suspend fun reconcileAll(context: Context) {
        val tasks = AppDatabase.getDatabase(context).taskDao().getAllTasks().first()
        tasks.forEach { reconcileTask(context, it.id) }
    }

    // Reread Room inside the lock so an edit wins over an older startup snapshot.
    suspend fun reconcileTask(context: Context, taskId: Int) {
        try {
            withContext(Dispatchers.IO) {
                reminderMutex.withLock {
                    val appContext = context.applicationContext
                    val task = AppDatabase.getDatabase(appContext).taskDao().getTaskById(taskId)
                    val alarms = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val work = WorkManager.getInstance(appContext)
                    val oldAlarm = pendingIntent(appContext, taskId, 0L, PendingIntent.FLAG_NO_CREATE)
                    if (oldAlarm != null) alarms.cancel(oldAlarm)
                    val deadline = task?.deadline
                    if (task == null || task.isCompleted || deadline == null) {
                        work.cancelUniqueWork(workName(taskId))
                        NotificationHelper.cancelTaskNotification(appContext, taskId)
                        return@withLock
                    }
                    // Avoid a flood of historical overdue reminders whenever the app starts.
                    if (deadline <= System.currentTimeMillis()) {
                        work.cancelUniqueWork(workName(taskId))
                        return@withLock
                    }
                    NotificationHelper.cancelTaskNotification(appContext, taskId)
                    if (canUseExactAlarms(appContext)) {
                        try {
                            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, deadline,
                                pendingIntent(appContext, taskId, deadline, PendingIntent.FLAG_UPDATE_CURRENT)!!)
                            work.cancelUniqueWork(workName(taskId))
                            return@withLock
                        } catch (permissionChanged: SecurityException) {
                            Log.w("TaskReminders", "Exact access unavailable; using delayed work", permissionChanged)
                        }
                    }
                    val request = OneTimeWorkRequestBuilder<TaskReminderWorker>()
                        .setInputData(workDataOf(TASK_ID to taskId, DEADLINE to deadline))
                        .setInitialDelay((deadline - System.currentTimeMillis()).coerceAtLeast(0L), TimeUnit.MILLISECONDS)
                        .build()
                    work.enqueueUniqueWork(workName(taskId), ExistingWorkPolicy.REPLACE, request)
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e("TaskReminders", "Could not schedule task $taskId; saved task was not reset", error)
        }
    }

    // Dedupe metadata stays outside Room; no database version change is needed.
    suspend fun deliver(context: Context, taskId: Int, expectedDeadline: Long) {
        withContext(Dispatchers.IO) {
            reminderMutex.withLock {
                val task = AppDatabase.getDatabase(context).taskDao().getTaskById(taskId)
                if (!reminderIsDue(task, expectedDeadline, System.currentTimeMillis())) return@withLock
                val prefs = context.getSharedPreferences("TaskReminderDelivery", Context.MODE_PRIVATE)
                val key = "delivered_$taskId"
                if (prefs.getLong(key, Long.MIN_VALUE) == expectedDeadline) return@withLock
                if (NotificationHelper.showTaskNotification(context, task!!)) {
                    prefs.edit().putLong(key, expectedDeadline).commit()
                }
            }
        }
    }

    private fun workName(taskId: Int) = "routine-deadline-$taskId"

    private fun pendingIntent(context: Context, taskId: Int, deadline: Long, flags: Int): PendingIntent? {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            data = Uri.parse("perfectroutine://reminder/$taskId")
            putExtra(TASK_ID, taskId)
            putExtra(DEADLINE, deadline)
        }
        return PendingIntent.getBroadcast(context, taskId, intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }
}
