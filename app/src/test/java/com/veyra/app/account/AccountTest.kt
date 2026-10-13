package com.veyra.app.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountTest {
    @Test
    fun namesAndInitials() {
        val account = Account("Abhinav Kumar Sinha", "a@example.com")
        assertEquals("Abhinav", account.firstName)
        assertEquals("AK", account.initials)
        assertFalse(account.isGuest)
    }

    @Test
    fun guest() {
        val guest = Account("", null)
        assertTrue(guest.isGuest)
        assertEquals("", guest.firstName)
        assertEquals("?", guest.initials)
    }
}
