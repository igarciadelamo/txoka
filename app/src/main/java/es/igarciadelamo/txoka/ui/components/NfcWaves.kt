package es.igarciadelamo.txoka.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Three arcs and a dot, the same motif used for sharing. */
@Composable
fun NfcWaves(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(28.dp)) {
        val stroke = Stroke(width = size.minDimension * 0.08f, cap = StrokeCap.Round)
        val center = Offset(size.width * 0.30f, size.height / 2f)
        drawCircle(color = color, radius = size.minDimension * 0.10f, center = center)
        listOf(0.26f, 0.44f).forEach { fraction ->
            drawArc(
                color = color,
                startAngle = -50f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(
                    center.x - size.minDimension * fraction,
                    center.y - size.minDimension * fraction,
                ),
                size = Size(size.minDimension * fraction * 2f, size.minDimension * fraction * 2f),
                style = stroke,
            )
        }
    }
}
