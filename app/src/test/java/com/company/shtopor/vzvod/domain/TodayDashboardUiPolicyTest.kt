package com.company.shtopor.vzvod.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayDashboardUiPolicyTest {

    @Test
    fun sectionOrder_totalsThenRoutes() {
        assertEquals(
            listOf(TodayDashboardSection.TOTALS, TodayDashboardSection.ROUTES),
            TodayDashboardUiPolicy.sectionOrder()
        )
    }

    @Test
    fun totalsTitle_isObshchee() {
        assertEquals("Общее", TodayDashboardUiPolicy.totalsTitle())
    }

    @Test
    fun route_onlyRouteApUpExpandable() {
        assertTrue(TodayDashboardUiPolicy.isExpandable(TodayDashboardTapTarget.ROUTE_LABEL))
        assertTrue(TodayDashboardUiPolicy.isExpandable(TodayDashboardTapTarget.AP))
        assertTrue(TodayDashboardUiPolicy.isExpandable(TodayDashboardTapTarget.UP))
        assertFalse(TodayDashboardUiPolicy.isExpandable(TodayDashboardTapTarget.IBDR))
        assertFalse(TodayDashboardUiPolicy.isExpandable(TodayDashboardTapTarget.MIGRANT))
        assertFalse(TodayDashboardUiPolicy.isExpandable(TodayDashboardTapTarget.STATEMENTS))
    }

    @Test
    fun totals_onlyApUpExpandable() {
        assertTrue(TodayDashboardUiPolicy.isTotalsExpandable(TodayDashboardTapTarget.AP))
        assertTrue(TodayDashboardUiPolicy.isTotalsExpandable(TodayDashboardTapTarget.UP))
        assertFalse(TodayDashboardUiPolicy.isTotalsExpandable(TodayDashboardTapTarget.ROUTE_LABEL))
        assertFalse(TodayDashboardUiPolicy.isTotalsExpandable(TodayDashboardTapTarget.IBDR))
        assertFalse(TodayDashboardUiPolicy.isTotalsExpandable(TodayDashboardTapTarget.MIGRANT))
        assertFalse(TodayDashboardUiPolicy.isTotalsExpandable(TodayDashboardTapTarget.STATEMENTS))
    }

    @Test
    fun totalsAndRoutes_collapsedByDefault() {
        assertTrue(TodayDashboardUiPolicy.sectionCollapsedByDefault())
    }

    @Test
    fun metricRows_keepDisplayOrder() {
        assertEquals(
            listOf(
                TodayDashboardTapTarget.IBDR,
                TodayDashboardTapTarget.MIGRANT,
                TodayDashboardTapTarget.AP,
                TodayDashboardTapTarget.UP,
                TodayDashboardTapTarget.STATEMENTS
            ),
            TodayDashboardUiPolicy.metricRowOrder()
        )
    }
}
