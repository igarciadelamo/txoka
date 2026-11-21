package es.igarciadelamo.txoka.ui.components

import es.igarciadelamo.txoka.domain.profile.CardBackground
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

class CardMaterialCellsTest {
    @Test
    fun `bundled atlas identifies every source pane including the cropped edge fragments`() {
        val labels = CardMaterialCells.fromBytes(asset("glass-cells.u8")).raster(WIDTH, HEIGHT)
        val counts = labels.toList().groupingBy { it }.eachCount()
        assertEquals((0 until CardMaterialCells.CELL_COUNT).toSet(), counts.keys)
        assertTrue("A source pane disappeared", counts.values.all { it > 200 })
        assertTrue("Distinct source panes collapsed together", counts.values.all { it < 15_000 })
        val sourceCentres = listOf(
            60 to 60, 190 to 35, 335 to 115, 460 to 45, 665 to 50, 845 to 30,
            940 to 50, 10 to 170, 145 to 195, 550 to 165, 780 to 175, 915 to 280,
            20 to 355, 150 to 405, 325 to 315, 300 to 445, 560 to 355, 780 to 390,
            930 to 420, 40 to 520, 220 to 545, 410 to 515, 630 to 515, 830 to 535,
        )
        sourceCentres.forEachIndexed { id, (x, y) ->
            assertEquals("Wrong optical identity at source pane $id", id, labels[y / 2 * WIDTH + x / 2])
        }
    }

    @Test
    fun `cell loader rejects malformed atlases and owns its source and raster buffers`() {
        val bytes = asset("glass-cells.u8")
        val atlas = CardMaterialCells.fromBytes(bytes)
        val expected = atlas.raster(WIDTH, HEIGHT)
        bytes.fill(0)
        val raster = atlas.raster(WIDTH, HEIGHT)
        raster.fill(0)
        assertArrayEquals(expected, atlas.raster(WIDTH, HEIGHT))
        listOf(ByteArray(0), ByteArray(WIDTH * HEIGHT - 1), bytes, ByteArray(WIDTH * HEIGHT) { 255.toByte() })
            .forEach { assertTrue(runCatching { CardMaterialCells.fromBytes(it) }.isFailure) }
        val scaled = atlas.raster(WIDTH / 2, HEIGHT / 3)
        assertEquals(expected[WIDTH + 1], scaled[0])
        assertTrue(runCatching { atlas.raster(0, HEIGHT) }.isFailure)
    }

    @Test
    fun `every bundled RGB artwork survives neutral rendering byte for byte`() {
        listOf(
            "glass" to CardBackground.STAINED_GLASS,
            "nacre" to CardBackground.NACRE,
            "metal" to CardBackground.BRUSHED_METAL,
        ).forEach { (name, background) ->
            val artwork = CardMaterialArtwork.fromBytes(asset("$name-color.rgb"))
            val texture = CardMaterialTexture(WIDTH, HEIGHT, artwork = artwork)
            assertArrayEquals(
                "Approved RGB artwork changed in $background",
                artwork.raster(WIDTH, HEIGHT),
                texture.render(background, CardMaterialArtwork.ReferencePalette, CardMotion()),
            )
        }
    }

    @Test
    fun `real glass pane IDs change both light axes while neutral RGB remains exact`() {
        val artwork = CardMaterialArtwork.fromBytes(asset("glass-color.rgb"))
        val cells = CardMaterialCells.fromBytes(asset("glass-cells.u8"))
        val panes = CardMaterialTexture(WIDTH, HEIGHT, artwork = artwork, cells = cells)
        val uniform = CardMaterialTexture(WIDTH, HEIGHT, artwork = artwork)
        val palette = CardMaterialArtwork.ReferencePalette
        val background = CardBackground.STAINED_GLASS
        val expected = artwork.raster(WIDTH, HEIGHT)
        listOf(CardMotion(.35f, 0f), CardMotion(0f, -.35f)).forEach { motion ->
            val perPane = panes.render(background, palette, motion)
            val common = uniform.render(background, palette, motion)
            val difference = perPane.indices.sumOf { index ->
                abs((perPane[index] and 255) - (common[index] and 255))
            } / perPane.size.toDouble()
            assertTrue("Source pane orientation ignored for $motion", difference > .5)
            assertArrayEquals(expected, panes.render(background, palette, CardMotion()))
        }
    }

    private fun asset(name: String): ByteArray =
        listOf(File("src/main/assets/card-materials/$name"), File("app/src/main/assets/card-materials/$name"))
            .first { it.isFile }.readBytes()

    companion object {
        private const val WIDTH = 480
        private const val HEIGHT = 303
    }
}
