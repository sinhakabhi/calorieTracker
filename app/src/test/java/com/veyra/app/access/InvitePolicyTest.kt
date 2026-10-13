package com.veyra.app.access

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InvitePolicyTest {

    private val list = """
        Friend@Gmail.com, tester2@gmail.com
        @mycompany.in
    """.trimIndent()

    @Test
    fun exactEmailsAreCaseInsensitive() {
        assertTrue(InvitePolicy.isAllowed("friend@gmail.com", list))
        assertTrue(InvitePolicy.isAllowed("  TESTER2@gmail.com ", list))
        assertFalse(InvitePolicy.isAllowed("stranger@gmail.com", list))
    }

    @Test
    fun domainEntriesAllowEveryoneOnThatDomain() {
        assertTrue(InvitePolicy.isAllowed("anyone@mycompany.in", list))
        assertFalse(InvitePolicy.isAllowed("anyone@mycompany.com", list))
        // A domain entry must not match a look-alike address.
        assertFalse(InvitePolicy.isAllowed("evil@notmycompany.in", list))
    }

    @Test
    fun emptyListAllowsNobodyAndStarAllowsEveryone() {
        assertFalse(InvitePolicy.isAllowed("friend@gmail.com", ""))
        assertTrue(InvitePolicy.isAllowed("friend@gmail.com", "*"))
        assertFalse(InvitePolicy.isAllowed("not-an-email", "*"))
    }
}
