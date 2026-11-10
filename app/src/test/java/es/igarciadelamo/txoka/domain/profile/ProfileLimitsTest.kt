package es.igarciadelamo.txoka.domain.profile

import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import es.igarciadelamo.txoka.domain.protocol.fitsInTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileLimitsTest {
    @Test
    fun `keeps a value below the limit`() {
        assertEquals("Ana", ProfileLimits.limit("Ana", ProfileLimits.NAME))
    }

    @Test
    fun `cuts a value above the limit`() {
        val limited = ProfileLimits.limit("A".repeat(100), ProfileLimits.NAME)

        assertEquals(ProfileLimits.NAME, limited.length)
        assertEquals("A".repeat(ProfileLimits.NAME), limited)
    }

    @Test
    fun `never splits a surrogate pair`() {
        val limited = ProfileLimits.limit("a😀😀", 2)

        assertEquals("a😀", limited)
        assertEquals(2, limited.codePointCount(0, limited.length))
    }

    @Test
    fun `a card with every field at its limit still fits in the tag`() {
        val longest = VCardPayload(
            name = "A".repeat(ProfileLimits.NAME),
            phone = "+34" + "6".repeat(ProfileLimits.PHONE - 3),
            email = "a".repeat(ProfileLimits.EMAIL - 5) + "@b.cd",
            linkedin = "a".repeat(ProfileLimits.HANDLE),
            instagram = "b".repeat(ProfileLimits.HANDLE),
        )

        assertTrue(longest.fitsInTag())
    }

    @Test
    fun `a card can still overflow with multibyte text`() {
        val emojiName = VCardPayload(
            name = "😀".repeat(ProfileLimits.NAME),
            phone = "+34600123456",
            email = "a".repeat(ProfileLimits.EMAIL - 5) + "@b.cd",
            linkedin = "a".repeat(ProfileLimits.HANDLE),
            instagram = "b".repeat(ProfileLimits.HANDLE),
        )

        assertFalse(emojiName.fitsInTag())
    }
}
