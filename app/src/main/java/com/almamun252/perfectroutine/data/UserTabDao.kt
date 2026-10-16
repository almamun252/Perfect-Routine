package com.almamun252.perfectroutine.data

import androidx.room.*
import com.almamun252.perfectroutine.model.UserTab
import kotlinx.coroutines.flow.Flow

@Dao
interface UserTabDao {
    @Query("SELECT * FROM user_tabs ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<UserTab>>

    @Query("SELECT * FROM user_tabs ORDER BY sortOrder, id")
    suspend fun getAll(): List<UserTab>

    @Query("SELECT * FROM user_tabs WHERE id = :id")
    suspend fun getById(id: String): UserTab?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(tab: UserTab)

    @Update
    suspend fun update(tab: UserTab)

    @Query("DELETE FROM user_tabs WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM task_table WHERE tabType = :id")
    suspend fun taskCount(id: String): Int

    @Query("UPDATE task_table SET tabType = :target WHERE tabType = :source")
    suspend fun moveTasks(source: String, target: String)

    @Query("UPDATE category_table SET tabType = :target WHERE tabType = :source")
    suspend fun moveCategories(source: String, target: String)

    @Query("DELETE FROM category_table WHERE tabType = :id")
    suspend fun deleteCategories(id: String)
}
