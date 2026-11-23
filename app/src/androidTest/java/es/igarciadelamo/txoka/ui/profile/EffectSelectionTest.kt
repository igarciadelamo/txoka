package es.igarciadelamo.txoka.ui.profile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.profile.CardAppearance
import es.igarciadelamo.txoka.domain.profile.CardAppearanceCodec
import es.igarciadelamo.txoka.domain.profile.CardBackground
import es.igarciadelamo.txoka.domain.profile.CardEffect
import es.igarciadelamo.txoka.domain.profile.CardMosaic
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.ui.theme.TxokaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EffectSelectionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun noEffectClearsOnlyTheOverlay() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val original = CardAppearance(background = CardBackground.NACRE, mosaic = CardMosaic.SQUARES)
        val palette = CardPalette.Default.copy(appearance = original)
        val card = mutableStateOf(palette)
        compose.setContent {
            TxokaTheme {
                AppearanceEditor(
                    state = ProfileEditorUiState(card = card.value),
                    onCardChange = { card.value = it },
                    modifier = Modifier.fillMaxSize(),
                    contactFields = {},
                )
            }
        }
        compose.onNodeWithContentDescription(context.getString(R.string.appearance_pause)).performClick()
        val effectTab = compose.onNodeWithText(context.getString(R.string.appearance_effect))
        val clear = compose.onNodeWithText(context.getString(R.string.appearance_no_effect))
        effectTab.performClick()
        clear.assertIsDisplayed().assertIsSelected()
        for (effect in listOf(CardEffect.WATER, CardEffect.FIRE)) {
            compose.onNodeWithText(context.getString(effect.label)).performScrollTo().performClick()
            compose.runOnIdle { assertEquals(palette.copy(appearance = original.copy(effect = effect)), card.value) }
            compose.onNodeWithText(context.getString(R.string.appearance_mosaic)).performClick()
            effectTab.performClick()
            clear.assertIsDisplayed().performClick()
            clear.assertIsSelected()
            compose.runOnIdle {
                assertEquals(palette, card.value)
                val encoded = CardAppearanceCodec.encode(requireNotNull(card.value.appearance))
                assertEquals(original, CardAppearanceCodec.decode(encoded))
            }
        }
    }
}
