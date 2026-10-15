package com.almamun252.perfectroutine.utils

import com.almamun252.perfectroutine.model.Task
import java.util.Calendar
import java.util.TimeZone

// Overdue pending tasks stay visible independently of the selected upcoming date range.
object HomeTaskFilter {
    fun isOverdue(task: Task, now: Long): Boolean =
        !task.isCompleted && task.deadline?.let { it < now } == true

    fun matchesDate(task: Task, filter: String, now: Long,
                    start: Long? = null, end: Long? = null,
                    zone: TimeZone = TimeZone.getDefault()): Boolean {
        if (filter == "সব কাজ") return true
        val deadline = task.deadline ?: return false
        val dayStart = Calendar.getInstance(zone).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        fun day(offset: Int): Long = (dayStart.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, offset)
        }.timeInMillis
        return when (filter) {
            "আজকের কাজ" -> deadline >= day(0) && deadline < day(1)
            "আগামীকালের কাজ" -> deadline >= day(1) && deadline < day(2)
            "আগামী ৩ দিনের কাজ" -> deadline >= now && deadline <=
                Calendar.getInstance(zone).apply { timeInMillis = now; add(Calendar.DAY_OF_YEAR, 3) }.timeInMillis
            "এই সপ্তাহের" -> deadline >= now && deadline <=
                Calendar.getInstance(zone).apply { timeInMillis = now; add(Calendar.DAY_OF_YEAR, 7) }.timeInMillis
            "কাস্টম রেঞ্জ" -> {
                // Material DatePicker stores the chosen calendar date at UTC midnight.
                fun localPickerDay(value: Long, next: Boolean): Long {
                    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = value }
                    return Calendar.getInstance(zone).apply {
                        clear()
                        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
                        if (next) add(Calendar.DAY_OF_YEAR, 1)
                    }.timeInMillis
                }
                deadline >= (start?.let { localPickerDay(it, false) } ?: Long.MIN_VALUE) &&
                    (end == null || deadline < localPickerDay(end, true))
            }
            else -> true
        }
    }
}
