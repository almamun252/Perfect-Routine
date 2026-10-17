package com.almamun252.perfectroutine.data

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.almamun252.perfectroutine.model.Task
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class TaskScheduleMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "schedule-${UUID.randomUUID()}.db"
    private var opened: AppDatabase? = null
    @After fun cleanup() { opened?.close(); context.deleteDatabase(name) }
    private fun db() = AppDatabase.buildDatabase(context, name).also { opened = it }
    private fun repo(db: AppDatabase) = RoutineRepository(context, db.taskDao(), db.categoryDao(), db.timeSessionDao(), db)

    @Test fun versionFiveUpgradePreservesDataAndInitializesStartAndOrder() = runBlocking {
        val schema = InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.almamun252.perfectroutine.data.AppDatabase/3.json")
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val path = context.getDatabasePath(name)
        path.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { sql ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                sql.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
            }
            sql.execSQL("CREATE TABLE user_tabs (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, sortOrder INTEGER NOT NULL, isVisible INTEGER NOT NULL, colorArgb INTEGER NOT NULL, isQuickList INTEGER NOT NULL, iconKey TEXT NOT NULL DEFAULT 'folder')")
            sql.execSQL("INSERT INTO user_tabs VALUES ('Work','Projects',0,1,4282804441,0,'code')")
            sql.execSQL("INSERT INTO task_table VALUES (11,'Note','Keep note','Java','Work','High',0,1000,2000,'[]')")
            sql.execSQL("INSERT INTO time_session_table VALUES (13,11,1000,2000,1000)")
            sql.version = 5
        }
        val db = db()
        val task = db.taskDao().getTaskById(11)!!
        assertEquals(1000L, task.startDate!!)
        assertEquals(1000L, task.sortOrder!!)
        assertEquals(1000L, task.taskDate)
        assertEquals(2000L, task.deadline!!)
        assertEquals("Keep note", task.description)
        assertEquals("code", db.userTabDao().getById("Work")!!.iconKey)
        db.openHelper.readableDatabase.query("SELECT duration FROM time_session_table WHERE id=13").use {
            assertTrue(it.moveToFirst()); assertEquals(1000L, it.getLong(0))
        }
    }

    @Test fun reorderingCannotOverwriteNewerTaskEditsOrItsStartTime() = runBlocking {
        val db = db()
        val original = Task(title = "Original", subjectName = "Java", tabType = "Work", priority = "High",
            taskDate = 1000, deadline = 5000, startDate = 3000)
        val id = db.taskDao().insertTask(original).toInt()
        val stale = original.copy(id = id)
        db.taskDao().insertTask(stale.copy(title = "Edited", isCompleted = true))
        repo(db).updateTasksOrder(listOf(stale))
        val after = db.taskDao().getTaskById(id)!!
        assertEquals("Edited", after.title)
        assertTrue(after.isCompleted)
        assertEquals(1000L, after.taskDate)
        assertEquals(3000L, after.startDate!!)
        assertEquals(5000L, after.deadline!!)
    }

    @Test fun invalidNewScheduleIsRejectedBeforeDatabaseWrite() = runBlocking {
        val db = db()
        try {
            repo(db).insertTask(Task(title = "Invalid", subjectName = "Java", tabType = "Work", priority = "Low",
                taskDate = 1000, startDate = 3000, deadline = 2000))
            fail("Deadline before start must be rejected")
        } catch (_: IllegalArgumentException) { }
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM task_table").use {
            assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0))
        }
    }
}
