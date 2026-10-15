package com.almamun252.perfectroutine.worker

import com.almamun252.perfectroutine.model.Task

// Never alert for a completed/deleted task or a stale deadline from an old schedule.
internal fun reminderIsDue(task: Task?, expectedDeadline: Long, now: Long): Boolean =
    task != null && !task.isCompleted && task.deadline == expectedDeadline && expectedDeadline <= now
