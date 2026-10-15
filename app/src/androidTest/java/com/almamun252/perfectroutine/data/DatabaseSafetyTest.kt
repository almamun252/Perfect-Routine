package com.almamun252.perfectroutine.data

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DatabaseSafetyTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "database-safety-${UUID.randomUUID()}.db"

    @After fun cleanUp() { context.deleteDatabase(name) }

    // Use the production builder so a future destructive fallback breaks this test.
    private fun seedDatabase() {
        val db = AppDatabase.buildDatabase(context, name)
        try {
            val sql = db.openHelper.writableDatabase
            sql.execSQL("INSERT INTO category_table (id, name, tabType) VALUES (7, 'Java', 'Learning')")
            sql.execSQL("INSERT INTO task_table (id, title, description, subjectName, tabType, priority, isCompleted, taskDate, deadline) VALUES (11, 'Keep this task', 'My notes', 'Java', 'Learning', 'High', 0, 1000, 2000)")
            sql.execSQL("INSERT INTO time_session_table (id, taskId, startTime, endTime, duration) VALUES (13, 11, 1000, 2000, 1000)")
        } finally { db.close() }
    }

    private fun assertSavedData(query: (String) -> Cursor) {
        query("SELECT title, description, isCompleted FROM task_table WHERE id = 11").use {
            assertTrue(it.moveToFirst())
            assertEquals("Keep this task", it.getString(0))
            assertEquals("My notes", it.getString(1))
            assertEquals(0, it.getInt(2))
        }
        query("SELECT name FROM category_table WHERE id = 7").use {
            assertTrue(it.moveToFirst())
            assertEquals("Java", it.getString(0))
        }
        query("SELECT taskId, duration FROM time_session_table WHERE id = 13").use {
            assertTrue(it.moveToFirst())
            assertEquals(11, it.getInt(0))
            assertEquals(1000L, it.getLong(1))
        }
    }

    @Test fun reopeningCurrentDatabasePreservesAllThreeTables() {
        seedDatabase()
        val reopened = AppDatabase.buildDatabase(context, name)
        try {
            assertSavedData { reopened.openHelper.writableDatabase.query(it) }
        } finally { reopened.close() }
    }

    // This deliberately simulates an unknown version; it is not a v1 migration fixture.
    @Test fun missingMigrationDoesNotEraseExistingData() {
        seedDatabase()
        SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null,
            SQLiteDatabase.OPEN_READWRITE).use { it.version = 1 }
        val unsupported = AppDatabase.buildDatabase(context, name)
        try {
            unsupported.openHelper.writableDatabase
            fail("An unknown database version must not be silently reset")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message.orEmpty().contains("migration", ignoreCase = true))
        } finally { unsupported.close() }
        SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null,
            SQLiteDatabase.OPEN_READONLY).use { db ->
            assertSavedData { db.rawQuery(it, null) }
        }
    }
}
