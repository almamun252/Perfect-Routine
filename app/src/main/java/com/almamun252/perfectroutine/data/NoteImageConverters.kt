package com.almamun252.perfectroutine.data

import androidx.room.TypeConverter
import org.json.JSONArray

class NoteImageConverters {
    @TypeConverter
    fun encode(paths: List<String>): String = JSONArray(paths).toString()

    @TypeConverter
    fun decode(value: String): List<String> {
        val array = JSONArray(value)
        return List(array.length()) { array.getString(it) }
    }
}
