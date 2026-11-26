package es.igarciadelamo.txoka.ui.components

import es.igarciadelamo.txoka.domain.profile.CardContrast
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.RgbColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The library avatar and the incoming sheet draw initials on top of the card gradient, so they
 * must follow the same contrast rule as the card itself instead of the theme surface.
 */
class CardContentColorTest {
    @Test
    fun `initials colour mirrors the card contrast rule`() {
        CardPalette.Presets.forEach { palette ->
            assertEquals(CardContrast.contentColor(palette).toComposeColor(), palette.cardContentColor())
        }
    }

    @Test
    fun `initials stay readable on every preset and on a custom accent`() {
        val palettes = CardPalette.Presets + CardPalette.fromAccent(RgbColor(0x4F, 0x46, 0xE5))

        palettes.forEach { palette ->
            val ratio = CardContrast.worstRatio(palette, CardContrast.contentColor(palette))
            assertTrue(
                "Initials on ${palette.accent.hex} -> ${palette.accent2.hex} score $ratio",
                ratio >= CardContrast.BODY_MIN_RATIO,
            )
        }
    }

    @Test
    fun `a light preset does not use the light surface colour`() {
        val lightSurface = RgbColor(0xFF, 0xFF, 0xFF)
        val palette = CardPalette.Presets[3]

        assertTrue(
            CardContrast.worstRatio(palette, lightSurface) < CardContrast.BODY_MIN_RATIO,
        )
        assertEquals(CardContrast.DarkContent.toComposeColor(), palette.cardContentColor())
    }
}
