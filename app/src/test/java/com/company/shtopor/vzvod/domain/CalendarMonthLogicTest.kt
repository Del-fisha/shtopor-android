package com.company.shtopor.vzvod.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarMonthLogicTest {

    @Test
    fun grid_startsOnMonday_andCoversFullWeeks() {
        // август 2026: 1-е — суббота
        val cells = CalendarMonthLogic.cells(YearMonth.of(2026, 8))
        assertEquals(0, cells.size % 7)
        assertTrue(cells.first().date <= LocalDate.of(2026, 8, 1))
        assertTrue(cells.last().date >= LocalDate.of(2026, 8, 31))
        assertEquals(LocalDate.of(2026, 8, 1), cells.first { it.inMonth }.date)
    }

    @Test
    fun department_alternatesInBlocksOfTwo() {
        val d1 = LocalDate.of(2026, 2, 22)
        assertEquals(1, CalendarMonthLogic.department(d1))
        assertEquals(1, CalendarMonthLogic.department(d1.plusDays(1)))
        assertEquals(2, CalendarMonthLogic.department(d1.plusDays(2)))
        assertEquals(2, CalendarMonthLogic.department(d1.plusDays(3)))
        assertEquals(1, CalendarMonthLogic.department(d1.plusDays(4)))
    }

    @Test
    fun canShowShiftSummary_onlyTodayAndPast() {
        val today = LocalDate.of(2026, 8, 10)
        assertTrue(CalendarMonthLogic.canShowShiftSummary(today, today))
        assertTrue(CalendarMonthLogic.canShowShiftSummary(today.minusDays(1), today))
        assertFalse(CalendarMonthLogic.canShowShiftSummary(today.plusDays(1), today))
    }

    @Test
    fun eventDates_markMatchingDays() {
        val marked = CalendarMonthLogic.eventDatesOn(
            listOf("2026-08-10", "2026-08-10", "2026-08-15", null)
        )
        assertEquals(setOf(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 15)), marked)
    }
}
