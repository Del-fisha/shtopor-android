package com.company.shtopor.vzvod.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckingShiftPolicyTest {
    @Test
    fun isChecking_onlyExactCheckingId() {
        assertTrue(CheckingShiftPolicy.isChecking("CHECKING"))
        assertFalse(CheckingShiftPolicy.isChecking("VZVOD_ROUTE"))
        assertFalse(CheckingShiftPolicy.isChecking(null))
        assertFalse(CheckingShiftPolicy.isChecking(""))
    }

    @Test
    fun partnersRequired_falseOnlyForChecking() {
        assertFalse(CheckingShiftPolicy.partnersRequired("CHECKING"))
        assertTrue(CheckingShiftPolicy.partnersRequired("BAT_POST"))
        assertTrue(CheckingShiftPolicy.partnersRequired(null))
    }

    @Test
    fun showRouteCheckCounters_onlyForCheckingShift() {
        assertTrue(CheckingShiftPolicy.showRouteCheckCounters("CHECKING"))
        assertFalse(CheckingShiftPolicy.showRouteCheckCounters("VZVOD_ROUTE"))
        assertFalse(CheckingShiftPolicy.showRouteCheckCounters(null))
    }

    @Test
    fun showMetricCounters_hiddenForCheckingShift() {
        assertFalse(CheckingShiftPolicy.showMetricCounters("CHECKING"))
        assertTrue(CheckingShiftPolicy.showMetricCounters("BAT_POST"))
        assertTrue(CheckingShiftPolicy.showMetricCounters(null))
    }
}
