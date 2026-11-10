package com.company.shtopor.vzvod.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarMonthSwipeTest {

    @Test
    fun dragLeft_goesToNextMonth() {
        assertEquals(1, CalendarMonthSwipe.monthDeltaForDrag(-100f))
    }

    @Test
    fun dragRight_goesToPreviousMonth() {
        assertEquals(-1, CalendarMonthSwipe.monthDeltaForDrag(100f))
    }

    @Test
    fun smallDrag_ignored() {
        assertEquals(0, CalendarMonthSwipe.monthDeltaForDrag(20f))
        assertEquals(0, CalendarMonthSwipe.monthDeltaForDrag(-20f))
    }
}
