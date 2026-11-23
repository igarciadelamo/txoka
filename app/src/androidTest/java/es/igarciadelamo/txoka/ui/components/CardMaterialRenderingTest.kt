package es.igarciadelamo.txoka.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.igarciadelamo.txoka.domain.profile.CardAppearance
import es.igarciadelamo.txoka.domain.profile.CardBackground
import es.igarciadelamo.txoka.domain.profile.CardEffect
import es.igarciadelamo.txoka.domain.profile.CardMosaic
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.RgbColor
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class CardMaterialRenderingTest {
    @Test
    fun bundledGlassAndShellAtlasesContainUsableHeightDetail() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        listOf(CardBackground.STAINED_GLASS, CardBackground.NACRE).forEach { background ->
            val relief = CardMaterialReliefAssets.load(context, background)
            assertNotNull("Missing or invalid bundled optical atlas for $background", relief)
            val levels = (0 until 256).map { index ->
                requireNotNull(relief).sample(index % 16 / 15f, index / 16 / 15f)
            }
            assertTrue("Flat optical atlas for $background", levels.max() - levels.min() > .15f)
        }
    }

    @Test
    fun glassCellAtlasRetainsAllTwentyFourSourcePanes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cells = CardMaterialCellsAssets.load(context)
        assertNotNull("Missing canonical glass cell atlas", cells)
        val labels = requireNotNull(cells).raster(480, 303)
        val areas = labels.toList().groupingBy { it }.eachCount()
        assertEquals(CardMaterialCells.CELL_COUNT, areas.size)
        assertTrue("Most glass collapsed into one optical plane", requireNotNull(areas.values.maxOrNull()) < 30_000)
        assertTrue("Glass cell atlas contains degenerate panes", areas.values.all { it > 200 })
    }

    @Test
    fun neutralCanvasReproducesApprovedRgbArtworkWithoutAnExtraScrim() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        materials.forEach { background ->
            val artwork = CardMaterialArtworkAssets.load(context, background)
            assertNotNull("Missing approved RGB artwork for $background", artwork)
            val expected = requireNotNull(artwork).raster(WIDTH, HEIGHT)
            assertArrayEquals(
                "Canvas altered the approved artwork for $background",
                expected,
                draw(CardAppearance(background = background)),
            )
        }
    }

    @Test
    fun nativeMaterialsFillTheCanvasAndReflectBothMotionAxes() {
        materials.forEach { background ->
            val appearance = CardAppearance(background = background)
            val still = draw(appearance)
            val yaw = draw(appearance, CardMotion(.35f, 0f))
            val pitch = draw(appearance, CardMotion(0f, .35f))
            assertTrue("Transparent surface in $background", still.all { Color.alpha(it) == 255 })
            assertTrue("Canvas lost yaw lighting in $background", difference(still, yaw) > .5)
            assertTrue("Canvas lost pitch lighting in $background", difference(still, pitch) > .5)
        }
    }

    @Test
    fun mosaicMaskChangesOnlyItsCoveredHalfOnEveryNativeMaterial() {
        val maskPixels = IntArray(WIDTH * HEIGHT) { index ->
            when {
                index % WIDTH < WIDTH / 4 -> Color.WHITE
                index % WIDTH < WIDTH / 2 -> Color.argb(128, 255, 255, 255)
                else -> Color.TRANSPARENT
            }
        }
        val colorMask = Bitmap.createBitmap(maskPixels, WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val masks = listOf(colorMask, colorMask.extractAlpha())
        try {
            materials.forEach { background ->
                val appearance = CardAppearance(background = background)
                val plain = draw(appearance)
                val results = masks.map { mask ->
                    val decorated = draw(appearance.copy(mosaic = CardMosaic.SQUARES), mask = mask)
                    var changed = 0
                    plain.indices.forEach { index ->
                        if (index % WIDTH >= WIDTH / 2) {
                            assertEquals(
                                "Mask leaked into uncovered $background / ${mask.config}",
                                plain[index],
                                decorated[index],
                            )
                        } else if (plain[index] != decorated[index]) {
                            changed++
                        }
                    }
                    assertTrue("Mosaic disappeared on $background / ${mask.config}", changed > WIDTH * HEIGHT / 4)
                    decorated
                }
                results.first().indices.forEach { index ->
                    listOf(0, 8, 16, 24).forEach { shift ->
                        val first = results.first()[index] shr shift and 255
                        val second = results.last()[index] shr shift and 255
                        assertTrue("Mask alpha differs on $background at $index", abs(first - second) <= 1)
                    }
                }
            }
        } finally {
            masks.forEach(Bitmap::recycle)
        }
    }

    @Test
    fun effectPixelsRemainASeparateOverlayOnEveryNativeMaterial() {
        val effectPixels = IntArray(WIDTH * HEIGHT) { index ->
            if (index / WIDTH >= HEIGHT / 2) Color.argb(100, 230, 245, 255) else Color.TRANSPARENT
        }
        val effect = Bitmap.createBitmap(effectPixels, WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        materials.forEach { background ->
            val appearance = CardAppearance(background = background, effect = CardEffect.WIND)
            val plain = draw(appearance)
            val decorated = draw(appearance, effect = effect)
            val half = WIDTH * HEIGHT / 2
            assertTrue(plain.take(half) == decorated.take(half))
            assertTrue("Effect disappeared on $background", difference(plain, decorated, half) > 5)
        }
        effect.recycle()
    }

    private fun draw(
        appearance: CardAppearance,
        motion: CardMotion = CardMotion(),
        mask: Bitmap? = null,
        effect: Bitmap? = null,
    ): IntArray {
        val chosenPalette = palette.copy(appearance = appearance)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val relief = CardMaterialReliefAssets.load(context, appearance.background)
        val artwork = CardMaterialArtworkAssets.load(context, appearance.background)
        val cells = if (appearance.background == CardBackground.STAINED_GLASS) {
            CardMaterialCellsAssets.load(context)
        } else {
            null
        }
        val surface = CardMaterialTexture(WIDTH, HEIGHT, relief, artwork, cells)
            .render(appearance.background, chosenPalette, motion)
        val source = Bitmap.createBitmap(surface, WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        CardMaterialPainter().draw(
            Canvas(bitmap), WIDTH.toFloat(), HEIGHT.toFloat(), chosenPalette, appearance, motion, 0f,
            mask, effect, source,
        )
        val pixels = IntArray(WIDTH * HEIGHT)
        bitmap.getPixels(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT)
        source.recycle()
        bitmap.recycle()
        return pixels
    }

    private fun difference(first: IntArray, second: IntArray, start: Int = 0): Double {
        val sum = (start until first.size).sumOf { index ->
            listOf(0, 8, 16).sumOf { shift ->
                abs((first[index] shr shift and 255) - (second[index] shr shift and 255))
            }
        }
        return sum / ((first.size - start) * 3.0)
    }

    companion object {
        private const val WIDTH = 240
        private const val HEIGHT = 152
        private val materials = listOf(CardBackground.STAINED_GLASS, CardBackground.NACRE, CardBackground.BRUSHED_METAL)
        private val palette = CardPalette(RgbColor(225, 93, 104), RgbColor(17, 166, 172))
    }
}
