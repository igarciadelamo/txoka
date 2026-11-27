package es.igarciadelamo.txoka.domain.received

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactSearchTest {
    private val contact = ReceivedContact(
        id = 1,
        name = "Ana López",
        phone = "+34 600 999 888",
        email = "ana.lopez@example.com",
        linkedin = "ana-lopez",
        instagram = "ana.png",
        note = "Met at the Compose meetup",
    )

    @Test
    fun `a blank query matches everything`() {
        assertTrue(contact.matchesSearch("  "))
    }

    @Test
    fun `ignores accents and case`() {
        assertTrue(contact.matchesSearch("lopez"))
        assertTrue(contact.matchesSearch("LÓPEZ"))
        assertTrue(contact.matchesSearch("ANA.LOPEZ"))
    }

    @Test
    fun `matches phone digits with separators`() {
        assertTrue(contact.matchesSearch("600999888"))
        assertTrue(contact.matchesSearch("600 999 888"))
        assertTrue(contact.matchesSearch("+34 600 999 888"))
    }

    @Test
    fun `does not match the phone when the query contains text`() {
        assertFalse(contact.matchesSearch("zzz 600"))
    }

    @Test
    fun `matches the note, the email and the handles`() {
        assertTrue(contact.matchesSearch("meetup"))
        assertTrue(contact.matchesSearch("ana.lopez@example.com"))
        assertTrue(contact.matchesSearch("ana-lopez"))
        assertTrue(contact.matchesSearch("ana.png"))
    }

    @Test
    fun `does not match an unrelated query`() {
        assertFalse(contact.matchesSearch("zzz"))
    }
}
