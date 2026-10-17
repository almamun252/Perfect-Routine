package com.almamun252.perfectroutine.utils

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class TaskScheduleTest {
    @Test fun localDateSurvivesPositiveAndNegativeUtcOffsets() {
        for (zoneId in listOf("Asia/Dhaka", "America/Los_Angeles", "Pacific/Kiritimati")) {
            val zone = TimeZone.getTimeZone(zoneId)
            for (hour in listOf(0, 23)) {
                val instant = Calendar.getInstance(zone).apply { clear(); set(2026, 9, 17, hour, 45) }.timeInMillis
                val result = TaskSchedule.localTime(TaskSchedule.pickerDate(instant, zone), hour, 45, zone)
                assertEquals(zoneId, instant, result)
                assertEquals(0L, result % 60000L)
            }
        }
    }

    @Test fun deadlineMayBeAbsentOrEqualButCannotPrecedeStart() {
        assertFalse(TaskSchedule.invalid(1000, null))
        assertFalse(TaskSchedule.invalid(1000, 1000))
        assertFalse(TaskSchedule.invalid(1000, 2000))
        assertTrue(TaskSchedule.invalid(1000, 999))
    }
}
