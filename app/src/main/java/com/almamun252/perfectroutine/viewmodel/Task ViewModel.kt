package com.almamun252.perfectroutine.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.almamun252.perfectroutine.data.RoutineRepository
import com.almamun252.perfectroutine.model.Category
import com.almamun252.perfectroutine.model.Task
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.Deferred

/**
 * TaskViewModel মূলত UI-তে ডেটা সরবরাহ করে এবং ইউজার অ্যাকশন অনুযায়ী
 * রিপোজিটরি ব্যবহার করে ডেটাবেসে পরিবর্তন আনে।
 */
class TaskViewModel(private val repository: RoutineRepository) : ViewModel() {
    val tabs = repository.getTabs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    suspend fun saveTab(tab: com.almamun252.perfectroutine.model.UserTab) = repository.saveTab(tab)
    suspend fun moveTab(id: String, direction: Int) = repository.moveTab(id, direction)
    suspend fun deleteTab(id: String, moveTo: String?) = repository.deleteTab(id, moveTo)


    // ==========================================
    //            Task States (Flows)
    // ==========================================

    // সব কাজ পাওয়ার জন্য
    val allTasks: StateFlow<List<Task>> = repository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // লার্নিং (Academic + Extra Learning) ট্যাবের কাজগুলো
    val learningTasks: StateFlow<List<Task>> = repository.getTasksByTab("Learning")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // রিসার্চ ট্যাবের কাজগুলো
    val researchTasks: StateFlow<List<Task>> = repository.getTasksByTab("Research")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ওয়ার্ক বা কাজের ট্যাবের কাজগুলো
    val workTasks: StateFlow<List<Task>> = repository.getTasksByTab("Work")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- নতুন: Temp বা টেম্পোরারি ট্যাবের কাজগুলো ---
    val tempTasks: StateFlow<List<Task>> = repository.getTasksByTab("Temp")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ==========================================
    //          Category Suggestions
    // ==========================================

    // ড্রপডাউনে দেখানোর জন্য সব সাবজেক্ট সাজেশন
    val allCategories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // ==========================================
    //            User Operations
    // ==========================================

    // নতুন কাজ যোগ করা
    fun addTask(task: Task) {
        viewModelScope.launch { repository.insertTask(task) }
    }

    // A configuration change must not duplicate a new task if its save already committed.
    private val saveRequests = mutableMapOf<String, Deferred<Unit>>()
    suspend fun saveTask(task: Task, requestKey: String) {
        val request = saveRequests.getOrPut(requestKey) {
            viewModelScope.async { repository.insertTask(task) }
        }
        try { request.await() }
        catch (error: Exception) {
            if (request.isCancelled) saveRequests.remove(requestKey)
            throw error
        }
    }

    suspend fun deleteCategory(category: Category) { repository.deleteCategory(category) }

    // কাজ ডিলিট করা
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    // কাজের স্ট্যাটাস (পেন্ডিং/সম্পন্ন) আপডেট করা
    fun updateTaskStatus(task: Task, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.updateTaskStatus(task.id, isCompleted)
        }
    }

    // টেম্পোরারি টাস্কগুলোর সিরিয়াল (Position) আপডেট করা
    fun updateTasksOrder(tasks: List<Task>) {
        viewModelScope.launch {
            tasks.forEachIndexed { index, task ->
                // ড্র্যাগ এন্ড ড্রপের সিরিয়াল ঠিক রাখতে taskDate কাস্টমাইজ করে আপডেট করা হচ্ছে
                val updatedTask = task.copy(taskDate = System.currentTimeMillis() - (index * 1000L))
                repository.insertTask(updatedTask) // REPLACE Conflict Strategy থাকায় এটি আপডেট হয়ে যাবে
            }
        }
    }

    // মোট কাজের সময় বের করা
    fun getTotalTimeSpentFlow(taskId: Int) = repository.getTotalDuration(taskId)

}