package com.almamun252.perfectroutine.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.almamun252.perfectroutine.model.Category
import com.almamun252.perfectroutine.model.Task
import com.almamun252.perfectroutine.model.TimeSession

/**
 * অ্যাপের মূল ডেটাবেস ক্লাস।
 * এখানে আমাদের তিনটি টেবিল (Task, Category, TimeSession) রেজিস্টার করা হয়েছে।
 */
@Database(
    entities = [Task::class, Category::class, TimeSession::class],
    version = 2, // নতুন আর্কিটেকচারের জন্য ভার্সন আপডেট করা হয়েছে
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // DAO-গুলোর রেফারেন্স
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
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "perfect_routine_database"
                )
                    // নতুন স্ট্রাকচারের কারণে পুরোনো ডেটা মুছে নতুনভাবে শুরু করার জন্য
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}