package com.almamun252.perfectroutine.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.almamun252.perfectroutine.model.Category
import com.almamun252.perfectroutine.model.UserTab
import com.almamun252.perfectroutine.model.Task
import com.almamun252.perfectroutine.model.TimeSession

/**
 * অ্যাপের মূল ডেটাবেস ক্লাস।
 * এখানে আমাদের চারটি টেবিল (Task, Category, TimeSession, UserTab) রেজিস্টার করা হয়েছে।
 */
@Database(
    entities = [Task::class, Category::class, TimeSession::class, UserTab::class],
    version = 5,
    exportSchema = true
)
@TypeConverters(NoteImageConverters::class)
abstract class AppDatabase : RoomDatabase() {

    // DAO-গুলোর রেফারেন্স
    abstract fun userTabDao(): UserTabDao

    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun timeSessionDao(): TimeSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * ডেটাবেস ইনস্ট্যান্স তৈরি করার জন্য Singleton প্যাটার্ন।
         * এটি নিশ্চিত করে যে পুরো অ্যাপে ডেটাবেসের শুধুমাত্র একটি কানেকশনই থাকবে।
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = buildDatabase(context, "perfect_routine_database")
                INSTANCE = instance
                instance
            }
        }

        // Unknown versions must never silently erase a user's saved data.
        internal fun buildDatabase(context: Context, name: String): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, name)
                .addMigrations(*DatabaseMigrations.ALL)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed once, only for a new database. Deleting Work never recreates it.
                        db.execSQL("INSERT INTO user_tabs (id, name, sortOrder, isVisible, colorArgb, isQuickList, iconKey) VALUES ('Work', 'Work', 0, 1, 4282804441, 0, 'work')")
                    }
                })
                .build()
    }
}
