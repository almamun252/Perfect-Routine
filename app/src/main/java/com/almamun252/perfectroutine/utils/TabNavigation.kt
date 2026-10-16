package com.almamun252.perfectroutine.utils

import com.almamun252.perfectroutine.model.UserTab

object TabNavigation {
    const val HOME = "__home__"
    const val PROFILE = "__profile__"
    const val ALL_TABS = "__tabs__"

    // Always use an odd number of slots so Home stays in the physical center.
    fun slots(tabs: List<UserTab>): List<String> {
        val visible = tabs.filter { it.isVisible }.sortedBy { it.sortOrder }
        return when (visible.size) {
            0 -> listOf(ALL_TABS, HOME, PROFILE)
            1 -> listOf(visible.first().id, HOME, PROFILE)
            3 -> listOf(visible[0].id, visible[1].id, HOME, visible[2].id, PROFILE)
            else -> listOf(visible[0].id, visible[1].id, HOME, ALL_TABS, PROFILE)
        }
    }
}
