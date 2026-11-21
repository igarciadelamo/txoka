package es.igarciadelamo.txoka.domain.protocol

import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.Decoder
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import es.igarciadelamo.txoka.domain.profile.CardAppearance
import es.igarciadelamo.txoka.domain.profile.CardBackground
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class QrVCardCodecTest {
    private val style = CardPalette.Default.copy(appearance = CardAppearance(background = CardBackground.HOLOGRAM))

    @Test
    fun `round trips Unicode contact and style through a real UTF-8 QR symbol`() {
        val profile = Profile(
            name = "Ana 世界 😀; García",
            phone = "+34600123456",
            email = "ana@example.com",
            linkedin = "ana-garcia",
            instagram = "ana.garcia",
            card = style,
        )

        val text = QrVCardCodec.encode(profile)

        assertEquals(VCardSerializer.encode(profile.toVCardPayload()), text)
        assertTrue(text.contains("X-TXOKA-STYLE:"))
        assertFalse(text.contains(NdefMessage.APP_PACKAGE))
        assertFalse(text.contains("android.com:pkg"))
        assertEquals(profile.toVCardPayload(), QrVCardCodec.decode(qrRoundTrip(text)))
    }

    @Test
    fun `exact QR cap encodes at correction M with ECI while NFC remains capped`() {
        val profile = profileOfSize(QrVCardCodec.MAX_QR_VCARD_BYTES)
        val text = QrVCardCodec.encode(profile)

        assertEquals(2048, QrVCardCodec.byteSize(profile))
        assertTrue(QrVCardCodec.fits(profile))
        assertEquals(text, qrRoundTrip(text))
        assertEquals(profile.toVCardPayload(), QrVCardCodec.decode(text))
        assertEquals(768, NdefMessage.MAX_MESSAGE_BYTES)
        assertFalse(profile.toVCardPayload().fitsInTag())
        assertThrows(IllegalArgumentException::class.java) { NdefMessage.encode(text) }
    }

    @Test
    fun `rejects one byte beyond the QR budget without truncation`() {
        val profile = profileOfSize(QrVCardCodec.MAX_QR_VCARD_BYTES + 1)
        val text = VCardSerializer.encode(profile.toVCardPayload())

        assertEquals(2049, QrVCardCodec.byteSize(profile))
        assertFalse(QrVCardCodec.fits(profile))
        assertNull(QrVCardCodec.decode(text))
        assertThrows(IllegalArgumentException::class.java) { QrVCardCodec.encode(profile) }
    }

    @Test
    fun `bounds UTF-8 bytes before parsing multibyte or huge input`() {
        val text = "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:${"界".repeat(800)}\r\nEND:VCARD\r\n"

        assertTrue(text.length < QrVCardCodec.MAX_QR_VCARD_BYTES)
        assertTrue(text.toByteArray(Charsets.UTF_8).size > QrVCardCodec.MAX_QR_VCARD_BYTES)
        assertNull(QrVCardCodec.decode(text))
        assertNull(QrVCardCodec.decode("A".repeat(100_000)))
    }

    @Test
    fun `rejects malformed vCard envelopes and invalid UTF-8 strings`() {
        val valid = QrVCardCodec.encode(Profile(name = "Ana", email = "ana@example.com"))
        val malformed = listOf(
            "https://example.com",
            valid.replace("BEGIN:VCARD", "BEGIN:OTHER"),
            valid.replace("END:VCARD\r\n", ""),
            valid.replace("VERSION:3.0", "VERSION:4.0"),
            valid.replace("FN:Ana", "FN:"),
            valid.replace("FN:Ana", "FN:Ana\u0000"),
            valid.replace("FN:Ana", "FN:Ana\uD800"),
            valid.replace("FN:Ana", "FN:Ana\uDC00"),
            valid.replace("VERSION:3.0", "VERSION:3.0\r\nVERSION:3.0"),
            valid + valid,
        )

        malformed.forEach { assertNull(QrVCardCodec.decode(it)) }
        val invalidProfile = Profile(name = "Ana\uD800", email = "ana@example.com")
        assertFalse(QrVCardCodec.fits(invalidProfile))
        assertThrows(IllegalArgumentException::class.java) { QrVCardCodec.encode(invalidProfile) }
    }

    @Test
    fun `accepts a normal third-party vCard without Txoka extensions`() {
        val text = "BEGIN:VCARD\nVERSION:3.0\nFN:Ana García\nEMAIL:ana@example.com\nEND:VCARD\n"

        assertEquals(
            VCardPayload(name = "Ana García", email = "ana@example.com", protocolVersion = 0),
            QrVCardCodec.decode(text),
        )
    }

    private fun qrRoundTrip(text: String): String {
        val symbol = Encoder.encode(text, ErrorCorrectionLevel.M, mapOf(EncodeHintType.CHARACTER_SET to "UTF-8"))
        val matrix = symbol.matrix
        val bits = Array(matrix.height) { y -> BooleanArray(matrix.width) { x -> matrix.get(x, y).toInt() == 1 } }
        return Decoder().decode(bits).text
    }

    private fun profileOfSize(bytes: Int): Profile = (1..bytes).asSequence().flatMap { length ->
        (0..3).asSequence().map { extra ->
            Profile(name = "Ana 世界😀" + "a".repeat(length), email = "a".repeat(extra) + "@b.cd", card = style)
        }
    }.first { QrVCardCodec.byteSize(it) == bytes }
}
