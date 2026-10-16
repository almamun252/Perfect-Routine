package com.almamun252.perfectroutine.data

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.almamun252.perfectroutine.model.Task
import com.almamun252.perfectroutine.model.TimeSession
import com.almamun252.perfectroutine.model.UserTab
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class UserTabsTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "user-tabs-${UUID.randomUUID()}.db"
    private var opened: AppDatabase? = null
    @After fun cleanup() { opened?.close(); context.deleteDatabase(name) }

    private fun database(): AppDatabase = AppDatabase.buildDatabase(context, name).also { opened = it }
    private fun repository(db: AppDatabase) = RoutineRepository(context, db.taskDao(), db.categoryDao(), db.timeSessionDao(), db)
    private fun task(tab: String) = Task(title = "Keep note", subjectName = "Java", tabType = tab,
        description = "Keep text", priority = "High", taskDate = 1000L,
        imagePaths = listOf("note_images/abcd.img"))

    @Test fun newInstallHasNoPredefinedTabs() = runBlocking {
        assertTrue(database().userTabDao().getAll().isEmpty())
    }

    @Test fun renameHideAndReorderKeepStableTaskMembership() = runBlocking {
        val db = database()
        val repo = repository(db)
        repo.saveTab(UserTab("", "University", 0))
        repo.saveTab(UserTab("", "Personal", 0))
        val tab = db.userTabDao().getAll().first()
        val taskId = db.taskDao().insertTask(task(tab.id)).toInt()
        repo.saveTab(tab.copy(name = "বিশ্ববিদ্যালয়", isVisible = false))
        repo.moveTab(tab.id, 1)
        assertEquals(tab.id, db.taskDao().getTaskById(taskId)!!.tabType)
        assertEquals("বিশ্ববিদ্যালয়", db.userTabDao().getById(tab.id)!!.name)
        assertFalse(db.userTabDao().getById(tab.id)!!.isVisible)
        assertEquals(tab.id, db.userTabDao().getAll().last().id)
        try {
            repo.saveTab(UserTab("", "PERSONAL", 0))
            fail("Duplicate names must be rejected")
        } catch (_: IllegalArgumentException) { }
        assertEquals(2, db.userTabDao().getAll().size)
    }

    @Test fun populatedTabRequiresDestinationAndMovesWithoutDeletingHistory() = runBlocking {
        val db = database()
        val repo = repository(db)
        repo.saveTab(UserTab("", "Source", 0))
        repo.saveTab(UserTab("", "Destination", 0))
        val source = db.userTabDao().getAll().first()
        val target = db.userTabDao().getAll().last()
        val id = db.taskDao().insertTask(task(source.id)).toInt()
        db.categoryDao().ensureCategory("Java", source.id)
        db.timeSessionDao().insertTimeSession(TimeSession(taskId = id, startTime = 1000, duration = 60000))
        try {
            repo.deleteTab(source.id, null)
            fail("A populated tab cannot be deleted without a destination")
        } catch (_: IllegalArgumentException) { }
        assertNotNull(db.userTabDao().getById(source.id))
        repo.deleteTab(source.id, target.id)
        assertNull(db.userTabDao().getById(source.id))
        val moved = db.taskDao().getTaskById(id)!!
        assertEquals(target.id, moved.tabType)
        assertEquals("Keep text", moved.description)
        assertEquals(listOf("note_images/abcd.img"), moved.imagePaths)
        assertEquals(60000L, db.timeSessionDao().getTotalDurationForTask(id).first())
        assertEquals(target.id, db.categoryDao().getAllCategories().first().single().tabType)
    }

    @Test fun upgradeFromExportedVersionThreePreservesLegacyAndUnknownTabKeys() = runBlocking {
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
            sql.execSQL("INSERT INTO task_table VALUES (11, 'Old note', 'Keep text', 'Java', 'Learning', 'High', 0, 1000, 2000, '[]')")
            sql.execSQL("INSERT INTO task_table VALUES (12, 'Other note', NULL, 'Topic', 'Custom / বাংলা', 'Low', 1, 1000, NULL, '[]')")
            sql.execSQL("INSERT INTO category_table VALUES (7, 'Legacy', 'Category-only')")
            sql.execSQL("INSERT INTO time_session_table VALUES (13, 11, 1000, 2000, 1000)")
            sql.version = 3
        }
        val db = database()
        val keys = db.userTabDao().getAll().map { it.id }
        assertTrue(keys.containsAll(listOf("Learning", "Research", "Work", "Temp", "Custom / বাংলা", "Category-only")))
        assertEquals("Keep text", db.taskDao().getTaskById(11)!!.description)
        assertEquals("Custom / বাংলা", db.taskDao().getTaskById(12)!!.tabType)
        assertTrue(db.userTabDao().getById("Temp")!!.isQuickList)
        assertEquals(1000L, db.timeSessionDao().getTotalDurationForTask(11).first())
    }
}
