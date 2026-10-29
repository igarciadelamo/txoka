package es.igarciadelamo.txoka.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RgbColorTest {
    @Test
    fun `parses six digit hex with and without prefix`() {
        val expected = RgbColor(0x4F, 0x46, 0xE5)

        assertEquals(expected, RgbColor.parse("#4F46E5"))
        assertEquals(expected, RgbColor.parse("4F46E5"))
        assertEquals(expected, RgbColor.parse("  #4f46e5  "))
    }

    @Test
    fun `rejects malformed hex values`() {
        assertNull(RgbColor.parse(""))
        assertNull(RgbColor.parse("#12345"))
        assertNull(RgbColor.parse("#1234567"))
        assertNull(RgbColor.parse("#GGGGGG"))
    }

    @Test
    fun `round trips through hex`() {
        val color = RgbColor(0x06, 0xB6, 0xD4)

        assertEquals("#06B6D4", color.hex)
        assertEquals(color, RgbColor.parse(color.hex))
    }

    @Test
    fun `exposes opaque argb`() {
        assertEquals(0xFF4F46E5L, RgbColor(0x4F, 0x46, 0xE5).argb)
    }

    @Test
    fun `dark colours prefer light content`() {
        assertFalse(RgbColor(0, 0, 0).prefersDarkContent())
        assertFalse(RgbColor(0x4F, 0x46, 0xE5).prefersDarkContent())
        assertTrue(RgbColor(255, 255, 255).prefersDarkContent())
        assertTrue(RgbColor(0xFB, 0xBF, 0x24).prefersDarkContent())
    }

    @Test
    fun `lighten moves every channel towards white`() {
        val color = RgbColor(0x4F, 0x46, 0xE5)
        val lighter = color.lighten(0.25)

        assertTrue(lighter.red > color.red)
        assertTrue(lighter.green > color.green)
        assertTrue(lighter.blue >= color.blue)
        assertEquals(color, color.lighten(0.0))
    }
}
