package es.igarciadelamo.txoka.ui.components

import es.igarciadelamo.txoka.domain.profile.CardBackground
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.RgbColor
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CardMaterialArtworkTest {
    @Test
    fun `reference palette is the palette used by the approved artwork`() {
        assertEquals(
            CardPalette(RgbColor(225, 93, 104), RgbColor(17, 166, 172)),
            CardMaterialArtwork.ReferencePalette,
        )
    }

    @Test
    fun `neutral reference pose reproduces every source RGB byte at native resolution`() {
        val artwork = CardMaterialArtwork.fromBytes(sourceBytes())
        materials.forEach { background ->
            val pixels = texture(artwork).render(background, CardMaterialArtwork.ReferencePalette, CardMotion())
            assertArrayEquals("Artwork changed at rest in $background", sourcePixels(), pixels)
        }
    }

    @Test
    fun `artwork owns an immutable copy of its RGB bytes`() {
        val bytes = sourceBytes()
        val artwork = CardMaterialArtwork.fromBytes(bytes)
        bytes.fill(0)
        materials.forEach { background ->
            val pixels = texture(artwork).render(background, CardMaterialArtwork.ReferencePalette, CardMotion())
            assertArrayEquals("Caller mutated artwork in $background", sourcePixels(), pixels)
        }
    }

    @Test
    fun `artwork rejects byte buffers that are not exactly the fixed RGB size`() {
        listOf(0, 1, BYTE_COUNT - 1, BYTE_COUNT + 1, BYTE_COUNT * 2).forEach { size ->
            assertTrue(
                "Accepted RGB buffer of $size bytes",
                runCatching { CardMaterialArtwork.fromBytes(ByteArray(size)) }.isFailure,
            )
        }
    }

    @Test
    fun `both tilt axes relight real artwork pixels and returning to neutral restores the source`() {
        val artwork = CardMaterialArtwork.fromBytes(sourceBytes())
        val original = sourcePixels()
        materials.forEach { background ->
            val material = texture(artwork)
            val yaw = material.render(background, CardMaterialArtwork.ReferencePalette, CardMotion(.35f, 0f))
            val pitch = material.render(background, CardMaterialArtwork.ReferencePalette, CardMotion(0f, -.35f))
            assertTrue("Artwork ignores yaw in $background", meanDifference(original, yaw) > .5)
            assertTrue("Artwork ignores pitch in $background", meanDifference(original, pitch) > .5)
            assertTrue("Both axes give the same reflection in $background", meanDifference(yaw, pitch) > .5)
            assertArrayEquals(
                "Artwork drifts after tilt in $background",
                original,
                material.render(background, CardMaterialArtwork.ReferencePalette, CardMotion()),
            )
        }
    }

    @Test
    fun `changing palette recolours glass while preserving white glints and charcoal seams`() {
        val artwork = CardMaterialArtwork.fromBytes(sourceBytes())
        val original = sourcePixels()
        val chosen = CardPalette(RgbColor(91, 63, 220), RgbColor(54, 164, 67))
        materials.forEach { background ->
            val result = texture(artwork).render(background, chosen, CardMotion())
            assertNotEquals("First colour was ignored in $background", original[CORAL_SAMPLE], result[CORAL_SAMPLE])
            assertNotEquals("Second colour was ignored in $background", original[CYAN_SAMPLE], result[CYAN_SAMPLE])
            original.indices.filter { original[it] == WHITE || original[it] == CHARCOAL }.forEach { index ->
                assertEquals("Neutral artwork pigment was recoloured in $background", original[index], result[index])
            }
        }
    }

    @Test
    fun `tilt keeps the source seam anchored to its original image coordinates`() {
        val artwork = CardMaterialArtwork.fromBytes(sourceBytes())
        materials.forEach { background ->
            val material = texture(artwork)
            listOf(CardMotion(.35f, 0f), CardMotion(0f, -.35f)).forEach { motion ->
                val pixels = material.render(background, CardMaterialArtwork.ReferencePalette, motion)
                val darkSeam = (0 until HEIGHT).count { y ->
                    val pixel = pixels[y * WIDTH + WIDTH / 2]
                    maxOf(pixel shr 16 and 255, pixel shr 8 and 255, pixel and 255) < 64
                }
                assertTrue("Source geometry moved with tilt in $background", darkSeam > HEIGHT * .9)
            }
        }
    }

    private fun texture(artwork: CardMaterialArtwork): CardMaterialTexture =
        CardMaterialTexture(WIDTH, HEIGHT, relief = null, artwork = artwork)

    private fun sourcePixels(): IntArray =
        IntArray(WIDTH * HEIGHT) { index -> sourcePixel(index % WIDTH, index / WIDTH) }

    private fun sourceBytes(): ByteArray {
        val bytes = ByteArray(BYTE_COUNT)
        sourcePixels().forEachIndexed { index, pixel ->
            bytes[index * 3] = (pixel shr 16 and 255).toByte()
            bytes[index * 3 + 1] = (pixel shr 8 and 255).toByte()
            bytes[index * 3 + 2] = (pixel and 255).toByte()
        }
        return bytes
    }

    private fun sourcePixel(x: Int, y: Int): Int = when {
        x in WIDTH / 2 - 2..WIDTH / 2 + 2 -> CHARCOAL
        x in 40..140 && y in 28..35 -> WHITE
        x in 350..400 && y in 245..251 -> WHITE
        x < WIDTH / 2 -> CORAL
        else -> CYAN
    }

    private fun meanDifference(first: IntArray, second: IntArray): Double {
        val sum = first.indices.sumOf { index ->
            abs((first[index] shr 16 and 255) - (second[index] shr 16 and 255)) +
                abs((first[index] shr 8 and 255) - (second[index] shr 8 and 255)) +
                abs((first[index] and 255) - (second[index] and 255))
        }
        return sum / (first.size * 3.0)
    }

    companion object {
        private const val WIDTH = 480
        private const val HEIGHT = 303
        private const val BYTE_COUNT = WIDTH * HEIGHT * 3
        private const val CORAL = 0xFFE15D68.toInt()
        private const val CYAN = 0xFF11A6AC.toInt()
        private const val WHITE = -1
        private const val CHARCOAL = 0xFF1B1B1B.toInt()
        private const val CORAL_SAMPLE = 100 * WIDTH + 80
        private const val CYAN_SAMPLE = 180 * WIDTH + 360
        private val materials = listOf(CardBackground.STAINED_GLASS, CardBackground.NACRE, CardBackground.BRUSHED_METAL)
    }
}
