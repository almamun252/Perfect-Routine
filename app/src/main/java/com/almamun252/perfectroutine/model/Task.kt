package com.almamun252.perfectroutine.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * এটি অ্যাপের প্রধান টেবিল।
 * নতুন "Quick Add" লজিক অনুযায়ী সব কাজ সরাসরি এই এক টেবিলেই জমা থাকবে।
 */
@Entity(tableName = "task_table")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,              // কাজের নাম (যেমন: Assignment 1)
    val description: String? = null,// বিস্তারিত নোট (ঐচ্ছিক)

    val subjectName: String,        // সাবজেক্ট বা ক্যাটাগরির নাম (যেমন: Physics)
    val tabType: String,            // এটি কোন ট্যাবে দেখাবে (Learning, Research, Work ইত্যাদি)

    val priority: String,           // গুরুত্ব (High, Medium, Low)
    val isCompleted: Boolean = false, // কাজটি সম্পন্ন হয়েছে কিনা (Boolean স্ট্যাটাস)

    val taskDate: Long,             // কাজ তৈরির তারিখ (Timestamp)
    val deadline: Long? = null      // কাজ শেষ করার সময় (ডেডলাইন - TimePicker থেকে আসবে)
)