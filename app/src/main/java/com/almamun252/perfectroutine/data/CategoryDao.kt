package com.almamun252.perfectroutine.data

import androidx.room.*
import com.almamun252.perfectroutine.model.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    // নতুন ক্যাটাগরি সাজেশন হিসেবে সেভ করা
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    // ড্রপডাউনে দেখানোর জন্য সব ক্যাটাগরি বা সাবজেক্টের লিস্ট
    @Query("SELECT * FROM category_table ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    // নির্দিষ্ট ট্যাবের ক্যাটাগরি সাজেশন পাওয়ার জন্য
    @Query("SELECT * FROM category_table WHERE tabType = :tabType ORDER BY name ASC")
    fun getCategoriesByTab(tabType: String): Flow<List<Category>>

    @Delete
    suspend fun deleteCategory(category: Category)
}