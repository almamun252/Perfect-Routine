package com.almamun252.perfectroutine.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * এই টেবিলটি শুধু ড্রপডাউনে সাজেশন (Suggestion) দেখানোর জন্য ব্যবহার করা হবে।
 * ইউজার যখন কোনো নতুন সাবজেক্ট টাইপ করবে, সেটি এখানে সেভ হবে যাতে পরবর্তীতে
 * ড্রপডাউন থেকে সহজেই সেটি সিলেক্ট করা যায়।
 */
@Entity(tableName = "category_table")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,     // সাবজেক্ট বা টপিকের নাম (যেমন: Physics, AI Research, Java)
    val tabType: String   // এটি কোন ট্যাবের জন্য (যেমন: Learning, Research, Work)
)