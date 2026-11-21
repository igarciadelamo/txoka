package es.igarciadelamo.txoka.ui.profile

import es.igarciadelamo.txoka.domain.profile.CardAppearance
import es.igarciadelamo.txoka.domain.profile.CardBackground
import es.igarciadelamo.txoka.domain.profile.CardContrast
import es.igarciadelamo.txoka.domain.profile.CardEffect
import es.igarciadelamo.txoka.domain.profile.CardMosaic
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.CardTextStyle
import es.igarciadelamo.txoka.domain.profile.RgbColor
import es.igarciadelamo.txoka.domain.profile.TextFinish
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceEditorStateTest {
    @Test
    fun `name and contact colours are independent and preserve all other layers`() {
        val initial = CardPalette.Default.copy(
            appearance = CardAppearance(
                background = CardBackground.HOLOGRAM,
                mosaic = CardMosaic.NATURE_FISH,
                effect = CardEffect.WATER,
                name = CardTextStyle(finish = TextFinish.METAL),
            ),
        )
        val nameColor = RgbColor(230, 170, 80)
        val dataColor = RgbColor(240, 240, 255)
        val nameChanged = initial.withColour(CardColourTarget.NAME, nameColor)
        val bothChanged = nameChanged.withColour(CardColourTarget.DATA, dataColor)

        assertEquals(nameColor, bothChanged.colourAt(CardColourTarget.NAME))
        assertEquals(dataColor, bothChanged.colourAt(CardColourTarget.DATA))
        assertEquals(initial.accent, bothChanged.accent)
        assertEquals(initial.accent2, bothChanged.accent2)
        assertEquals(initial.appearance?.background, bothChanged.appearance?.background)
        assertEquals(initial.appearance?.mosaic, bothChanged.appearance?.mosaic)
        assertEquals(initial.appearance?.effect, bothChanged.appearance?.effect)
        assertEquals(TextFinish.METAL, bothChanged.appearance?.name?.finish)
    }

    @Test
    fun `changing either gradient colour does not derive or overwrite the other`() {
        val first = RgbColor(250, 210, 150)
        val second = RgbColor(20, 80, 190)
        val initial = CardPalette.Default
        val changed = initial.withColour(CardColourTarget.FIRST, first).withColour(CardColourTarget.SECOND, second)

        assertEquals(first, changed.accent)
        assertEquals(second, changed.accent2)
        assertEquals(CardAppearance(), changed.appearance)
    }

    @Test
    fun `category filters never insert the selected mosaic from a different category`() {
        assertTrue(MosaicCategory.FISH.includes(CardMosaic.NATURE_FISH))
        assertFalse(MosaicCategory.FISH.includes(CardMosaic.NATURE_REPTILES))
        assertFalse(MosaicCategory.REPTILES.includes(CardMosaic.NATURE_FISH))
        assertTrue(MosaicCategory.GEOMETRIC.includes(CardMosaic.TRIANGLES))
        assertFalse(MosaicCategory.GEOMETRIC.includes(CardMosaic.NATURE_BIRDS))
    }

    @Test
    fun `all mosaics have a translated label and at least one category`() {
        CardMosaic.entries.forEach { mosaic ->
            assertTrue(mosaic.label != 0)
            assertTrue(MosaicCategory.entries.any { it.includes(mosaic) })
        }
    }

    @Test
    fun `first mosaic or effect edit preserves the automatic dark text of a light legacy card`() {
        val legacy = CardPalette(RgbColor(255, 255, 255), RgbColor(245, 235, 225))
        val foreground = CardContrast.contentColor(legacy)
        val explicit = legacy.editableAppearance()
        val mosaic = legacy.copy(appearance = explicit.copy(mosaic = CardMosaic.TRIANGLES))
        val effect = legacy.copy(appearance = explicit.copy(effect = CardEffect.WATER))

        assertTrue(foreground.relativeLuminance() < .1)
        listOf(mosaic, effect).forEach { changed ->
            assertEquals(foreground, changed.colourAt(CardColourTarget.NAME))
            assertEquals(foreground, changed.colourAt(CardColourTarget.DATA))
            assertEquals(legacy.accent, changed.accent)
            assertEquals(legacy.accent2, changed.accent2)
        }
    }
}
