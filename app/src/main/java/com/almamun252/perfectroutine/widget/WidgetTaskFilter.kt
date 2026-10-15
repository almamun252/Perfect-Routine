package com.almamun252.perfectroutine.widget

import com.almamun252.perfectroutine.model.Task
import java.util.Calendar

// Match the app's next-three-days horizon, including every older overdue task.
internal fun widgetTasks(tasks: List<Task>, now: Long): List<Task> {
    val end = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, 3)
    }.timeInMillis
    return tasks.filter { !it.isCompleted && it.deadline != null && it.deadline <= end }
        .sortedWith(compareBy<Task> { it.deadline }.thenBy { it.id })
}
