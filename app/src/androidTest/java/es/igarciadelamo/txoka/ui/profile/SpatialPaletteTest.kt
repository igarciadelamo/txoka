package es.igarciadelamo.txoka.ui.profile

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.profile.CardAppearance
import es.igarciadelamo.txoka.domain.profile.CardBackground
import es.igarciadelamo.txoka.domain.profile.CardMosaic
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.RgbColor
import es.igarciadelamo.txoka.ui.theme.TxokaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpatialPaletteTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val first = CardPalette.Default.accent
    private val second = CardPalette.Default.accent2

    @Test
    fun twoAndFourStopsStayInPhysicalCornersInRtl() {
        var card by mutableStateOf(CardPalette.Default.copy(appearance = CardAppearance()))
        compose.setContent {
            TxokaTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    BackgroundPalette(card, requireNotNull(card.appearance), onChange = { card = it }, onColour = {})
                }
            }
        }
        val tl = handle(R.string.appearance_top_left, first)
        val tr = handle(R.string.appearance_top_right, second)
        val bl = handle(R.string.appearance_bottom_left, first)
        val br = handle(R.string.appearance_bottom_right, second)
        val a = tl.fetchSemanticsNode().boundsInRoot
        val d = br.fetchSemanticsNode().boundsInRoot
        assertTrue(a.right <= d.left && a.bottom <= d.top)
        tr.assertDoesNotExist()
        bl.assertDoesNotExist()

        val toggle = compose.onNodeWithText(context.getString(R.string.appearance_four_corners))
        toggle.performClick()
        val b = tr.fetchSemanticsNode().boundsInRoot
        val c = bl.fetchSemanticsNode().boundsInRoot
        assertTrue(a.right <= b.left && a.bottom <= c.top && c.right <= d.left && b.bottom <= d.top)
        toggle.performClick()
        tr.assertDoesNotExist()
        bl.assertDoesNotExist()
    }

    @Test
    fun flatFillAndMosaicExposeOnlyTheirEditableInks() {
        val flat = CardAppearance(background = CardBackground.FLAT)
        var appearance by mutableStateOf(flat)
        var clicked: CardColourTarget? = null
        compose.setContent {
            TxokaTheme {
                BackgroundPalette(
                    card = CardPalette.Default.copy(appearance = appearance),
                    appearance = appearance,
                    onChange = {},
                    onColour = { clicked = it },
                )
            }
        }
        handle(R.string.appearance_background, first).performClick()
        compose.runOnIdle {
            assertEquals(CardColourTarget.FIRST, clicked)
            appearance = flat.copy(mosaic = CardMosaic.SQUARES)
        }
        handle(R.string.appearance_background, first).assertDoesNotExist()
        handle(R.string.appearance_mosaic, first).performClick()
        compose.runOnIdle { assertEquals(CardColourTarget.FIRST, clicked) }
        handle(R.string.appearance_background, second).performClick()
        compose.runOnIdle { assertEquals(CardColourTarget.SECOND, clicked) }
        compose.onNodeWithText(context.getString(R.string.appearance_four_corners)).assertDoesNotExist()
    }

    @Test
    fun editingTopRightChangesOnlyThatCornerAndReturnsToPalette() {
        val four = CardAppearance(topRight = second, bottomLeft = first, mosaic = CardMosaic.TRIANGLES)
        val initial = CardPalette.Default.copy(appearance = four)
        var current by mutableStateOf(initial)
        compose.setContent {
            TxokaTheme {
                AppearanceEditor(
                    state = ProfileEditorUiState(card = current),
                    onCardChange = { current = it },
                    contactFields = {},
                )
            }
        }
        handle(R.string.appearance_top_right, second).performScrollTo().performClick()
        val red = requireNotNull(RgbColor.parse("#EF4444"))
        compose.onNodeWithContentDescription(context.getString(R.string.appearance_colour_value, red.hex))
            .performScrollTo().performClick()
        compose.runOnIdle { assertEquals(initial.copy(appearance = four.copy(topRight = red)), current) }
        handle(R.string.appearance_top_right, red).assertExists()
    }

    private fun handle(label: Int, color: RgbColor) = compose.onNodeWithContentDescription(
        context.getString(R.string.appearance_edit_palette_colour, context.getString(label), color.hex),
    )
}
