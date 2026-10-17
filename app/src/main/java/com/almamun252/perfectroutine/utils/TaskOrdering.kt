package com.almamun252.perfectroutine.utils

import com.almamun252.perfectroutine.model.Task

// Home and widgets share the same order, including legacy tasks without startDate.
object TaskOrdering {
    fun comparator(now: Long): Comparator<Task> = compareBy<Task>(
        { if ((it.startDate ?: it.taskDate) <= now) 0 else 1 },
        { it.startDate ?: it.taskDate },
        { when (it.priority) { "High" -> 0; "Medium" -> 1; else -> 2 } },
        { it.deadline ?: Long.MAX_VALUE },
        { it.id }
    )
}
