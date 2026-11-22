package es.igarciadelamo.txoka.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.igarciadelamo.txoka.domain.profile.CardAppearance
import es.igarciadelamo.txoka.domain.profile.CardBackground
import es.igarciadelamo.txoka.domain.profile.CardMosaic
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.RgbColor
import es.igarciadelamo.txoka.ui.components.artwork.CardMosaicMasks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.Closeable
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class NativeSvgMaterialIntegrationTest {
    @Test(timeout = MAX_DURATION_MILLIS)
    fun everySuppliedSvgKeepsItsNativeMaterialAndBothLightAxes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mosaics = CardMosaic.entries.filter { it.assetId?.startsWith("geo-") == true }
        assertEquals("The supplied SVG catalogue changed", EXPECTED_MOSAICS, mosaics.size)
        NativeCanvasFixture().use { fixture ->
            mosaics.forEach { mosaic ->
                val mask = requireNotNull(CardMosaicMasks.mask(context, mosaic, WIDTH, HEIGHT)) {
                    "Missing bundled SVG mask for $mosaic"
                }
                val alpha = pixels(mask).map(Color::alpha)
                val covered = alpha.indices.filter { alpha[it] >= COVERED_ALPHA }
                val uncovered = alpha.indices.filter { alpha[it] == 0 }
                assertTrue("SVG lost its first ink: $mosaic", covered.isNotEmpty())
                assertTrue("SVG lost its second ink: $mosaic", uncovered.isNotEmpty())
                val results = materials.map { background ->
                    verifyComposition(fixture, background, mosaic, mask, covered, uncovered)
                }
                results.zipWithNext().forEach { (first, second) ->
                    assertTrue("SVG replaced the material: $mosaic", difference(first, second) > MIN_VISIBLE_DELTA)
                }
            }
        }
    }

    private fun verifyComposition(
        fixture: NativeCanvasFixture,
        background: CardBackground,
        mosaic: CardMosaic,
        mask: Bitmap,
        covered: List<Int>,
        uncovered: List<Int>,
    ): IntArray {
        val appearance = CardAppearance(background = background, mosaic = mosaic)
        val frames = poses.map { pose ->
            val surface = fixture.surfaces.getValue(background).getValue(pose)
            val plain = fixture.draw(appearance.copy(mosaic = CardMosaic.NONE), pose, null, surface)
            val decorated = fixture.draw(appearance, pose, mask, surface)
            val uniform = fixture.draw(appearance, pose, mask, fixture.uniform)
            val uniformPlain = fixture.draw(appearance.copy(mosaic = CardMosaic.NONE), pose, null, fixture.uniform)
            val label = "$background / $mosaic / $pose"
            assertTrue("Transparent composed card: $label", decorated.all { Color.alpha(it) == 255 })
            uncovered.forEach { index ->
                assertEquals("Mosaic changed an uncovered pixel: $label", plain[index], decorated[index])
            }
            assertTrue("Mosaic disappeared: $label", difference(plain, decorated, covered) > MIN_VISIBLE_DELTA)
            val sourceTexture = difference(plain, uniformPlain, covered)
            val remainingTexture = difference(decorated, uniform, covered)
            assertTrue("Missing source texture: $label", sourceTexture > MIN_VISIBLE_DELTA)
            assertTrue("Opaque SVG hid the material: $label", remainingTexture / sourceTexture > MIN_RETAINED_TEXTURE)
            decorated
        }
        frames.drop(1).forEachIndexed { index, moved ->
            assertTrue(
                "SVG lost light axis ${index + 1}: $background / $mosaic",
                difference(frames.first(), moved, covered) > MIN_MOTION_DELTA,
            )
        }
        return frames.first()
    }

    private class NativeCanvasFixture : Closeable {
        private val context = InstrumentationRegistry.getInstrumentation().targetContext
        private val painter = CardMaterialPainter()
        private val target = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        private val canvas = Canvas(target)
        val uniform: Bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.GRAY)
        }
        val surfaces: Map<CardBackground, Map<CardMotion, Bitmap>> = materials.associateWith { background ->
            val artwork = requireNotNull(CardMaterialArtworkAssets.load(context, background)) {
                "Missing approved RGB artwork for $background"
            }
            val relief = CardMaterialReliefAssets.load(context, background)
            val cells = if (background == CardBackground.STAINED_GLASS) CardMaterialCellsAssets.load(context) else null
            val texture = CardMaterialTexture(WIDTH, HEIGHT, relief, artwork, cells)
            val appearance = CardAppearance(background = background)
            poses.associateWith { pose ->
                val values = texture.render(background, palette.copy(appearance = appearance), pose)
                Bitmap.createBitmap(values, WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
            }
        }

        fun draw(appearance: CardAppearance, pose: CardMotion, mask: Bitmap?, surface: Bitmap): IntArray {
            painter.draw(
                canvas,
                WIDTH.toFloat(),
                HEIGHT.toFloat(),
                palette.copy(appearance = appearance),
                appearance,
                pose,
                0f,
                mask,
                null,
                surface,
            )
            return pixels(target)
        }

        override fun close() {
            surfaces.values.flatMap { it.values }.forEach(Bitmap::recycle)
            uniform.recycle()
            target.recycle()
        }
    }

    companion object {
        private const val WIDTH = 192
        private const val HEIGHT = 121
        private const val EXPECTED_MOSAICS = 20
        private const val MAX_DURATION_MILLIS = 120_000L
        private const val COVERED_ALPHA = 224
        private const val MIN_VISIBLE_DELTA = 1.0
        private const val MIN_MOTION_DELTA = .25
        private const val MIN_RETAINED_TEXTURE = .30
        private val materials = listOf(CardBackground.STAINED_GLASS, CardBackground.NACRE, CardBackground.BRUSHED_METAL)
        private val poses = listOf(CardMotion(), CardMotion(.35f, 0f), CardMotion(0f, .35f))
        private val palette = CardPalette(RgbColor(225, 93, 104), RgbColor(17, 166, 172))

        private fun pixels(bitmap: Bitmap): IntArray = IntArray(WIDTH * HEIGHT).also {
            bitmap.getPixels(it, 0, WIDTH, 0, 0, WIDTH, HEIGHT)
        }

        private fun difference(
            first: IntArray,
            second: IntArray,
            indices: List<Int> = first.indices.toList(),
        ): Double = indices.sumOf { index ->
            abs(Color.red(first[index]) - Color.red(second[index])) +
                abs(Color.green(first[index]) - Color.green(second[index])) +
                abs(Color.blue(first[index]) - Color.blue(second[index]))
        } / (indices.size * 3.0)
    }
}
