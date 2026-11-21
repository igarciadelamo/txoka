package es.igarciadelamo.txoka.ui.components

import es.igarciadelamo.txoka.domain.profile.CardBackground
import es.igarciadelamo.txoka.domain.profile.CardPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CardMaterialReliefTest {
    @Test
    fun `raw optical atlases require exactly one bounded unsigned byte per height`() {
        listOf(0, 65_535, 65_537).forEach { size ->
            assertTrue(runCatching { CardMaterialRelief.fromBytes(ByteArray(size)) }.isFailure)
        }
        val white = CardMaterialRelief.fromBytes(ByteArray(65_536) { 255.toByte() })
        assertEquals(1f, white.sample(.37f, .82f), 0f)
        assertTrue(runCatching { CardMaterialRelief(FloatArray(65_536) { Float.NaN }) }.isFailure)
        assertTrue(runCatching { CardMaterialRelief(FloatArray(65_536) { -1f }) }.isFailure)
    }

    @Test
    fun `height field owns immutable samples and clamps at the card boundary`() {
        val values = FloatArray(65_536) { .5f }
        val relief = CardMaterialRelief(values)
        values.fill(0f)
        assertEquals(.5f, relief.sample(.2f, .8f), 0f)
        assertEquals(.5f, relief.sample(-.2f, 1.8f), 0f)
    }

    @Test
    fun `optical relief changes real glass and shell pixels while both tilt axes remain active`() {
        val values = FloatArray(65_536) { index ->
            val x = index % 256 / 255f
            val y = index / 256 / 255f
            (x * x + y * .6f).coerceIn(0f, 1f)
        }
        val relief = CardMaterialRelief(values)
        listOf(CardBackground.STAINED_GLASS, CardBackground.NACRE).forEach { background ->
            val texture = CardMaterialTexture(160, 100, relief)
            val still = texture.render(background, CardPalette.Default, CardMotion())
            val procedural = CardMaterialTexture(160, 100).render(background, CardPalette.Default, CardMotion())
            assertTrue("Ignored optical atlas on $background", difference(still, procedural) > 1)
            assertTrue(
                "Relief lost yaw on $background",
                difference(still, texture.render(background, CardPalette.Default, CardMotion(.3f, 0f))) > .5,
            )
            assertTrue(
                "Relief lost pitch on $background",
                difference(still, texture.render(background, CardPalette.Default, CardMotion(0f, .3f))) > .5,
            )
        }
    }

    private fun difference(first: IntArray, second: IntArray): Double {
        val total = first.indices.sumOf { index ->
            listOf(0, 8, 16).sumOf { shift ->
                abs((first[index] shr shift and 255) - (second[index] shr shift and 255))
            }
        }
        return total / (first.size * 3.0)
    }
}
