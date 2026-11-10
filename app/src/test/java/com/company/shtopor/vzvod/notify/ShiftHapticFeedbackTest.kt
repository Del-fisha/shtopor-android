package com.company.shtopor.vzvod.notify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftHapticFeedbackTest {

    @Test
    fun weakTick_isShortAndSoft() {
        assertEquals(18L, ShiftHapticFeedback.DURATION_MS)
        assertTrue(ShiftHapticFeedback.AMPLITUDE in 1..80)
    }
}
