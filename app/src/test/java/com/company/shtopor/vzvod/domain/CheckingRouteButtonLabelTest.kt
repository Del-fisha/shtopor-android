package com.company.shtopor.vzvod.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CheckingRouteButtonLabelTest {
    @Test
    fun format_withoutChecks_isRouteOnly() {
        assertEquals("МП 28", CheckingRouteButtonLabel.format("МП 28", emptyList()))
        assertEquals("МП 28", CheckingRouteButtonLabel.format("МП 28", listOf(null, "")))
    }

    @Test
    fun format_accumulatesTimesWithPipes() {
        assertEquals(
            "МП 30 | 18:12",
            CheckingRouteButtonLabel.format("МП 30", listOf("18:12:00"))
        )
        assertEquals(
            "МП 30 | 18:12 | 18:14",
            CheckingRouteButtonLabel.format("МП 30", listOf("18:12", "18:14:00"))
        )
    }

    @Test
    fun toHm_normalizesToHoursMinutes() {
        assertEquals("09:05", CheckingRouteButtonLabel.toHm("9:05:00"))
        assertEquals("21:28", CheckingRouteButtonLabel.toHm("21:28"))
        assertNull(CheckingRouteButtonLabel.toHm(null))
        assertNull(CheckingRouteButtonLabel.toHm("bad"))
    }
}
