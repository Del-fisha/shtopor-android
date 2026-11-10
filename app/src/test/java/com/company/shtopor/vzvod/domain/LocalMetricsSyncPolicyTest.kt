package com.company.shtopor.vzvod.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalMetricsSyncPolicyTest {

    @Test
    fun shouldOverwrite_whenNoPendingOps() {
        assertTrue(LocalMetricsSyncPolicy.shouldOverwriteLocalFromServer(0))
    }

    @Test
    fun shouldNotOverwrite_whenPendingOpsExist() {
        assertFalse(LocalMetricsSyncPolicy.shouldOverwriteLocalFromServer(1))
        assertFalse(LocalMetricsSyncPolicy.shouldOverwriteLocalFromServer(10))
    }
}
