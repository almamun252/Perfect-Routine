package com.almamun252.perfectroutine.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    // Existing tasks, notes, categories and timer sessions remain in their original tables.
    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE task_table ADD COLUMN imagePaths TEXT NOT NULL DEFAULT '[]'")
        }
    }
    // Upgrades from v3 retain all legacy tabs; fresh-install defaults are seeded in AppDatabase.
    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS user_tabs (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, sortOrder INTEGER NOT NULL, isVisible INTEGER NOT NULL, colorArgb INTEGER NOT NULL, isQuickList INTEGER NOT NULL)")
            val legacy = listOf("Learning", "Research", "Work", "Temp")
            val colors = listOf(0xFF3B82F6L, 0xFF8B5CF6L, 0xFFF59E0BL, 0xFFEF4444L)
            legacy.forEachIndexed { index, id ->
                db.execSQL("INSERT INTO user_tabs VALUES (?, ?, ?, 1, ?, ?)",
                    arrayOf<Any>(id, id, index, colors[index], if (id == "Temp") 1 else 0))
            }
            // Preserve every historic/custom tab key, including category-only keys.
            db.query("SELECT tabType FROM task_table UNION SELECT tabType FROM category_table ORDER BY tabType").use { cursor ->
                var order = legacy.size
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    if (id !in legacy) db.execSQL("INSERT INTO user_tabs VALUES (?, ?, ?, 1, ?, 0)",
                        arrayOf<Any>(id, id, order++, 0xFF3B82F6L))
                }
            }
        }
    }
    val MIGRATION_4_5: Migration = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE user_tabs ADD COLUMN iconKey TEXT NOT NULL DEFAULT 'folder'")
            db.execSQL("UPDATE user_tabs SET iconKey = CASE id WHEN 'Learning' THEN 'school' WHEN 'Research' THEN 'science' WHEN 'Work' THEN 'work' WHEN 'Temp' THEN 'flash' ELSE CASE WHEN isQuickList = 1 THEN 'flash' ELSE 'folder' END END")
        }
    }
    val MIGRATION_5_6: Migration = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE task_table ADD COLUMN startDate INTEGER DEFAULT NULL")
            db.execSQL("ALTER TABLE task_table ADD COLUMN sortOrder INTEGER DEFAULT NULL")
            db.execSQL("UPDATE task_table SET startDate = taskDate, sortOrder = taskDate")
        }
    }
    val ALL: Array<Migration> = arrayOf(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
}
