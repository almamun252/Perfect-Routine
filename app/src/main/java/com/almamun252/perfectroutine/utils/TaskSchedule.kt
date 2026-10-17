package com.almamun252.perfectroutine.utils

import java.util.Calendar
import java.util.TimeZone

object TaskSchedule {
    // Material date pickers encode a calendar date at UTC midnight, not a local instant.
    fun pickerDate(instant: Long, zone: TimeZone = TimeZone.getDefault()): Long {
        val local = Calendar.getInstance(zone).apply { timeInMillis = instant }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }

    fun localTime(date: Long, hour: Int, minute: Int, zone: TimeZone = TimeZone.getDefault()): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = date }
        return Calendar.getInstance(zone).apply {
            clear()
            set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), hour, minute, 0)
        }.timeInMillis
    }

    fun invalid(start: Long, deadline: Long?): Boolean = deadline != null && deadline < start
}
