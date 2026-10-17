package com.almamun252.perfectroutine.utils

import com.almamun252.perfectroutine.model.Task
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskOrderingTest {
    private val now = 10_000L
    private fun task(id: Int, start: Long?, priority: String, deadline: Long? = null) =
        Task(id = id, title = "Task", subjectName = "Test", tabType = "Work",
            priority = priority, taskDate = 1000, startDate = start, deadline = deadline)

    @Test fun futureHighPriorityCannotJumpAheadOfStartedLowPriority() {
        val tasks = listOf(task(1, now + 1000, "High", now + 2000), task(2, now - 1000, "Low", now + 5000))
        assertEquals(listOf(2, 1), tasks.sortedWith(TaskOrdering.comparator(now)).map { it.id })
    }

    @Test fun earlierStartWinsThenPriorityBreaksEqualStartTies() {
        val tasks = listOf(task(1, 2000, "High"), task(2, 1000, "Low"),
            task(3, 2000, "Low"), task(4, 2000, "Medium"))
        assertEquals(listOf(2, 1, 4, 3), tasks.sortedWith(TaskOrdering.comparator(now)).map { it.id })
    }

    @Test fun legacyStartFallsBackToCreationTimeAndEqualValuesUseStableIds() {
        val tasks = listOf(task(3, null, "High"), task(2, 1000, "High"), task(1, 2000, "Low"))
        assertEquals(listOf(2, 3, 1), tasks.sortedWith(TaskOrdering.comparator(now)).map { it.id })
    }
}
