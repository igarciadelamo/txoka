package es.igarciadelamo.txoka.domain.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class NdefMessageTest {
    private val vCard = "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:Ana\r\nEND:VCARD\r\n"

    @Test
    fun `encodes a short mime record`() {
        val message = NdefMessage.encode(vCard)

        assertEquals(0xD2, message[0].toInt() and 0xFF)
        assertEquals(NdefMessage.MIME_TYPE.length, message[1].toInt() and 0xFF)
        assertEquals(vCard.toByteArray().size, message[2].toInt() and 0xFF)
        assertEquals(NdefMessage.MIME_TYPE, String(message, 3, NdefMessage.MIME_TYPE.length))
    }

    @Test
    fun `round trips a short card`() {
        assertEquals(vCard, NdefMessage.decode(NdefMessage.encode(vCard)))
    }

    @Test
    fun `uses a long record above 255 bytes`() {
        val longCard = "BEGIN:VCARD\r\nFN:${"A".repeat(300)}\r\nEND:VCARD\r\n"
        val message = NdefMessage.encode(longCard)

        assertEquals(0xC2, message[0].toInt() and 0xFF)
        assertEquals(longCard.toByteArray().size, longPayloadLength(message))
        assertEquals(longCard, NdefMessage.decode(message))
    }

    @Test
    fun `rejects cards above the message limit`() {
        val huge = "A".repeat(NdefMessage.MAX_MESSAGE_BYTES)

        assertThrows(IllegalArgumentException::class.java) { NdefMessage.encode(huge) }
    }

    @Test
    fun `sizes a card before encoding it`() {
        assertEquals(NdefMessage.encode(vCard).size, NdefMessage.encodedSize(vCard))
        assertTrue(NdefMessage.fits(vCard))
    }

    @Test
    fun `fits a card that fills the payload exactly`() {
        // A long record spends four length bytes on the header: flags, type length, length and type.
        val card = "A".repeat(NdefMessage.MAX_MESSAGE_BYTES - LONG_RECORD_OVERHEAD)
        val message = NdefMessage.encode(card)

        assertEquals(NdefMessage.MAX_MESSAGE_BYTES, message.size)
        assertTrue(NdefMessage.fits(card))
        assertFalse(NdefMessage.fits("$card "))
    }

    @Test
    fun `counts multibyte text in bytes`() {
        val emoji = "😀".repeat(200)

        assertEquals(800, emoji.toByteArray(Charsets.UTF_8).size)
        assertFalse(NdefMessage.fits(emoji))
    }

    @Test
    fun `returns null for other mime types`() {
        val message = NdefMessage.encode(vCard)
            .copyOf()
            .also { bytes ->
                val type = "text/plain".toByteArray()
                type.copyInto(bytes, 3)
            }

        assertNull(NdefMessage.decode(message))
    }

    @Test
    fun `returns null for truncated messages`() {
        val message = NdefMessage.encode(vCard)

        assertNull(NdefMessage.decode(message.copyOfRange(0, 5)))
        assertNull(NdefMessage.decode(ByteArray(0)))
    }

    private fun longPayloadLength(message: ByteArray): Int {
        var length = 0
        repeat(4) { index -> length = (length shl 8) or (message[2 + index].toInt() and 0xFF) }
        return length
    }

    private companion object {
        /** Flags, type length, the four length bytes and the `text/vcard` type. */
        const val LONG_RECORD_OVERHEAD = 2 + 4 + 10
    }
}
