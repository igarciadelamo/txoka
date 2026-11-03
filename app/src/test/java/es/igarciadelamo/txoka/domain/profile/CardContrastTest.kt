package es.igarciadelamo.txoka.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardContrastTest {
    @Test
    fun `every preset reaches AA on both gradient stops`() {
        CardPalette.Presets.forEach { palette ->
            val content = CardContrast.contentColor(palette)
            val ratio = CardContrast.worstRatio(palette, content)

            assertTrue(
                "Preset ${palette.accent.hex} -> ${palette.accent2.hex} scores $ratio",
                ratio >= CardContrast.BODY_MIN_RATIO,
            )
        }
    }

    @Test
    fun `dark cards get light text and light cards get dark text`() {
        val dark = CardPalette(RgbColor(0x10, 0x10, 0x10), RgbColor(0x33, 0x41, 0x55))
        val light = CardPalette(RgbColor(0xFB, 0xBF, 0x24), RgbColor(0xF9, 0x73, 0x16))

        assertEquals(CardContrast.LightContent, CardContrast.contentColor(dark))
        assertEquals(CardContrast.DarkContent, CardContrast.contentColor(light))
    }

    @Test
    fun `picks the side with the best worst case`() {
        // The second stop decides: cyan is far too light for white text.
        val palette = CardPalette(RgbColor(0x4F, 0x46, 0xE5), RgbColor(0x22, 0xD3, 0xEE))

        assertEquals(CardContrast.DarkContent, CardContrast.contentColor(palette))
        assertTrue(
            CardContrast.worstRatio(palette, CardContrast.DarkContent) >
                CardContrast.worstRatio(palette, CardContrast.LightContent),
        )
    }

    @Test
    fun `custom accents stay readable on the derived gradient`() {
        val palette = CardPalette.fromAccent(RgbColor(0x4F, 0x46, 0xE5))
        val content = CardContrast.contentColor(palette)

        assertTrue(CardContrast.worstRatio(palette, content) >= 4.5)
    }
}
