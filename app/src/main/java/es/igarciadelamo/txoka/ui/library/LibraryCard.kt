package es.igarciadelamo.txoka.ui.library

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import es.igarciadelamo.txoka.domain.received.ReceivedContact
import es.igarciadelamo.txoka.ui.components.CardLayout
import es.igarciadelamo.txoka.ui.components.CardMotion
import es.igarciadelamo.txoka.ui.components.ContactCard
import es.igarciadelamo.txoka.ui.components.PHYSICAL_CARD_RATIO
import kotlin.math.roundToInt

/** The complete card scales as one object; its surface always keeps the physical card ratio. */
@Composable
internal fun LibraryCard(
    contact: ReceivedContact,
    modifier: Modifier = Modifier,
    motion: CardMotion = CardMotion(),
    paused: Boolean = true,
) {
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val referenceWidth = 480.dp * fontScale
    Layout(
        modifier = modifier.fillMaxSize(),
        content = {
            ContactCard(
                profile = contact.libraryProfile(),
                modifier = Modifier.height(referenceWidth / PHYSICAL_CARD_RATIO),
                layout = CardLayout.Preview,
                motion = motion,
                paused = paused,
            )
        },
    ) { measurables, constraints ->
        val card = measurables.single().measure(Constraints.fixedWidth(referenceWidth.roundToPx()))
        val scale = minOf(constraints.maxWidth.toFloat() / card.width, constraints.maxHeight.toFloat() / card.height)
        val width = (card.width * scale).roundToInt()
        val height = (card.height * scale).roundToInt()
        layout(constraints.maxWidth, constraints.maxHeight) {
            card.placeWithLayer((constraints.maxWidth - width) / 2, (constraints.maxHeight - height) / 2) {
                transformOrigin = TransformOrigin(0f, 0f)
                scaleX = scale
                scaleY = scale
            }
        }
    }
}
