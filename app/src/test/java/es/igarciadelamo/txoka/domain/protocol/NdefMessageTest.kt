package es.igarciadelamo.txoka.domain.protocol

import es.igarciadelamo.txoka.BuildConfig
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

        assertEquals(0x92, message[0].toInt() and 0xFF)
        assertEquals(NdefMessage.MIME_TYPE.length, message[1].toInt() and 0xFF)
        assertEquals(vCard.toByteArray().size, message[2].toInt() and 0xFF)
        assertEquals(NdefMessage.MIME_TYPE, String(message, 3, NdefMessage.MIME_TYPE.length))
    }

    @Test
    fun `adds an android application record for this app`() {
        val message = NdefMessage.encode(vCard)
        val aar = recordEnd(message)

        assertEquals(0x54, message[aar].toInt() and 0xFF)
        assertEquals(AAR_TYPE.length, message[aar + 1].toInt() and 0xFF)
        assertEquals(NdefMessage.APP_PACKAGE.toByteArray().size, message[aar + 2].toInt() and 0xFF)
        assertEquals(AAR_TYPE, String(message, aar + 3, AAR_TYPE.length))
        assertEquals(
            NdefMessage.APP_PACKAGE,
            String(message, aar + 3 + AAR_TYPE.length, NdefMessage.APP_PACKAGE.length),
        )
        assertEquals(message.size, aar + AAR_RECORD_BYTES)
    }

    @Test
    fun `names the package the app is built with`() {
        assertEquals(BuildConfig.APPLICATION_ID, NdefMessage.APP_PACKAGE)
    }

    @Test
    fun `round trips a short card`() {
        assertEquals(vCard, NdefMessage.decode(NdefMessage.encode(vCard)))
    }

    @Test
    fun `uses a long record above 255 bytes`() {
        val longCard = "BEGIN:VCARD\r\nFN:${"A".repeat(300)}\r\nEND:VCARD\r\n"
        val message = NdefMessage.encode(longCard)

        assertEquals(0x82, message[0].toInt() and 0xFF)
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
        val card = "A".repeat(NdefMessage.MAX_MESSAGE_BYTES - LONG_RECORD_OVERHEAD - AAR_RECORD_BYTES)
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

    /** Index where the record after the vCard starts. */
    private fun recordEnd(message: ByteArray): Int {
        val isShort = message[0].toInt() and 0x10 != 0
        val typeLength = message[1].toInt() and 0xFF
        val headerBytes = if (isShort) 3 else 6
        val payloadLength = if (isShort) {
            message[2].toInt() and 0xFF
        } else {
            var length = 0
            repeat(4) { index -> length = (length shl 8) or (message[2 + index].toInt() and 0xFF) }
            length
        }
        return headerBytes + typeLength + payloadLength
    }

    private fun longPayloadLength(message: ByteArray): Int {
        var length = 0
        repeat(4) { index -> length = (length shl 8) or (message[2 + index].toInt() and 0xFF) }
        return length
    }

    private companion object {
        /** Flags, type length, the four length bytes and the `text/vcard` type. */
        const val LONG_RECORD_OVERHEAD = 2 + 4 + 10

        /** Flags, type length, payload length, the `android.com:pkg` type and the package name. */
        val AAR_RECORD_BYTES = 3 + AAR_TYPE.length + NdefMessage.APP_PACKAGE.length
        const val AAR_TYPE = "android.com:pkg"
    }
}
