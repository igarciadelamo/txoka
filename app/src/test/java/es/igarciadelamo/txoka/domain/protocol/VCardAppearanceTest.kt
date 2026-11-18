package es.igarciadelamo.txoka.domain.protocol

import es.igarciadelamo.txoka.domain.profile.CardAppearance
import es.igarciadelamo.txoka.domain.profile.CardAppearanceCodec
import es.igarciadelamo.txoka.domain.profile.CardBackground
import es.igarciadelamo.txoka.domain.profile.CardEffect
import es.igarciadelamo.txoka.domain.profile.CardMosaic
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.CardTextStyle
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.domain.profile.RgbColor
import es.igarciadelamo.txoka.domain.profile.TextFinish
import es.igarciadelamo.txoka.domain.profile.TextSize
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class VCardAppearanceTest {
    private val style = CardAppearance(
        background = CardBackground.HOLOGRAM,
        mosaic = CardMosaic.GEO_OPTICAL_WOVEN_GRID,
        effect = CardEffect.FIRE,
        topRight = RgbColor(1, 2, 3),
        bottomLeft = RgbColor(4, 5, 6),
        name = CardTextStyle(TextFinish.METAL, TextSize.XL, RgbColor(7, 8, 9)),
        data = CardTextStyle(TextFinish.RAISED, TextSize.XL, RgbColor(10, 11, 12)),
    )
    private val fallback = CardPalette(RgbColor(18, 52, 86), RgbColor(171, 205, 239))
    private val payload = VCardPayload(
        name = "Aña; 水, 😀\\\nRuiz",
        email = "ana@example.com",
        phone = "+34600123456",
        linkedin = "ana.ruiz",
        instagram = "ana.png",
        palette = fallback.copy(appearance = style),
    )

    @Test
    fun `new peers preserve all shared layers and text independently`() {
        val text = VCardSerializer.encode(payload)
        assertTrue(text.contains("X-TXOKA-STYLE:1:edRz0wECAwQFBgcICQoLDA\r\n"))
        assertTrue(text.contains("X-TXOKA-CARD-ACCENT:#123456\r\n"))
        assertTrue(text.contains("X-TXOKA-CARD-ACCENT2:#ABCDEF\r\n"))
        assertEquals(payload, decode(text))
        assertEquals(
            payload,
            Profile(
                payload.name,
                phone = payload.phone,
                email = payload.email,
                linkedin = payload.linkedin,
                instagram = payload.instagram,
                card = payload.palette,
            ).toVCardPayload(),
        )
        assertTrue(text.split("\r\n").all { it.toByteArray().size <= 75 })
    }

    @Test
    fun `legacy card round trip keeps null appearance and historical colours`() {
        val legacy = payload.copy(palette = fallback)
        val text = VCardSerializer.encode(legacy)
        assertFalse(text.contains("X-TXOKA-STYLE"))
        assertEquals(legacy, decode(text))
        val downgraded = VCardSerializer.encode(payload).lineSequence()
            .filterNot { it.startsWith("X-TXOKA-STYLE:") }.joinToString("\r\n")
        assertEquals(legacy, decode(downgraded))
    }

    @Test
    fun `explicit default style remains explicit even when legacy chooses dark text`() {
        val white = RgbColor(255, 255, 255)
        val explicit = payload.copy(palette = CardPalette(white, white, CardAppearance()))
        val text = VCardSerializer.encode(explicit)
        assertTrue(text.contains("X-TXOKA-STYLE:1:AGgB\r\n"))
        assertEquals(CardAppearance(), decode(text).palette.appearance)
        val legacy = explicit.copy(palette = CardPalette(white, white))
        assertNull(decode(VCardSerializer.encode(legacy)).palette.appearance)
    }

    @Test
    fun `invalid or duplicate style never drops the contact or changes fallback colours`() {
        val text = VCardSerializer.encode(payload)
        val validProperty = "X-TXOKA-STYLE:${CardAppearanceCodec.encode(style)}"
        listOf(
            "X-TXOKA-STYLE:2:edRz0wECAwQFBgcICQoLDA",
            "X-TXOKA-STYLE:1:AGgB=",
            "X-TXOKA-STYLE:1:AGgB,",
            "X-TXOKA-STYLE:1:\\AGgB",
            "X-TXOKA-STYLE:1:AGgB ",
            "X-TXOKA-STYLE:1:AGgB\r\n$validProperty",
            "$validProperty\r\n$validProperty",
            "$validProperty\r\nX-TXOKA-STYLE:invalid",
            "X-TXOKA-STYLE;X-PARAM=1:${CardAppearanceCodec.encode(style)}",
            "X-TXOKA-STYLE:",
            "X-TXOKA-STYLE:1:cWgBvw", // Retired/unknown unsigned selector 191.
            "X-TXOKA-STYLE:1:cWgB1A", // Unknown unsigned selector 212.
        ).forEach { invalid ->
            assertEquals(invalid, payload.copy(palette = fallback), decode(text.replace(validProperty, invalid)))
        }
    }

    @Test
    fun `a valid extension uses default fallback palette when legacy colours are absent or broken`() {
        val text = VCardSerializer.encode(payload)
            .replace("X-TXOKA-CARD-ACCENT:#123456", "X-TXOKA-CARD-ACCENT:invalid")
        assertEquals(CardPalette.Default.copy(appearance = style), decode(text).palette)
        val noColours = text.lineSequence().filterNot { it.startsWith("X-TXOKA-CARD-ACCENT") }.joinToString("\r\n")
        assertEquals(CardPalette.Default.copy(appearance = style), decode(noColours).palette)
    }

    @Test
    fun `folding and case insensitive property names preserve canonical style`() {
        val text = VCardSerializer.encode(payload)
            .replace("X-TXOKA-STYLE:1:edRz0wECAwQFBgcICQoLDA", "x-txoka-style:1:edRz0wEC\r\n AwQFBgcICQoLDA")
        assertEquals(payload, decode(text))
    }

    @Test
    fun `style overhead includes the real short record transition`() {
        for ((legacyBytes, marginal) in listOf(215 to 40, 216 to 43)) {
            val legacy = payloadWithVCardSize(legacyBytes)
            val enhanced = legacy.copy(palette = legacy.palette.copy(appearance = style))
            val before = VCardSerializer.encode(legacy)
            val after = VCardSerializer.encode(enhanced)
            assertEquals(40, after.toByteArray().size - before.toByteArray().size)
            assertEquals(marginal, NdefMessage.encodedSize(after) - NdefMessage.encodedSize(before))
            assertEquals(NdefMessage.encodedSize(after), NdefMessage.encode(after).size)
        }
    }

    @Test
    fun `the exact 768 byte message fits but 769 is rejected without losing style`() {
        for (legacySize in listOf(728, 729)) {
            val legacy = payloadWithMessageSize(legacySize)
            val enhanced = legacy.copy(palette = legacy.palette.copy(appearance = style))
            val text = VCardSerializer.encode(enhanced)
            assertEquals(legacySize + 40, NdefMessage.encodedSize(text))
            assertEquals(enhanced, decode(text))
            assertEquals(legacySize == 728, enhanced.fitsInTag())
            if (legacySize == 728) {
                assertEquals(768, NdefMessage.encode(text).size)
            } else {
                assertThrows(IllegalArgumentException::class.java) { NdefMessage.encode(text) }
            }
        }
    }

    @Test
    fun `type four fragmented reads reconstruct style header selector and RGB blocks exactly`() {
        val text = VCardSerializer.encode(payload)
        val message = NdefMessage.encode(text)
        val tag = Type4Tag(message)
        val aid = Type4Tag.NDEF_AID
        assertStatusOk(tag.process(byteArrayOf(0, 0xA4.toByte(), 4, 0, aid.size.toByte()) + aid))
        assertStatusOk(tag.process(byteArrayOf(0, 0xA4.toByte(), 0, 0x0C, 2, 0xE1.toByte(), 4)))
        val expected = byteArrayOf((message.size shr 8).toByte(), message.size.toByte()) + message
        for (chunkSize in listOf(1, 2, 3, 7, 13, 59, 255, 256)) {
            val received = ByteArrayOutputStream()
            var offset = 0
            while (offset < expected.size) {
                val response = tag.process(
                    byteArrayOf(0, 0xB0.toByte(), (offset shr 8).toByte(), offset.toByte(), chunkSize.toByte()),
                )
                assertStatusOk(response)
                val data = response.copyOfRange(0, response.size - 2)
                assertTrue(data.isNotEmpty())
                received.write(data)
                offset += data.size
            }
            assertArrayEquals("chunk size $chunkSize", expected, received.toByteArray())
            assertEquals(payload, decode(NdefMessage.decode(received.toByteArray().drop(2).toByteArray())!!))
        }
    }

    private fun decode(text: String): VCardPayload = (VCardSerializer.decode(text) as VCardParseResult.Success).payload

    private fun payloadWithVCardSize(bytes: Int): VCardPayload = (1..700).asSequence()
        .map { VCardPayload(name = "A".repeat(it)) }
        .first { VCardSerializer.encode(it).toByteArray().size == bytes }

    private fun payloadWithMessageSize(bytes: Int): VCardPayload = (1..700).asSequence()
        .map { VCardPayload(name = "A".repeat(it)) }
        .first { NdefMessage.encodedSize(VCardSerializer.encode(it)) == bytes }

    private fun assertStatusOk(response: ByteArray) {
        assertEquals(0x90, response[response.size - 2].toInt() and 0xFF)
        assertEquals(0, response.last().toInt() and 0xFF)
    }
}
