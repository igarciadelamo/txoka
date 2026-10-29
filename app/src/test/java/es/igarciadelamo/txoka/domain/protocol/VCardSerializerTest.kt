package es.igarciadelamo.txoka.domain.protocol

import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.RgbColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VCardSerializerTest {
    private val payload = VCardPayload(
        name = "Isidro García",
        phone = "+34600123456",
        email = "isidro@example.com",
        palette = CardPalette.Default,
        protocolVersion = 1,
    )

    @Test
    fun `round trips a full card`() {
        val decoded = VCardSerializer.decode(VCardSerializer.encode(payload))

        assertTrue(decoded is VCardParseResult.Success)
        assertEquals(payload, (decoded as VCardParseResult.Success).payload)
    }

    @Test
    fun `writes standard and custom properties`() {
        val text = VCardSerializer.encode(payload)

        assertTrue(text.startsWith("BEGIN:VCARD\r\nVERSION:3.0\r\n"))
        assertTrue(text.contains("FN:Isidro García"))
        assertTrue(text.contains("TEL;TYPE=CELL:+34600123456"))
        assertTrue(text.contains("EMAIL;TYPE=INTERNET:isidro@example.com"))
        assertTrue(text.contains("X-TXOKA-PROTOCOL:1"))
        assertTrue(text.contains("X-TXOKA-CARD-ACCENT:#4F46E5"))
        assertTrue(text.contains("X-TXOKA-CARD-ACCENT2:#06B6D4"))
        assertTrue(text.endsWith("END:VCARD\r\n"))
    }

    @Test
    fun `omits optional properties`() {
        val text = VCardSerializer.encode(VCardPayload(name = "Ana"))

        assertFalse(text.contains("TEL"))
        assertFalse(text.contains("EMAIL"))
    }

    @Test
    fun `escapes and unescapes special characters`() {
        val tricky = "Ana; Ruiz, \"La Jefa\"\\ \nsegunda línea"
        val decoded = VCardSerializer.decode(VCardSerializer.encode(VCardPayload(name = tricky)))

        assertEquals(tricky, (decoded as VCardParseResult.Success).payload.name)
    }

    @Test
    fun `folds long lines to 75 octets`() {
        val longName = "Ñandú ".repeat(40)
        val text = VCardSerializer.encode(VCardPayload(name = longName))

        assertTrue(text.contains("\r\n "))
        text.split("\r\n").forEach { line ->
            assertTrue("line too long: $line", line.toByteArray(Charsets.UTF_8).size <= 75)
        }
        val decoded = VCardSerializer.decode(text) as VCardParseResult.Success
        assertEquals(longName.trim(), decoded.payload.name.trim())
    }

    @Test
    fun `parses a foreign card without our properties`() {
        val text = """
            BEGIN:VCARD
            VERSION:3.0
            FN:Marta Ruiz
            TEL;TYPE=CELL:+34600111222
            EMAIL:marta@example.com
            END:VCARD
        """.trimIndent()

        val decoded = VCardSerializer.decode(text) as VCardParseResult.Success

        assertEquals("Marta Ruiz", decoded.payload.name)
        assertEquals("+34600111222", decoded.payload.phone)
        assertEquals(CardPalette.Default, decoded.payload.palette)
        assertEquals(VCardPayload.UNKNOWN_PROTOCOL_VERSION, decoded.payload.protocolVersion)
    }

    @Test
    fun `falls back to the default palette when colours are invalid`() {
        val text = VCardSerializer.encode(payload)
            .replace("#4F46E5", "not-a-colour")

        val decoded = VCardSerializer.decode(text) as VCardParseResult.Success

        assertEquals(CardPalette.Default, decoded.payload.palette)
    }

    @Test
    fun `derives the second colour when only the accent is present`() {
        val accent = RgbColor(0x10, 0xB9, 0x81)
        val text = VCardSerializer.encode(payload)
            .replace("X-TXOKA-CARD-ACCENT:#4F46E5", "X-TXOKA-CARD-ACCENT:${accent.hex}")
            .replace("X-TXOKA-CARD-ACCENT2:#06B6D4", "")

        val decoded = VCardSerializer.decode(text) as VCardParseResult.Success

        assertEquals(CardPalette.fromAccent(accent), decoded.payload.palette)
    }

    @Test
    fun `ignores unknown properties`() {
        val text = VCardSerializer.encode(payload)
            .replace("END:VCARD", "X-UNKNOWN;PARAM=1:whatever\r\nX-ANOTHER:value\r\nEND:VCARD")

        val decoded = VCardSerializer.decode(text) as VCardParseResult.Success

        assertEquals(payload, decoded.payload)
    }

    @Test
    fun `prefers the cell phone`() {
        val text = """
            BEGIN:VCARD
            VERSION:3.0
            FN:Ana
            TEL;TYPE=HOME:+34900000000
            TEL;TYPE=CELL:+34600111222
            END:VCARD
        """.trimIndent()

        val decoded = VCardSerializer.decode(text) as VCardParseResult.Success

        assertEquals("+34600111222", decoded.payload.phone)
    }

    @Test
    fun `fails without begin or name`() {
        assertTrue(VCardSerializer.decode("FN:Ana") is VCardParseResult.Failure)
        assertTrue(
            VCardSerializer.decode("BEGIN:VCARD\r\nVERSION:3.0\r\nEND:VCARD") is VCardParseResult.Failure,
        )
    }
}
