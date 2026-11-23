package es.igarciadelamo.txoka.ui.components

import es.igarciadelamo.txoka.domain.profile.CardAppearance
import es.igarciadelamo.txoka.domain.profile.CardBackground
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.RgbColor
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CardMaterialTextureTest {
    @Test
    fun `native material pixels are opaque detailed and reproducible`() {
        materials.forEach { background ->
            val texture = CardMaterialTexture(WIDTH, HEIGHT)
            val first = texture.render(background, palette, CardMotion())
            assertEquals(WIDTH * HEIGHT, first.size)
            assertTrue("Transparent pixels in $background", first.all { it ushr 24 == 255 })
            assertTrue("Missing surface detail in $background", first.distinct().size > 500)
            assertArrayEquals(first, texture.render(background, palette, CardMotion()))
            assertArrayEquals(first, CardMaterialTexture(WIDTH, HEIGHT).render(background, palette, CardMotion()))
        }
    }

    @Test
    fun `each tilt axis changes the reflected light without replacing the material`() {
        materials.forEach { background ->
            val texture = CardMaterialTexture(WIDTH, HEIGHT)
            val initial = texture.render(background, palette, CardMotion())
            val yaw = texture.render(background, palette, CardMotion(.35f, 0f))
            val pitch = texture.render(background, palette, CardMotion(0f, .35f))
            assertTrue("Yaw has no visible effect on $background", meanColourDifference(initial, yaw) > .5)
            assertTrue("Pitch has no visible effect on $background", meanColourDifference(initial, pitch) > .5)
            assertTrue("Axes produce identical light on $background", meanColourDifference(yaw, pitch) > .5)
            assertArrayEquals(initial, texture.render(background, palette, CardMotion()))
        }
    }

    @Test
    fun `both palette colours and optional corner tints remain visible in every material`() {
        val firstChanged = palette.copy(accent = RgbColor(65, 45, 190))
        val secondChanged = palette.copy(accent2 = RgbColor(230, 180, 35))
        val fourCorners = palette.copy(
            appearance = CardAppearance(topRight = RgbColor(240, 190, 35), bottomLeft = RgbColor(65, 180, 70)),
        )
        materials.forEach { background ->
            val texture = CardMaterialTexture(WIDTH, HEIGHT)
            val original = texture.render(background, palette, CardMotion())
            listOf(firstChanged, secondChanged, fourCorners).forEach { changed ->
                val result = texture.render(background, changed, CardMotion())
                assertTrue("Lost a chosen colour in $background", meanColourDifference(original, result) > 2)
            }
        }
    }

    @Test
    fun `stained glass has bounded charcoal seams that stay attached to the panes through tilt`() {
        val texture = CardMaterialTexture(WIDTH, HEIGHT)
        val still = texture.render(CardBackground.STAINED_GLASS, palette, CardMotion())
        val tilted = texture.render(CardBackground.STAINED_GLASS, palette, CardMotion(.4f, -.3f))
        val lead = still.indices.filter { isCharcoal(still[it]) }
        assertTrue("Missing lead between glass panes", lead.size > still.size / 100)
        assertTrue("Lead obscures the glass", lead.size < still.size / 4)
        val unchangedLead = lead.count { isCharcoal(tilted[it]) }
        assertTrue("Tilt moves the pane geometry", unchangedLead > lead.size * .8)
        val rowPatterns = (8 until HEIGHT - 8 step 8).map { y ->
            (8 until WIDTH - 8).filter { x -> isCharcoal(still[y * WIDTH + x]) }
        }
        assertTrue("Pane boundaries form a repeating rectangular grid", rowPatterns.distinct().size > 8)
    }

    @Test
    fun `materials have distinct appearances for the same palette and pose`() {
        val texture = CardMaterialTexture(WIDTH, HEIGHT)
        val rendered = materials.map { texture.render(it, palette, CardMotion(.1f, -.1f)) }
        rendered.forEachIndexed { index, first ->
            rendered.drop(index + 1).forEach { second ->
                assertTrue("Native materials render alike", meanColourDifference(first, second) > 5)
            }
        }
    }

    @Test
    fun `pose sanitisation preserves deterministic pixels`() {
        val texture = CardMaterialTexture(WIDTH, HEIGHT)
        materials.forEach { background ->
            assertArrayEquals(
                texture.render(background, palette, CardMotion()),
                texture.render(background, palette, CardMotion(Float.NaN, Float.POSITIVE_INFINITY)),
            )
            assertArrayEquals(
                texture.render(background, palette, CardMotion(1f, -1f)),
                texture.render(background, palette, CardMotion(50f, -50f)),
            )
        }
    }

    @Test
    fun `material allocations reject dimensions outside the bounded texture size`() {
        listOf(0 to 1, 1 to 0, -1 to 40, 481 to 1, 1 to 321, Int.MAX_VALUE to Int.MAX_VALUE).forEach { (w, h) ->
            assertTrue("Accepted unbounded texture $w x $h", runCatching { CardMaterialTexture(w, h) }.isFailure)
        }
        assertEquals(1, CardMaterialTexture(1, 1).render(CardBackground.NACRE, palette, CardMotion()).size)
    }

    private fun isCharcoal(color: Int): Boolean {
        val channels = listOf(color shr 16 and 255, color shr 8 and 255, color and 255)
        return channels.max() < 48 && channels.max() - channels.min() < 16
    }

    private fun meanColourDifference(first: IntArray, second: IntArray): Double {
        val sum = first.indices.sumOf { index ->
            listOf(0, 8, 16).sumOf { shift ->
                abs((first[index] shr shift and 255) - (second[index] shr shift and 255))
            }
        }
        return sum / (first.size * 3.0)
    }

    companion object {
        private const val WIDTH = 240
        private const val HEIGHT = 152
        private val materials = listOf(CardBackground.STAINED_GLASS, CardBackground.NACRE, CardBackground.BRUSHED_METAL)
        private val palette = CardPalette(RgbColor(225, 93, 104), RgbColor(17, 166, 172))
    }
}
