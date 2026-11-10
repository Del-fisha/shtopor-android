package com.company.shtopor.vzvod.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionAuthPolicyTest {

    @Test
    fun mustReauthOnProcessStart_alwaysTrue() {
        assertTrue(SessionAuthPolicy.mustReauthOnProcessStart())
    }

    @Test
    fun mustLogoutOnHttpCode_only401() {
        assertTrue(SessionAuthPolicy.mustLogoutOnHttpCode(401))
        assertFalse(SessionAuthPolicy.mustLogoutOnHttpCode(403))
        assertFalse(SessionAuthPolicy.mustLogoutOnHttpCode(500))
        assertFalse(SessionAuthPolicy.mustLogoutOnHttpCode(0))
    }
}
