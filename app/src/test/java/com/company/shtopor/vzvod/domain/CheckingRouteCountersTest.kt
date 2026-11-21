package com.company.shtopor.vzvod.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckingRouteCountersTest {

    @Test
    fun commanderSeesOnlySpAndMp() {
        assertTrue(CheckingRouteCounters.commanderSeesRoute("МП 28"))
        assertTrue(CheckingRouteCounters.commanderSeesRoute("СП 3"))
        assertTrue(CheckingRouteCounters.commanderSeesRoute("сп 18"))
        assertFalse(CheckingRouteCounters.commanderSeesRoute("Другое"))
        assertFalse(CheckingRouteCounters.commanderSeesRoute("Another"))
        assertFalse(CheckingRouteCounters.commanderSeesRoute("Пост"))
        assertFalse(CheckingRouteCounters.commanderSeesRoute(null))
        assertFalse(CheckingRouteCounters.commanderSeesRoute("  "))
    }

    @Test
    fun lastCheckId_takesLatestNonBlank() {
        assertEquals("c-2", CheckingRouteCounters.lastCheckId(listOf("c-1", "c-2")))
        assertEquals("c-1", CheckingRouteCounters.lastCheckId(listOf("c-1", null, "")))
        assertNull(CheckingRouteCounters.lastCheckId(emptyList()))
        assertNull(CheckingRouteCounters.lastCheckId(listOf(null, "  ")))
    }

    @Test
    fun formatSummary_includesRoutesChecksApUp() {
        assertEquals(
            "Проверок 3 · маршрутов 2 · АП 1 · УП 0",
            CheckingRouteCounters.formatSummary(
                routeCount = 2,
                checkCount = 3,
                adminCount = 1,
                criminalCount = 0
            )
        )
    }
}
