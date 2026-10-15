package com.almamun252.perfectroutine.widget

import com.almamun252.perfectroutine.model.Task
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class WidgetTaskFilterTest {
    private val now = 1_800_000_000_000L
    private val cutoff = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, 3)
    }.timeInMillis

    private fun task(id: Int, deadline: Long?, done: Boolean = false, tab: String = "Learning") =
        Task(id, "Task $id", subjectName = "Test", tabType = tab, priority = "High",
            isCompleted = done, taskDate = now, deadline = deadline)

    @Test fun includesAllOverdueAndUpcomingAcrossTabs() {
        val tasks = listOf(task(1, cutoff, tab = "Work"), task(2, now - 100_000_000_000L),
            task(3, now, tab = "Research"), task(4, cutoff + 1), task(5, null), task(6, now - 1, true))
        assertEquals(listOf(2, 3, 1), widgetTasks(tasks, now).map { it.id })
    }

    @Test fun horizonMovesWithoutADataEdit() {
        val task = task(1, cutoff + 1)
        assertEquals(emptyList<Task>(), widgetTasks(listOf(task), now))
        assertEquals(listOf(task), widgetTasks(listOf(task), now + 1))
    }

    @Test fun equalDeadlinesHaveStableOrder() {
        assertEquals(listOf(1, 2), widgetTasks(listOf(task(2, now), task(1, now)), now).map { it.id })
    }
}
