package es.igarciadelamo.txoka.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Spacing scale, so screens never invent their own gaps. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val huge = 48.dp
}

/** Corner radii used by cards, sheets and controls. */
object Corners {
    val card = 28.dp
    val sheet = 20.dp
    val field = 16.dp
    val chip = 12.dp
}

val TxokaShapes = Shapes(
    extraSmall = RoundedCornerShape(Corners.chip),
    small = RoundedCornerShape(Corners.field),
    medium = RoundedCornerShape(Corners.field),
    large = RoundedCornerShape(Corners.sheet),
    extraLarge = RoundedCornerShape(Corners.card),
)

/** Content width used by the screens, so tablets do not stretch the layout. */
val ContentMaxWidth = 560.dp
