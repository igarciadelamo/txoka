package es.igarciadelamo.txoka.domain.received

import org.junit.Assert.assertEquals
import org.junit.Test

class ContactKeysTest {
    @Test
    fun `phone keys keep only digits`() {
        assertEquals("34600123456", ContactKeys.phone("+34 600 123 456"))
        assertEquals("", ContactKeys.phone(null))
        assertEquals("", ContactKeys.phone(""))
    }

    @Test
    fun `email keys are trimmed and lowercased`() {
        assertEquals("marta@example.com", ContactKeys.email("  Marta@Example.com "))
        assertEquals("", ContactKeys.email(null))
    }
}
