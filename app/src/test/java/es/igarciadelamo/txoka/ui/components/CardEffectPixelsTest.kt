package es.igarciadelamo.txoka.ui.components

import es.igarciadelamo.txoka.domain.profile.CardEffect
import es.igarciadelamo.txoka.domain.profile.CardPalette
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CardEffectPixelsTest {
    @Test
    fun `fire is translucent with rising density and a clear upper border`() {
        val width = 72
        val height = 46
        val pixels = render(CardEffect.FIRE, width, height)
        val top = pixels.take(width * 3).map { it ushr 24 }.average()
        val bottom = pixels.takeLast(width * 3).map { it ushr 24 }.average()
        assertTrue(top < 2)
        assertTrue(bottom > 70)
        assertTrue(pixels.all { (it ushr 24) < 220 })
        assertTrue(pixels.count { (it ushr 24) in 1..200 } > width)
    }

    @Test
    fun `mist has diffuse volume without opaque areas`() {
        val pixels = render(CardEffect.WIND)
        val alphas = pixels.map { it ushr 24 }
        assertTrue(alphas.average() in 5.0..100.0)
        assertTrue(alphas.max() <= 143)
        assertTrue(alphas.distinct().size > 40)
    }

    @Test
    fun `flowing materials respond to time and each motion axis independently`() {
        listOf(CardEffect.FIRE, CardEffect.WIND).forEach { effect ->
            val initial = render(effect)
            assertFalse(initial.contentEquals(render(effect, time = 1.3f)))
            assertFalse(initial.contentEquals(render(effect, motion = CardMotion(.25f, 0f))))
            assertFalse(initial.contentEquals(render(effect, motion = CardMotion(0f, .25f))))
            assertArrayEquals(initial, render(effect))
        }
    }

    @Test
    fun `invalid pose and clock values cannot corrupt the material field`() {
        assertArrayEquals(
            render(CardEffect.WIND),
            render(CardEffect.WIND, motion = CardMotion(Float.NaN, Float.POSITIVE_INFINITY), time = Float.NaN),
        )
        assertTrue(render(CardEffect.NONE).all { it == 0 })
    }

    @Test(expected = IllegalArgumentException::class)
    fun `material buffers reject unbounded allocation requests`() {
        render(CardEffect.FIRE, width = 4000)
    }

    @Test
    fun `water has a moving level with a tilt response on both axes`() {
        val left = waterLevel(0f, 227f, CardMotion(.5f, 0f), 0f)
        val right = waterLevel(360f, 227f, CardMotion(.5f, 0f), 0f)
        assertTrue(left - right > 25)
        val center = waterLevel(180f, 227f, CardMotion(), 0f)
        assertTrue(center in 85f..110f)
        assertTrue(abs(center - waterLevel(180f, 227f, CardMotion(0f, .5f), 0f)) > 5)
        assertNotEquals(center, waterLevel(180f, 227f, CardMotion(), 1.4f))
    }

    @Test
    fun `holographic diffraction responds to small tilts on both axes`() {
        val first = CardPalette.Default.accent.argb.toInt()
        val second = CardPalette.Default.accent2.argb.toInt()
        val initial = foilColor(.4f, CardMotion(), first, second)
        assertNotEquals(initial, foilColor(.4f, CardMotion(.1f, 0f), first, second))
        assertNotEquals(initial, foilColor(.4f, CardMotion(0f, .1f), first, second))
    }

    private fun render(
        effect: CardEffect,
        width: Int = 64,
        height: Int = 40,
        motion: CardMotion = CardMotion(),
        time: Float = 0f,
    ): IntArray = CardEffectPixels.render(effect, width, height, motion, time, CardPalette.Default)
}
