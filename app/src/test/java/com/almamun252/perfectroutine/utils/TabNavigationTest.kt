package com.almamun252.perfectroutine.utils

import com.almamun252.perfectroutine.model.UserTab
import org.junit.Assert.*
import org.junit.Test

class TabNavigationTest {
    private fun tabs(count: Int) = (0 until count).map { UserTab("tab-$it", "Tab $it", it) }

    @Test fun homeIsAlwaysInThePhysicalCenterAndProfileIsLast() {
        for (count in 0..20) {
            val slots = TabNavigation.slots(tabs(count))
            assertTrue(slots.size == 3 || slots.size == 5)
            assertEquals(TabNavigation.HOME, slots[slots.size / 2])
            assertEquals(TabNavigation.PROFILE, slots.last())
            assertEquals(slots.size, slots.distinct().size)
        }
    }

    @Test fun firstInstallUsesWorkHomeProfile() {
        assertEquals(listOf("Work", TabNavigation.HOME, TabNavigation.PROFILE),
            TabNavigation.slots(listOf(UserTab("Work", "Work", 0))))
    }

    @Test fun hiddenTabsNeverOccupyASlotAndOverflowHasAnEntryPoint() {
        val data = tabs(6).map { if (it.id == "tab-0") it.copy(isVisible = false) else it }
        val slots = TabNavigation.slots(data)
        assertFalse(slots.contains("tab-0"))
        assertTrue(slots.contains(TabNavigation.ALL_TABS))
        assertEquals("tab-1", slots.first())
    }

    @Test fun reorderingAndRenamingDoNotChangeHomePositionOrTabIdentity() {
        val data = tabs(3).map { it.copy(name = "Renamed", sortOrder = -it.sortOrder) }
        assertEquals(listOf("tab-2", "tab-1", TabNavigation.HOME, "tab-0", TabNavigation.PROFILE), TabNavigation.slots(data))
    }
}
