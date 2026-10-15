package com.almamun252.perfectroutine.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.almamun252.perfectroutine.widget.RoutineTaskWidget
import com.almamun252.perfectroutine.worker.TaskReminderScheduler
import com.almamun252.perfectroutine.model.Category
import com.almamun252.perfectroutine.model.Task
import kotlinx.coroutines.flow.Flow
import com.almamun252.perfectroutine.model.TimeSession
/**
 * এই ক্লাসটি ViewModel এবং Room Database-এর মাঝখানে একটি ব্রিজ হিসেবে কাজ করে।
 * এটি DAO-এর মাধ্যমে ডেটাবেসে ডেটা আদান-প্রদান করে।
 */
class RoutineRepository(
    private val context: Context,
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao,
    private val timeSessionDao: TimeSessionDao
) {

    private val imageWriteMutex = Mutex()

    // ==========================================
    //            Task Operations
    // ==========================================

    // সব কাজ পাওয়ার জন্য (লিস্ট আকারে)
    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()

    // নির্দিষ্ট ট্যাবের (যেমন: Learning বা Work) কাজ পাওয়ার জন্য
    fun getTasksByTab(tabType: String): Flow<List<Task>> = taskDao.getTasksByTab(tabType)

    // নতুন কাজ যোগ করা বা এডিট করা
    suspend fun insertTask(task: Task) = withContext(Dispatchers.IO + NonCancellable) {
        imageWriteMutex.withLock {
            val previous = if (task.id != 0) taskDao.getTaskById(task.id) else null
            val images = NoteImageStore.prepare(context, task.imagePaths)
            val id = try {
                taskDao.insertTask(task.copy(imagePaths = images.paths)).toInt()
            } catch (error: Throwable) {
                NoteImageStore.delete(context, images.created)
                throw error
            }
            // Editing an existing task must not restore a category the user deliberately removed.
            if (previous == null || previous.subjectName != task.subjectName || previous.tabType != task.tabType) {
                try { categoryDao.ensureCategory(task.subjectName, task.tabType) }
                catch (error: Exception) { Log.e("RoutineRepository", "Category suggestion save failed", error) }
            }
            NoteImageStore.delete(context, images.drafts)
            try { removeUnreferencedImages(previous?.imagePaths.orEmpty() - images.paths.toSet()) }
            catch (error: Exception) { Log.e("RoutineRepository", "Old image cleanup deferred", error) }
            refreshTask(id)
        }
    }

    suspend fun deleteTask(task: Task) = withContext(Dispatchers.IO + NonCancellable) {
        imageWriteMutex.withLock {
            val current = taskDao.getTaskById(task.id)
            taskDao.deleteTask(task)
            try { removeUnreferencedImages(current?.imagePaths.orEmpty()) }
            catch (error: Exception) { Log.e("RoutineRepository", "Deleted task image cleanup deferred", error) }
            refreshTask(task.id)
        }
    }

    private suspend fun removeUnreferencedImages(paths: List<String>) {
        if (paths.isEmpty()) return
        val used = taskDao.getAllTasks().first().flatMap { it.imagePaths }.toSet()
        NoteImageStore.delete(context, paths.filter { it !in used })
    }

    private suspend fun refreshTask(id: Int) {
        // A widget/alarm refresh failure must not turn a successfully saved note into a failed save.
        try { TaskReminderScheduler.reconcileTask(context, id) }
        catch (error: Exception) {
            Log.e("RoutineRepository", "Reminder refresh failed", error)
            try { TaskReminderScheduler.requestReconcile(context) }
            catch (retryError: Exception) { Log.e("RoutineRepository", "Reminder retry unavailable", retryError) }
        }
        try { RoutineTaskWidget.refreshAll(context) }
        catch (error: Exception) { Log.e("RoutineRepository", "Widget refresh failed", error) }
    }

    // কাজের স্ট্যাটাস আপডেট করা (যেমন: পেন্ডিং থেকে সম্পন্ন)
    suspend fun updateTaskStatus(taskId: Int, completed: Boolean) {
        taskDao.updateTaskStatus(taskId, completed)
        TaskReminderScheduler.reconcileTask(context, taskId)
        RoutineTaskWidget.refreshAll(context)
    }


    // ==========================================
    //         Category Suggestions
    // ==========================================

    // ড্রপডাউনে দেখানোর জন্য সব সাবজেক্ট বা স্কিল সাজেশন
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    // নির্দিষ্ট ট্যাবের সাজেশন (যেমন শুধু পড়াশোনার সাবজেক্টগুলো)
    fun getCategoriesByTab(tabType: String): Flow<List<Category>> = categoryDao.getCategoriesByTab(tabType)

    // নতুন সাবজেক্ট সাজেশন হিসেবে সেভ করা
    suspend fun insertCategory(category: Category) = categoryDao.ensureCategory(category.name, category.tabType)

    suspend fun deleteCategory(category: Category) = categoryDao.deleteByName(category.name, category.tabType)


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
