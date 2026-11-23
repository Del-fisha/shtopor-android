package com.company.shtopor.vzvod.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellHistoryTest {

    @Test
    fun backReturnsToPreviousAreaAndTab() {
        val start = ShellHistory()
        assertFalse(start.canGoBack())

        val calendar = start.selectTab(AppSections.TAB_CALENDAR)
        val game = calendar.openArea(AppSections.GAME, null)
        assertEquals(AppSections.GAME, game.areaId)
        assertTrue(game.canGoBack())

        val backToCalendar = game.back()
        assertEquals(AppSections.SERVICE, backToCalendar.areaId)
        assertEquals(AppSections.TAB_CALENDAR, backToCalendar.tabId)

        val root = backToCalendar.back()
        assertEquals(AppSections.TAB_SHIFTS, root.tabId)
        assertFalse(root.canGoBack())
        assertEquals(root, root.back())
    }

    @Test
    fun samePlaceIsNotPushedAgain() {
        val start = ShellHistory()
        assertEquals(start, start.selectTab(AppSections.TAB_SHIFTS))
        assertEquals(start, start.openArea(AppSections.SERVICE, AppSections.TAB_SHIFTS))
    }
}
