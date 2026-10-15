package com.almamun252.perfectroutine.utils

import com.almamun252.perfectroutine.model.Task
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.TimeZone

class HomeTaskFilterTest {
    private val now = Instant.parse("2026-10-10T23:00:00Z").toEpochMilli()
    private val dhaka = TimeZone.getTimeZone("Asia/Dhaka")
    private fun task(deadline: Long?, done: Boolean = false) = Task(title = "Note", subjectName = "Java",
        tabType = "Learning", priority = "Medium", taskDate = 0, deadline = deadline, isCompleted = done)

    @Test fun oldPendingDeadlineIsOverdueEvenOutsideUpcomingFilter() {
        val old = task(now - 30L * 86_400_000L)
        assertTrue(HomeTaskFilter.isOverdue(old, now))
        assertFalse(HomeTaskFilter.matchesDate(old, "আগামী ৩ দিনের কাজ", now))
    }
    @Test fun completedAndUndatedTasksAreNeverOverdue() {
        assertFalse(HomeTaskFilter.isOverdue(task(now - 1, true), now))
        assertFalse(HomeTaskFilter.isOverdue(task(null), now))
    }
    @Test fun dueBoundaryChangesToOverdueAfterDeadline() {
        assertFalse(HomeTaskFilter.isOverdue(task(now), now))
        assertTrue(HomeTaskFilter.isOverdue(task(now), now + 1))
    }
    @Test fun tomorrowFilterUsesDhakaCalendarDate() {
        val earlyTomorrow = Instant.parse("2026-10-11T18:00:00Z").toEpochMilli()
        assertTrue(HomeTaskFilter.matchesDate(task(earlyTomorrow), "আগামীকালের কাজ", now, zone = dhaka))
        assertFalse(HomeTaskFilter.matchesDate(task(earlyTomorrow), "আজকের কাজ", now, zone = dhaka))
    }
    @Test fun customEndDateIncludesTheLocalEvening() {
        val pickerDay = Instant.parse("2026-10-11T00:00:00Z").toEpochMilli()
        val lateLocal = Instant.parse("2026-10-11T17:59:59Z").toEpochMilli()
        assertTrue(HomeTaskFilter.matchesDate(task(lateLocal), "কাস্টম রেঞ্জ", now, pickerDay, pickerDay, dhaka))
        assertFalse(HomeTaskFilter.matchesDate(task(lateLocal + 1000), "কাস্টম রেঞ্জ", now, pickerDay, pickerDay, dhaka))
    }
}
