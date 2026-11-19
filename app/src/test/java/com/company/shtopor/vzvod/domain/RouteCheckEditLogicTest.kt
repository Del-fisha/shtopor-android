package com.company.shtopor.vzvod.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RouteCheckEditLogicTest {
    @Test
    fun normalizeTime_padsHour() {
        assertEquals("09:05", RouteCheckEditLogic.normalizeTime("9:05"))
        assertEquals("21:14", RouteCheckEditLogic.normalizeTime("21:14:00"))
        assertNull(RouteCheckEditLogic.normalizeTime("24:00"))
        assertNull(RouteCheckEditLogic.normalizeTime("10"))
    }
}
