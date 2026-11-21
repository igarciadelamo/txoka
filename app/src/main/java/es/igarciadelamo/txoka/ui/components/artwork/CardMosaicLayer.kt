package es.igarciadelamo.txoka.ui.components.artwork

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import es.igarciadelamo.txoka.domain.profile.CardMosaic

@Composable
fun CardMosaicLayer(mosaic: CardMosaic, first: Color, second: Color, modifier: Modifier = Modifier) {
    var dimensions by remember { mutableStateOf(IntSize.Zero) }
    val bitmap = rememberCardMosaicMask(mosaic, dimensions.width, dimensions.height)
    Canvas(modifier = modifier.onSizeChanged { dimensions = it }) {
        drawRect(second)
        bitmap?.let {
            drawImage(
                image = it,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                colorFilter = ColorFilter.tint(first),
            )
        }
    }
}
