package com.almamun252.perfectroutine.worker

import com.almamun252.perfectroutine.model.Task
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPolicyTest {
    private val task = Task(id = 7, title = "Quiz", subjectName = "OOP", tabType = "Learning",
        priority = "High", taskDate = 100L, deadline = 1000L)

    @Test fun firesAtDeadlineAndAfterSystemDelay() {
        assertTrue(reminderIsDue(task, 1000L, 1000L))
        assertTrue(reminderIsDue(task, 1000L, 2000L))
    }
    @Test fun neverFiresEarly() { assertFalse(reminderIsDue(task, 1000L, 999L)) }
    @Test fun skipsCompletedTask() {
        assertFalse(reminderIsDue(task.copy(isCompleted = true), 1000L, 2000L))
    }
    @Test fun skipsDeletedTask() { assertFalse(reminderIsDue(null, 1000L, 2000L)) }
    @Test fun skipsOldNotificationWhenDeadlineChanges() {
        assertFalse(reminderIsDue(task.copy(deadline = 3000L), 1000L, 2000L))
    }
    @Test fun skipsTaskWhoseDeadlineWasRemoved() {
        assertFalse(reminderIsDue(task.copy(deadline = null), 1000L, 2000L))
    }
}
