package com.almamun252.perfectroutine.data

import androidx.room.*
import com.almamun252.perfectroutine.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // নতুন টাস্ক যোগ করা বা এডিট করা
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    // টাস্ক ডিলিট করা
    @Delete
    suspend fun deleteTask(task: Task)

    // সব টাস্ক পাওয়ার জন্য
    @Query("SELECT * FROM task_table ORDER BY deadline ASC")
    fun getAllTasks(): Flow<List<Task>>

    // নির্দিষ্ট ট্যাবের (যেমন: Academic বা Research) সব টাস্ক পাওয়ার জন্য
    @Query("SELECT * FROM task_table WHERE tabType = :tabType ORDER BY deadline ASC")
    fun getTasksByTab(tabType: String): Flow<List<Task>>

    // টাস্কের স্ট্যাটাস (Completed/Pending) আপডেট করার জন্য কাস্টম কুয়েরি
    @Query("UPDATE task_table SET isCompleted = :completed WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: Int, completed: Boolean)
}