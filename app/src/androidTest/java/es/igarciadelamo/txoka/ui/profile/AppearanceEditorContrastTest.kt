package es.igarciadelamo.txoka.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.graphics.ColorUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.ui.theme.TxokaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppearanceEditorContrastTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun mosaicControlsHaveReadableTextInDarkMode() = verifyEditorContrast(dark = true)

    @Test
    fun mosaicControlsHaveReadableTextInLightMode() = verifyEditorContrast(dark = false)

    private fun verifyEditorContrast(dark: Boolean) {
        compose.setContent {
            TxokaTheme(darkTheme = dark) {
                AppearanceEditor(
                    state = ProfileEditorUiState(),
                    onCardChange = {},
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                    contactFields = {},
                )
            }
        }
        val mosaic = context.getString(R.string.appearance_mosaic)
        compose.onNodeWithText(mosaic).performClick()
        val axesPrefix = context.getString(R.string.appearance_tilt).substringBefore('%')
        compose.onNodeWithText(axesPrefix, substring = true, useUnmergedTree = true).assertReadableInk(axesPrefix)
        for (label in listOf(mosaic, context.getString(R.string.appearance_background))) {
            compose.onNodeWithText(label, useUnmergedTree = true).assertReadableInk(label)
        }
        for (id in listOf(R.string.appearance_none, R.string.appearance_none_symbol, R.string.appearance_triangles)) {
            val label = context.getString(id)
            compose.onNodeWithText(label, useUnmergedTree = true).performScrollTo().assertReadableInk(label)
        }
    }

    private fun SemanticsNodeInteraction.assertReadableInk(label: String) {
        val pixels = captureToImage().toPixelMap()
        val background = pixels[0, 0].toArgb()
        var readablePixels = 0
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                if (ColorUtils.calculateContrast(pixels[x, y].toArgb(), background) >= 4.5) readablePixels++
            }
        }
        // Antialiasing deliberately blends the edges; measure the solid strokes inside each text node.
        assertTrue("Low rendered contrast for '$label'", readablePixels >= pixels.width * pixels.height * .01)
    }
}
