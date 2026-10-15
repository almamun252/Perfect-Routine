package com.almamun252.perfectroutine.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Only validate one task and post its notification; no persistent service or loop.
class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(TaskReminderScheduler.TASK_ID, -1)
        val deadline = intent.getLongExtra(TaskReminderScheduler.DEADLINE, Long.MIN_VALUE)
        if (taskId < 0 || deadline == Long.MIN_VALUE) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                TaskReminderScheduler.deliver(context.applicationContext, taskId, deadline)
            } catch (error: Exception) {
                Log.e("TaskReminders", "Unable to deliver deadline notification", error)
            } finally { pending.finish() }
        }
    }
}

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TaskReminderScheduler.requestReconcile(context.applicationContext)
    }
}
