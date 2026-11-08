package es.igarciadelamo.txoka.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class HsvColorTest {
    @Test
    fun `maps the primary colours to their hue`() {
        assertEquals(0f, HsvColor.fromRgb(RgbColor(0xFF, 0x00, 0x00)).hue, DELTA)
        assertEquals(120f, HsvColor.fromRgb(RgbColor(0x00, 0xFF, 0x00)).hue, DELTA)
        assertEquals(240f, HsvColor.fromRgb(RgbColor(0x00, 0x00, 0xFF)).hue, DELTA)
    }

    @Test
    fun `greys have no saturation`() {
        val grey = HsvColor.fromRgb(RgbColor(0x80, 0x80, 0x80))

        assertEquals(0f, grey.saturation, DELTA)
        assertEquals(0.5019608f, grey.value, DELTA)
    }

    @Test
    fun `black keeps the value at zero`() {
        val black = HsvColor.fromRgb(RgbColor(0x00, 0x00, 0x00))

        assertEquals(0f, black.value, DELTA)
        assertEquals(0f, black.saturation, DELTA)
    }

    @Test
    fun `round trips every channel combination`() {
        val colours = listOf(
            RgbColor(0x4F, 0x46, 0xE5),
            RgbColor(0x0E, 0xA5, 0xE9),
            RgbColor(0xF9, 0x73, 0x16),
            RgbColor(0xFB, 0xBF, 0x24),
            RgbColor(0x33, 0x41, 0x55),
            RgbColor(0xFF, 0xFF, 0xFF),
            RgbColor(0x00, 0x00, 0x00),
        )

        colours.forEach { colour ->
            assertEquals(colour, HsvColor.fromRgb(colour).toRgb())
        }
    }

    @Test
    fun `builds colours from hue saturation and value`() {
        assertEquals(RgbColor(0xFF, 0xFF, 0xFF), HsvColor(0f, 0f, 1f).toRgb())
        assertEquals(RgbColor(0x00, 0xFF, 0x00), HsvColor(120f, 1f, 1f).toRgb())
        assertEquals(RgbColor(0x00, 0x00, 0x00), HsvColor(200f, 1f, 0f).toRgb())
    }

    @Test
    fun `treats a full turn as the start of the wheel`() {
        assertEquals(HsvColor(0f, 1f, 1f).toRgb(), HsvColor(360f, 1f, 1f).toRgb())
    }

    @Test
    fun `rejects components outside their range`() {
        assertThrows(IllegalArgumentException::class.java) { HsvColor(361f, 0f, 0f) }
        assertThrows(IllegalArgumentException::class.java) { HsvColor(0f, 1.1f, 0f) }
        assertThrows(IllegalArgumentException::class.java) { HsvColor(0f, 0f, -0.1f) }
    }

    private companion object {
        const val DELTA = 0.001f
    }
}
