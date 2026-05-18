package com.almamun252.perfectroutine.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeFormatter {

    /**
     * টাইমারের মিলি-সেকেন্ডকে ঘণ্টা, মিনিট ও সেকেন্ডে কনভার্ট করবে।
     * যদি ১ ঘণ্টার কম হয়, তাহলে 00:00 (মিনিট:সেকেন্ড) ফরম্যাটে দেখাবে।
     * ১ ঘণ্টার বেশি হলে 00:00:00 (ঘণ্টা:মিনিট:সেকেন্ড) ফরম্যাটে দেখাবে।
     */
    fun formatTimer(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * ডেটাবেসের Timestamp (Long) কে শুধু তারিখে কনভার্ট করবে।
     * উদাহরণ: 10 Apr 2026
     */
    fun formatToDate(timestamp: Long): String {
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    /**
     * ডেটাবেসের Timestamp (Long) কে শুধু সময়ে কনভার্ট করবে।
     * উদাহরণ: 10:30 AM
     */
    fun formatToTime(timestamp: Long): String {
        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    /**
     * তারিখ ও সময় একসাথে দেখানোর জন্য।
     * উদাহরণ: 10 Apr 2026, 10:30 AM
     */
    fun formatToDateTime(timestamp: Long): String {
        val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }
}