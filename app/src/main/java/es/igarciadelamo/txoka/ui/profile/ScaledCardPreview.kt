package es.igarciadelamo.txoka.ui.profile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.ui.components.CardLayout
import es.igarciadelamo.txoka.ui.components.CardMotion
import es.igarciadelamo.txoka.ui.components.ContactCard
import kotlin.math.roundToInt

/** Scales the complete preview, including dense text, instead of letting it consume the editor. */
@Composable
internal fun ScaledCardPreview(profile: Profile, motion: CardMotion, paused: Boolean, maxHeight: Dp) {
    Layout(
        content = { ContactCard(profile = profile, layout = CardLayout.Preview, motion = motion, paused = paused) },
        modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight),
    ) { measurables, constraints ->
        val referenceWidth = 360.dp.roundToPx()
        val card = measurables.single().measure(Constraints.fixedWidth(referenceWidth))
        val scale = minOf(constraints.maxWidth.toFloat() / card.width, constraints.maxHeight.toFloat() / card.height)
        val width = constraints.constrainWidth((card.width * scale).roundToInt())
        val height = constraints.constrainHeight((card.height * scale).roundToInt())
        layout(width, height) {
            card.placeWithLayer(x = (width - card.width * scale).roundToInt() / 2, y = 0) {
                transformOrigin = TransformOrigin(0f, 0f)
                scaleX = scale
                scaleY = scale
            }
        }
    }
}
