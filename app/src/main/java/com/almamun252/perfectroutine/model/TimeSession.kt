package com.almamun252.perfectroutine.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * এই টেবিলটি টাইমার সেশনগুলো সেভ করার জন্য।
 * যখনই ইউজার কোনো কাজ করার সময় টাইমার চালু করবে, তার হিসাব এখানে থাকবে।
 */
@Entity(tableName = "time_session_table")
data class TimeSession(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val taskId: Int,          // কোন কাজের (Task) জন্য টাইমার চালু করা হয়েছে

    val startTime: Long,      // কখন কাজ শুরু হয়েছে (মিলি-সেকেন্ডে)
    val endTime: Long = 0L,   // কখন কাজ শেষ বা পজ হয়েছে

    val duration: Long = 0L   // এই সেশনে মোট কতক্ষণ কাজ হয়েছে (মিলি-সেকেন্ডে)
)