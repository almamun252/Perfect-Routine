package com.almamun252.perfectroutine.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Identity stays stable when the user changes the displayed name.
@Entity(tableName = "user_tabs")
data class UserTab(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int,
    val isVisible: Boolean = true,
    val colorArgb: Long = 0xFF3B82F6,
    val isQuickList: Boolean = false
)
