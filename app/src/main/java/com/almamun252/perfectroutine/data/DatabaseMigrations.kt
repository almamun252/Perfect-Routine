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
    val ALL: Array<Migration> = arrayOf(MIGRATION_2_3)
}
