package es.igarciadelamo.txoka.domain.received

import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.ProfileLimits
import es.igarciadelamo.txoka.domain.protocol.NdefMessage
import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import es.igarciadelamo.txoka.domain.protocol.VCardSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IncomingCardTest {
    private val payload = VCardPayload(
        name = "Marta Ruiz",
        phone = "+34600111222",
        email = "marta@example.com",
        linkedin = "marta-ruiz",
        instagram = "marta.ruiz",
        palette = CardPalette.Presets[2],
    )

    @Test
    fun `a valid card survives unchanged`() {
        assertEquals(payload, IncomingCard.sanitize(payload))
    }

    @Test
    fun `a valid card survives the full ndef decode`() {
        val ndef = NdefMessage.encode(VCardSerializer.encode(payload))

        assertEquals(payload, IncomingCard.fromNdef(ndef))
    }

    @Test
    fun `clamps oversized fields`() {
        val clean = IncomingCard.sanitize(
            VCardPayload(
                name = "N".repeat(ProfileLimits.NAME + 10),
                phone = "1".repeat(ProfileLimits.PHONE + 10),
                email = "a".repeat(ProfileLimits.EMAIL + 10),
                linkedin = "a".repeat(ProfileLimits.HANDLE + 10),
            ),
        )!!

        assertEquals(ProfileLimits.NAME, clean.name.length)
        assertEquals(ProfileLimits.PHONE, clean.phone!!.length)
        assertEquals(ProfileLimits.EMAIL, clean.email!!.length)
        assertEquals(ProfileLimits.HANDLE, clean.linkedin!!.length)
    }

    @Test
    fun `strips control and format characters`() {
        val clean = IncomingCard.sanitize(
            VCardPayload(
                name = "Ana\u0000\nRuiz\u200B\u202E",
                phone = "+34\n600\t111",
                email = "ana\u200B@example.com",
                instagram = "ana\u0000.ruiz",
            ),
        )!!

        assertEquals("Ana Ruiz", clean.name)
        assertEquals("+34600111", clean.phone)
        assertEquals("ana@example.com", clean.email)
        assertEquals("ana.ruiz", clean.instagram)
    }

    @Test
    fun `rejects a card without a usable name`() {
        assertNull(IncomingCard.sanitize(VCardPayload(name = " \n\u0000 ")))
    }

    @Test
    fun `drops invalid handles and blank optional fields`() {
        val clean = IncomingCard.sanitize(
            VCardPayload(name = "Ana", phone = " ", linkedin = "not a handle", instagram = "@"),
        )!!

        assertNull(clean.phone)
        assertNull(clean.linkedin)
        assertNull(clean.instagram)
    }

    @Test
    fun `rejects a message above the byte cap`() {
        assertNull(IncomingCard.fromNdef(ByteArray(IncomingCard.MAX_MESSAGE_BYTES + 1)))
    }

    @Test
    fun `rejects a message that is not a card`() {
        assertNull(IncomingCard.fromNdef(ByteArray(8)))
    }
}
