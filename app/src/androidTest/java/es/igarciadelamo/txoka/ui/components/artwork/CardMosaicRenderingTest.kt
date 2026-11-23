package es.igarciadelamo.txoka.ui.components.artwork

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.igarciadelamo.txoka.domain.profile.CardMosaic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardMosaicRenderingTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun everyProceduralAndBundledSvgMosaicRetainsBothInks() {
        CardMosaic.entries.filter { it != CardMosaic.NONE }.forEach { mosaic ->
            val bitmap = requireNotNull(CardMosaicMasks.mask(context, mosaic, 192, 121))
            assertEquals(192, bitmap.width)
            assertEquals(121, bitmap.height)
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            assertTrue("Missing first ink in $mosaic", pixels.any { Color.alpha(it) > 224 })
            assertTrue("Missing second ink in $mosaic", pixels.any { Color.alpha(it) < 32 })
        }
    }

    @Test
    fun maskResolutionIsBoundedWithoutChangingCardAspect() {
        val bitmap = requireNotNull(CardMosaicMasks.mask(context, CardMosaic.NATURE_FISH, 2560, 1600))
        assertEquals(1024, bitmap.width)
        assertEquals(640, bitmap.height)
        assertNull(CardMosaicMasks.mask(context, CardMosaic.NONE, 192, 121))
        assertNull(CardMosaicMasks.mask(context, CardMosaic.STARS, 0, 121))
        assertNull(CardMosaicMasks.mask(context, CardMosaic.STARS, Int.MAX_VALUE, 1))
    }
}
