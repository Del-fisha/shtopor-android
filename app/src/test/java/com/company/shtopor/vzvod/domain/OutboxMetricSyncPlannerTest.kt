package com.company.shtopor.vzvod.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OutboxMetricSyncPlannerTest {

    @Test
    fun plan_collapsesFiveMigrantDeltasIntoOneAbsolute() {
        val shiftId = "shift-1"
        val pending = (1..5).map {
            PendingOutboxOp(id = "op-$it", type = "ADJUST_MIGRANT", shiftId = shiftId)
        }
        val local = mapOf(
            shiftId to LocalShiftMetrics(ibdr = 10, migrant = 10, statements = 0, claims = 0)
        )

        val plan = OutboxMetricSyncPlanner.plan(pending, local)

        assertEquals(1, plan.absoluteMetricSyncs.size)
        assertEquals(shiftId, plan.absoluteMetricSyncs[0].shiftId)
        assertEquals(10, plan.absoluteMetricSyncs[0].metrics.ibdr)
        assertEquals(10, plan.absoluteMetricSyncs[0].metrics.migrant)
        assertEquals(5, plan.absoluteMetricSyncs[0].consumedOpIds.size)
        assertTrue(plan.remainingOps.isEmpty())
    }

    @Test
    fun plan_keepsAdminCreateInRemaining() {
        val pending = listOf(
            PendingOutboxOp("m1", "ADJUST_IBDR", "s1"),
            PendingOutboxOp("a1", "CREATE_ADMIN_VIOLATION", "s1"),
            PendingOutboxOp("e1", "END_SHIFT", "s1")
        )
        val local = mapOf("s1" to LocalShiftMetrics(8, 8, 1, 0))

        val plan = OutboxMetricSyncPlanner.plan(pending, local)

        assertEquals(1, plan.absoluteMetricSyncs.size)
        assertEquals(listOf("a1", "e1"), plan.remainingOps.map { it.id })
    }

    @Test
    fun plan_withoutLocalMetrics_keepsDeltasInRemaining() {
        val pending = listOf(PendingOutboxOp("m1", "ADJUST_MIGRANT_AND_IBDR", "missing"))
        val plan = OutboxMetricSyncPlanner.plan(pending, emptyMap())
        assertTrue(plan.absoluteMetricSyncs.isEmpty())
        assertEquals(listOf("m1"), plan.remainingOps.map { it.id })
    }

    @Test
    fun shouldEnqueueMetricDeltaAfterError_alwaysTrue_absoluteFlushIsSafe() {
        assertTrue(OutboxMetricSyncPlanner.shouldEnqueueMetricDeltaAfterError())
    }

    @Test
    fun absoluteSync_isIdempotentTarget() {
        // Повторный sync с теми же local 10 не должен «накрутить» сверх 10.
        val local = LocalShiftMetrics(10, 10, 0, 0)
        val plan1 = OutboxMetricSyncPlanner.plan(
            listOf(PendingOutboxOp("a", "ADJUST_MIGRANT", "s")),
            mapOf("s" to local)
        )
        val plan2 = OutboxMetricSyncPlanner.plan(
            listOf(PendingOutboxOp("b", "ADJUST_MIGRANT_AND_IBDR", "s")),
            mapOf("s" to local)
        )
        assertEquals(plan1.absoluteMetricSyncs[0].metrics, plan2.absoluteMetricSyncs[0].metrics)
    }
}
