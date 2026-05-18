package com.almamun252.perfectroutine.data

import com.almamun252.perfectroutine.model.Category
import com.almamun252.perfectroutine.model.Task
import kotlinx.coroutines.flow.Flow
import com.almamun252.perfectroutine.model.TimeSession
/**
 * এই ক্লাসটি ViewModel এবং Room Database-এর মাঝখানে একটি ব্রিজ হিসেবে কাজ করে।
 * এটি DAO-এর মাধ্যমে ডেটাবেসে ডেটা আদান-প্রদান করে।
 */
class RoutineRepository(
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao,
    private val timeSessionDao: TimeSessionDao
) {

    // ==========================================
    //            Task Operations
    // ==========================================

    // সব কাজ পাওয়ার জন্য (লিস্ট আকারে)
    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()

    // নির্দিষ্ট ট্যাবের (যেমন: Learning বা Work) কাজ পাওয়ার জন্য
    fun getTasksByTab(tabType: String): Flow<List<Task>> = taskDao.getTasksByTab(tabType)

    // নতুন কাজ যোগ করা বা এডিট করা
    suspend fun insertTask(task: Task) = taskDao.insertTask(task)

    // কাজ ডিলিট করা
    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    // কাজের স্ট্যাটাস আপডেট করা (যেমন: পেন্ডিং থেকে সম্পন্ন)
    suspend fun updateTaskStatus(taskId: Int, completed: Boolean) {
        taskDao.updateTaskStatus(taskId, completed)
    }


    // ==========================================
    //         Category Suggestions
    // ==========================================

    // ড্রপডাউনে দেখানোর জন্য সব সাবজেক্ট বা স্কিল সাজেশন
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    // নির্দিষ্ট ট্যাবের সাজেশন (যেমন শুধু পড়াশোনার সাবজেক্টগুলো)
    fun getCategoriesByTab(tabType: String): Flow<List<Category>> = categoryDao.getCategoriesByTab(tabType)

    // নতুন সাবজেক্ট সাজেশন হিসেবে সেভ করা
    suspend fun insertCategory(category: Category) = categoryDao.insertCategory(category)


    // ==========================================
    //         Time Session Operations
    // ==========================================

    // টাইমার সেশন সেভ করা
    suspend fun insertTimeSession(session: TimeSession) = timeSessionDao.insertTimeSession(session)

    // একটি কাজের সব টাইমার সেশন দেখা
    fun getSessionsForTask(taskId: Int): Flow<List<TimeSession>> = timeSessionDao.getSessionsByTask(taskId)

    // একটি কাজের পেছনে মোট কতক্ষণ সময় ব্যয় হয়েছে তা বের করা
    fun getTotalDuration(taskId: Int): Flow<Long?> = timeSessionDao.getTotalDurationForTask(taskId)
}