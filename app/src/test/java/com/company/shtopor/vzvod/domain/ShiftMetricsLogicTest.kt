package com.company.shtopor.vzvod.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftMetricsLogicTest {

    @Test
    fun applyDelta_plus_increments() {
        assertEquals(3, ShiftMetricsLogic.applyDelta(2, 1))
    }

    @Test
    fun applyDelta_minus_decrements() {
        assertEquals(1, ShiftMetricsLogic.applyDelta(2, -1))
    }

    @Test
    fun applyDelta_minus_clampsAtZero() {
        assertEquals(0, ShiftMetricsLogic.applyDelta(0, -1))
    }

    @Test
    fun applyIbdrDelta_onlyChangesIbdr() {
        val next = ShiftMetricsLogic.applyIbdrDelta(ibdr = 2, migrant = 5, delta = 1)
        assertEquals(3, next.ibdr)
        assertEquals(5, next.migrant)
    }

    @Test
    fun applyMigrantDelta_onlyChangesMigrant() {
        val next = ShiftMetricsLogic.applyMigrantDelta(ibdr = 2, migrant = 5, delta = -1)
        assertEquals(2, next.ibdr)
        assertEquals(4, next.migrant)
    }

    @Test
    fun applyMigrantAndIbdrDelta_plus_incrementsBoth() {
        val next = ShiftMetricsLogic.applyMigrantAndIbdrDelta(ibdr = 2, migrant = 1, delta = 1)
        assertEquals(3, next.ibdr)
        assertEquals(2, next.migrant)
    }

    @Test
    fun applyMigrantAndIbdrDelta_minus_clampsBothAtZero() {
        val next = ShiftMetricsLogic.applyMigrantAndIbdrDelta(ibdr = 0, migrant = 0, delta = -1)
        assertEquals(0, next.ibdr)
        assertEquals(0, next.migrant)
    }

    @Test
    fun formatSummary_showsBothCounters() {
        val text = ShiftMetricsLogic.formatSummary(12, 3, 1, 0, 1, 0)
        assertEquals("ИБДР 12 · мигрант 3 · заявки 1 · претензии 0 · АП 1 · УП 0", text)
    }
}
