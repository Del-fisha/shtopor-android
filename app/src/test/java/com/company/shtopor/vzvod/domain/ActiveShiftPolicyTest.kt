package com.company.shtopor.vzvod.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveShiftPolicyTest {

    @Test
    fun isOpen_whenEndTimeMissing() {
        assertTrue(ActiveShiftPolicy.isOpen(null))
        assertTrue(ActiveShiftPolicy.isOpen(""))
        assertTrue(ActiveShiftPolicy.isOpen("   "))
        assertFalse(ActiveShiftPolicy.isOpen("18:00"))
    }

    @Test
    fun openFromServer_picksFirstWithoutEndTime() {
        val id = ActiveShiftPolicy.openFromServer(
            listOf(
                "closed" to "10:00",
                "open-1" to null,
                "open-2" to null
            )
        )
        assertEquals("open-1", id)
    }

    @Test
    fun openFromServer_noneWhenAllClosed() {
        assertNull(
            ActiveShiftPolicy.openFromServer(
                listOf("a" to "09:00", "b" to "11:00")
            )
        )
    }

    @Test
    fun mergeCreatedIfMissing_whenServerLag() {
        assertTrue(ActiveShiftPolicy.mergeCreatedIfMissing(listOf("other"), "new-id"))
        assertFalse(ActiveShiftPolicy.mergeCreatedIfMissing(listOf("new-id"), "new-id"))
        assertFalse(ActiveShiftPolicy.mergeCreatedIfMissing(listOf("new-id"), null))
    }

    @Test
    fun notificationMustUseCacheOnly_neverNestedRefresh() {
        // Защита от ANR: уведомление/reconcile не должны снова дергать refreshShifts.
        assertTrue(ActiveShiftPolicy.notificationUsesCacheOnly())
        assertFalse(ActiveShiftPolicy.notificationMayCallRefreshShifts())
    }

    @Test
    fun resolveOpenAfterReconcile_prefersServerOpenId() {
        val resolved = ActiveShiftPolicy.resolveOpenShiftId(
            serverOpenIds = listOf("server-open"),
            cachedOpenId = "stale-local"
        )
        assertEquals("server-open", resolved)
    }

    @Test
    fun resolveOpenAfterReconcile_nullWhenServerHasNoOpen() {
        assertNull(
            ActiveShiftPolicy.resolveOpenShiftId(
                serverOpenIds = emptyList(),
                cachedOpenId = "zombie"
            )
        )
    }
}
