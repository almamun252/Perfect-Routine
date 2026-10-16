package com.almamun252.perfectroutine.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

data class TabIconOption(val key: String, val label: String, val image: ImageVector)

// Stored keys are independent of translated labels and can safely survive renaming.
val TabIconOptions = listOf(
    TabIconOption("work", "কাজ", Icons.Rounded.Work),
    TabIconOption("school", "পড়াশোনা", Icons.Rounded.School),
    TabIconOption("book", "বই", Icons.Rounded.MenuBook),
    TabIconOption("science", "রিসার্চ", Icons.Rounded.Science),
    TabIconOption("code", "কোডিং", Icons.Rounded.Code),
    TabIconOption("fitness", "ফিটনেস", Icons.Rounded.FitnessCenter),
    TabIconOption("favorite", "নিজের জন্য", Icons.Rounded.Favorite),
    TabIconOption("lightbulb", "আইডিয়া", Icons.Rounded.Lightbulb),
    TabIconOption("payments", "হিসাব", Icons.Rounded.Payments),
    TabIconOption("travel", "ভ্রমণ", Icons.Rounded.Flight),
    TabIconOption("flash", "দ্রুত কাজ", Icons.Rounded.FlashOn),
    TabIconOption("folder", "অন্যান্য", Icons.Rounded.Folder)
)

fun tabIcon(key: String): ImageVector = TabIconOptions.firstOrNull { it.key == key }?.image ?: Icons.Rounded.Folder
