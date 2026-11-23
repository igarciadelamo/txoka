package es.igarciadelamo.txoka.ui.profile

import es.igarciadelamo.txoka.domain.profile.HsvColor
import es.igarciadelamo.txoka.domain.profile.RgbColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColourDraftTest {
    @Test
    fun `darkening to black and back retains hue and saturation`() {
        val initial = ColourDraft(HsvColor(230f, .8f, .7f), "#000000")
        val black = initial.withHsv(initial.hsv.copy(value = 0f))
        val restored = black.withHsv(black.hsv.copy(value = .7f))

        assertEquals(230f, black.hsv.hue)
        assertEquals(.8f, black.hsv.saturation)
        assertEquals(initial.hsv.toRgb(), restored.color)
    }

    @Test
    fun `hex editing preserves a partial draft without corrupting the preview`() {
        val initial = ColourDraft.from(RgbColor(79, 70, 229))
        val partial = initial.withHex("#FF0")
        val valid = partial.withHex("#FF00AA")

        assertFalse(partial.validHex)
        assertEquals(initial.color, partial.color)
        assertEquals("#FF0", partial.hex)
        assertTrue(valid.validHex)
        assertEquals(RgbColor(255, 0, 170), valid.color)
    }

    @Test
    fun `black and grey hex edits retain the hue for further adjustment`() {
        val initial = ColourDraft(HsvColor(280f, .65f, .8f), "#000000")
        val black = initial.withHex("#000000")
        val grey = black.withHex("#808080")

        assertEquals(280f, black.hsv.hue)
        assertEquals(.65f, black.hsv.saturation)
        assertEquals(280f, grey.hsv.hue)
        assertEquals(0f, grey.hsv.saturation)
    }

    @Test
    fun `repeated picker sessions start from their own chosen colour`() {
        val first = RgbColor(79, 70, 229)
        val second = RgbColor(240, 80, 42)
        repeat(20) {
            val opening = if (it % 2 == 0) first else second
            val draft = ColourDraft.from(opening)
            val modified = draft.withHsv(draft.hsv.copy(value = .3f)).withHex("#")
            assertEquals(opening, draft.color)
            assertFalse(modified.validHex)
            assertEquals(opening.hex, ColourDraft.from(opening).hex)
        }
    }
}
