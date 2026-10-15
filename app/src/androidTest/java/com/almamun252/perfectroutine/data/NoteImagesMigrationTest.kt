package com.almamun252.perfectroutine.data

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class NoteImagesMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "note-migration-${UUID.randomUUID()}.db"
    @After fun cleanup() { context.deleteDatabase(name) }

    // The v2 tables are copied from the released entities, before imagePaths existed.
    @Test fun versionTwoUpgradePreservesNotesCategoriesAndSessions() = runBlocking {
        val path = context.getDatabasePath(name)
        path.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            db.execSQL("CREATE TABLE task_table (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, description TEXT, subjectName TEXT NOT NULL, tabType TEXT NOT NULL, priority TEXT NOT NULL, isCompleted INTEGER NOT NULL, taskDate INTEGER NOT NULL, deadline INTEGER)")
            db.execSQL("CREATE TABLE category_table (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, tabType TEXT NOT NULL)")
            db.execSQL("CREATE TABLE time_session_table (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, taskId INTEGER NOT NULL, startTime INTEGER NOT NULL, endTime INTEGER NOT NULL, duration INTEGER NOT NULL)")
            db.execSQL("INSERT INTO task_table VALUES (11, 'Class note', 'Keep my text', 'Java', 'Learning', 'High', 0, 1000, 2000)")
            db.execSQL("INSERT INTO task_table VALUES (12, 'Done task', NULL, 'Java', 'Learning', 'Low', 1, 1000, NULL)")
            db.execSQL("INSERT INTO category_table VALUES (7, 'Java', 'Learning')")
            db.execSQL("INSERT INTO time_session_table VALUES (13, 11, 1000, 2000, 1000)")
            db.version = 2
        }
        val db = AppDatabase.buildDatabase(context, name)
        try {
            val old = db.taskDao().getTaskById(11)!!
            assertEquals("Keep my text", old.description)
            assertEquals(2000L, old.deadline)
            assertTrue(old.imagePaths.isEmpty())
            assertTrue(db.taskDao().getTaskById(12)!!.isCompleted)
            assertNull(db.taskDao().getTaskById(12)!!.deadline)
            db.openHelper.writableDatabase.query("SELECT name FROM category_table WHERE id = 7").use {
                assertTrue(it.moveToFirst()); assertEquals("Java", it.getString(0))
            }
            db.openHelper.writableDatabase.query("SELECT duration FROM time_session_table WHERE id = 13").use {
                assertTrue(it.moveToFirst()); assertEquals(1000L, it.getLong(0))
            }
            val paths = listOf("note_images/a.img", "note_images/b.img")
            db.taskDao().insertTask(old.copy(description = null, imagePaths = paths))
            assertEquals(paths, db.taskDao().getTaskById(11)!!.imagePaths)
            db.categoryDao().ensureCategory("java", "Learning")
            db.categoryDao().deleteByName("Java", "Learning")
            db.openHelper.writableDatabase.query("SELECT COUNT(*) FROM category_table").use {
                assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0))
            }
            assertEquals(paths, db.taskDao().getTaskById(11)!!.imagePaths)
        } finally { db.close() }
        val reopened = AppDatabase.buildDatabase(context, name)
        try { assertEquals(2, reopened.taskDao().getTaskById(11)!!.imagePaths.size) }
        finally { reopened.close() }
    }
}
