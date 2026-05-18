package com.almamun252.perfectroutine.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.almamun252.perfectroutine.model.TimeSession

@Dao
interface TimeSessionDao {

    // টাইমার সেশন সেভ করা
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeSession(timeSession: TimeSession)

    // নির্দিষ্ট কাজের জন্য ব্যয় করা সব সময় দেখা
    @Query("SELECT * FROM time_session_table WHERE taskId = :taskId ORDER BY startTime DESC")
    fun getSessionsByTask(taskId: Int): Flow<List<TimeSession>>

    // একটি নির্দিষ্ট কাজের পেছনে মোট কতক্ষণ সময় ব্যয় হয়েছে তা বের করা
    @Query("SELECT SUM(duration) FROM time_session_table WHERE taskId = :taskId")
    fun getTotalDurationForTask(taskId: Int): Flow<Long?>

    @Delete
    suspend fun deleteTimeSession(timeSession: TimeSession)
}