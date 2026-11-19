package es.igarciadelamo.txoka.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import es.igarciadelamo.txoka.domain.profile.CardTextStyle
import es.igarciadelamo.txoka.domain.profile.TextFinish
import es.igarciadelamo.txoka.domain.profile.TextSize

@Composable
internal fun FinishedCardText(
    text: String,
    finish: CardTextStyle,
    motion: CardMotion,
    baseSize: TextUnit,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    leading: (@Composable () -> Unit)? = null,
) {
    val color = finish.effectiveColor.toComposeColor()
    val size = baseSize * finish.size.multiplier()
    val shape = RoundedCornerShape(if (finish.finish == TextFinish.BUBBLE) 24.dp else 5.dp)
    val panel = when (finish.finish) {
        TextFinish.BUBBLE -> Modifier.background(Color.Black.copy(alpha = .22f), shape)
        TextFinish.FRAME -> Modifier.border(1.dp, color.copy(alpha = .7f), shape)
        else -> Modifier
    }
    val inset = finish.finish == TextFinish.BUBBLE || finish.finish == TextFinish.FRAME
    Row(
        modifier.then(panel).then(if (inset) Modifier.padding(horizontal = 10.dp, vertical = 5.dp) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        leading?.invoke()
        val style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = size,
            lineHeight = size * 1.18f,
            fontWeight = FontWeight.Bold,
        )
        Box(Modifier.weight(1f, fill = false)) {
            if (finish.finish == TextFinish.METAL) {
                MetallicText(text, style, color, motion, maxLines)
            } else {
                DepthText(text, style, color, finish.finish, motion, maxLines)
            }
        }
    }
}

@Composable
private fun MetallicText(text: String, style: TextStyle, color: Color, motion: CardMotion, maxLines: Int) {
    val width = with(LocalDensity.current) { 240.dp.toPx() }
    val center = width * (.5f + motion.x * .66f + motion.y * .34f)
    val highlight = Color(
        red = maxOf(color.red, .93f),
        green = maxOf(color.green, .95f),
        blue = maxOf(color.blue, .98f),
    )
    val brush = Brush.linearGradient(
        colorStops = arrayOf(0f to color, .38f to color, .5f to highlight, .62f to color, 1f to color),
        start = Offset(center - width / 2f, -width * .15f * motion.y),
        end = Offset(center + width / 2f, width * .15f * motion.y),
    )
    Text(
        text = text,
        style = style.copy(drawStyle = Stroke(width = with(LocalDensity.current) { .65.dp.toPx() })),
        color = Color.Black.copy(alpha = .8f),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.clearAndSetSemantics {},
    )
    Text(text, style = style.copy(brush = brush), maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun DepthText(
    text: String,
    style: TextStyle,
    color: Color,
    finish: TextFinish,
    motion: CardMotion,
    maxLines: Int,
) {
    val depth = when (finish) {
        TextFinish.RAISED -> 1.8f
        TextFinish.EMBOSSED -> 1.25f
        else -> 0f
    }
    val density = LocalDensity.current.density
    val dx = (.24f + motion.x * .75f) * depth * density
    val dy = (.5f + motion.y * .75f) * depth * density
    val engraved = finish == TextFinish.EMBOSSED
    if (depth > 0f) {
        Text(
            text,
            style = style,
            color = if (engraved) Color.White.copy(alpha = .7f) else Color.Black.copy(alpha = .6f),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.graphicsLayer {
                translationX = dx
                translationY = dy
            }.clearAndSetSemantics {},
        )
    }
    Text(
        text,
        style = style.copy(
            shadow = if (depth > 0f) {
                Shadow(
                    color = if (engraved) Color.Black.copy(alpha = .8f) else Color.White.copy(alpha = .7f),
                    offset = Offset(-dx * .36f, -dy * .36f),
                    blurRadius = if (engraved) .5f * density else 0f,
                )
            } else {
                Shadow(Color.Black.copy(alpha = .15f), Offset(0f, density), density)
            },
        ),
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.graphicsLayer {
            translationX = -motion.x * depth * .42f * density
            translationY = -motion.y * depth * .42f * density
        },
    )
}

private fun TextSize.multiplier(): Float = when (this) {
    TextSize.S -> SMALL_TEXT_SCALE
    TextSize.M -> MEDIUM_TEXT_SCALE
    TextSize.L -> LARGE_TEXT_SCALE
    TextSize.XL -> EXTRA_LARGE_TEXT_SCALE
}

private const val SMALL_TEXT_SCALE = .78f
private const val MEDIUM_TEXT_SCALE = 1f
private const val LARGE_TEXT_SCALE = 1.14f
private const val EXTRA_LARGE_TEXT_SCALE = 1.28f
