package es.igarciadelamo.txoka.domain.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
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
}
