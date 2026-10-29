package es.igarciadelamo.txoka.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumbersTest {
    @Test
    fun `normalises international input`() {
        assertEquals("+34600123456", PhoneNumbers.toE164("+34 600 123 456", "ES"))
        assertEquals("+14155552671", PhoneNumbers.toE164("+1 (415) 555-2671", "US"))
    }

    @Test
    fun `normalises national input against the default region`() {
        assertEquals("+34600123456", PhoneNumbers.toE164("600 12 34 56", "ES"))
        assertEquals("+34600123456", PhoneNumbers.toE164("600-123-456", "es"))
    }

    @Test
    fun `rejects invalid and empty input`() {
        assertNull(PhoneNumbers.toE164("", "ES"))
        assertNull(PhoneNumbers.toE164("   ", "ES"))
        assertNull(PhoneNumbers.toE164("123", "ES"))
        assertNull(PhoneNumbers.toE164("not a phone", "ES"))
    }
}
