package es.igarciadelamo.txoka.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardPaletteTest {
    @Test
    fun `derives a lighter second colour from the accent`() {
        val accent = RgbColor(0x4F, 0x46, 0xE5)
        val palette = CardPalette.fromAccent(accent)

        assertEquals(accent, palette.accent)
        assertNotEquals(palette.accent, palette.accent2)
        assertTrue(palette.accent2.relativeLuminance() > palette.accent.relativeLuminance())
    }

    @Test
    fun `derivation is deterministic`() {
        val accent = RgbColor(0x0E, 0xA5, 0xE9)

        assertEquals(CardPalette.fromAccent(accent), CardPalette.fromAccent(accent))
    }

    @Test
    fun `presets are unique`() {
        assertEquals(CardPalette.Presets.size, CardPalette.Presets.toSet().size)
    }

    @Test
    fun `default palette is part of the presets`() {
        assertTrue(CardPalette.Default in CardPalette.Presets)
    }
}
