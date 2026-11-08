package es.igarciadelamo.txoka.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.HsvColor
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.ui.components.CardLayout
import es.igarciadelamo.txoka.ui.components.ContactCard
import es.igarciadelamo.txoka.ui.components.PrimaryButton
import es.igarciadelamo.txoka.ui.components.toComposeColor
import es.igarciadelamo.txoka.ui.theme.Corners
import es.igarciadelamo.txoka.ui.theme.Spacing
import es.igarciadelamo.txoka.ui.theme.TxokaTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val WheelSize = 220.dp
private val RingThickness = 22.dp
private val WheelGap = 10.dp
private val ThumbRadius = 10.dp
private val ThumbHalo = 3.dp

private const val FULL_TURN = 360f
private const val HALF_TURN = 180f
private const val SQUARE_ROOT_OF_TWO = 1.4142135f
private const val THUMB_OUTLINE_ALPHA = 0.35f

private val HueColors = listOf(
    Color.Red,
    Color.Yellow,
    Color.Green,
    Color.Cyan,
    Color.Blue,
    Color.Magenta,
    Color.Red,
)

private enum class WheelRegion { Ring, Square }

private data class WheelGeometry(val center: Offset, val ringRadius: Float, val squareHalf: Float)

/**
 * Bottom sheet that replaces the hex-only input. The wheel is the main control; the hex field
 * stays as a precise, accessible alternative. The second gradient stop is always derived, so a
 * custom colour keeps the same contrast guarantees as the presets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColourPickerSheet(profile: Profile, initial: CardPalette, onDismiss: () -> Unit, onApply: (CardPalette) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var hsv by remember(initial) { mutableStateOf(HsvColor.fromRgb(initial.accent)) }
    val accent = hsv.toRgb()
    // Keeping the untouched palette preserves a preset's curated second stop.
    val palette = if (accent == initial.accent) initial else CardPalette.fromAccent(accent)
    val preview = profile.copy(card = palette)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = Corners.card, topEnd = Corners.card),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = Spacing.xl)
                .padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(
                text = stringResource(R.string.card_colour_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            ContactCard(profile = preview, layout = CardLayout.Preview)
            ColourWheel(
                hsv = hsv,
                onHsvChange = { hsv = it },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.action_cancel))
                }
                PrimaryButton(
                    text = stringResource(R.string.card_colour_apply),
                    onClick = {
                        onApply(palette)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ColourWheel(hsv: HsvColor, onHsvChange: (HsvColor) -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.card_colour_wheel, hsv.toRgb().hex)
    val latestHsv by rememberUpdatedState(hsv)
    val latestChange by rememberUpdatedState(onHsvChange)
    val ringPx = with(LocalDensity.current) { RingThickness.toPx() }
    val gapPx = with(LocalDensity.current) { WheelGap.toPx() }

    Canvas(
        modifier = modifier
            .size(WheelSize)
            .semantics { contentDescription = description }
            .pointerInput(ringPx, gapPx) {
                val geometry = wheelGeometry(
                    size = Size(size.width.toFloat(), size.height.toFloat()),
                    ringPx = ringPx,
                    gapPx = gapPx,
                )
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val region = regionAt(down.position, geometry)
                    latestChange(hsvAt(region, latestHsv, down.position, geometry))
                    down.consume()
                    var pressed = true
                    while (pressed) {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.firstOrNull { it.id == down.id }
                        if (pointer == null || !pointer.pressed) {
                            pressed = false
                        } else {
                            latestChange(hsvAt(region, latestHsv, pointer.position, geometry))
                            pointer.consume()
                        }
                    }
                }
            },
    ) {
        drawWheel(hsv, ringPx, gapPx)
    }
}

private fun wheelGeometry(size: Size, ringPx: Float, gapPx: Float): WheelGeometry {
    val center = Offset(size.width / 2f, size.height / 2f)
    val outerRadius = min(size.width, size.height) / 2f
    val innerRadius = (outerRadius - ringPx).coerceAtLeast(0f)
    return WheelGeometry(
        center = center,
        ringRadius = (outerRadius + innerRadius) / 2f,
        squareHalf = ((innerRadius - gapPx) / SQUARE_ROOT_OF_TWO).coerceAtLeast(0f),
    )
}

private fun regionAt(position: Offset, geometry: WheelGeometry): WheelRegion {
    val dx = abs(position.x - geometry.center.x)
    val dy = abs(position.y - geometry.center.y)
    return if (dx <= geometry.squareHalf && dy <= geometry.squareHalf) {
        WheelRegion.Square
    } else {
        WheelRegion.Ring
    }
}

private fun hsvAt(region: WheelRegion, current: HsvColor, position: Offset, geometry: WheelGeometry): HsvColor =
    when (region) {
        WheelRegion.Ring -> current.copy(hue = hueAt(position, geometry.center))
        WheelRegion.Square -> {
            val half = geometry.squareHalf
            val saturation = ((position.x - geometry.center.x + half) / (2f * half)).coerceIn(0f, 1f)
            val value = 1f - ((position.y - geometry.center.y + half) / (2f * half)).coerceIn(0f, 1f)
            current.copy(saturation = saturation, value = value)
        }
    }

private fun hueAt(position: Offset, center: Offset): Float {
    val radians = atan2((position.y - center.y).toDouble(), (position.x - center.x).toDouble())
    val degrees = (radians * HALF_TURN / PI).toFloat()
    return (degrees + FULL_TURN) % FULL_TURN
}

private fun DrawScope.drawWheel(hsv: HsvColor, ringPx: Float, gapPx: Float) {
    val geometry = wheelGeometry(size, ringPx, gapPx)
    drawCircle(
        brush = Brush.sweepGradient(colors = HueColors, center = geometry.center),
        radius = geometry.ringRadius,
        center = geometry.center,
        style = Stroke(width = ringPx),
    )
    drawSquare(hsv, geometry)
    drawThumb(hsv, geometry)
}

private fun DrawScope.drawSquare(hsv: HsvColor, geometry: WheelGeometry) {
    val side = geometry.squareHalf * 2f
    val topLeft = Offset(geometry.center.x - geometry.squareHalf, geometry.center.y - geometry.squareHalf)
    val squareSize = Size(side, side)
    drawRect(
        color = HsvColor(hsv.hue, 1f, 1f).toRgb().toComposeColor(),
        topLeft = topLeft,
        size = squareSize,
    )
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.White, Color.Transparent),
            startX = topLeft.x,
            endX = topLeft.x + side,
        ),
        topLeft = topLeft,
        size = squareSize,
    )
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Black),
            startY = topLeft.y,
            endY = topLeft.y + side,
        ),
        topLeft = topLeft,
        size = squareSize,
    )
}

private fun DrawScope.drawThumb(hsv: HsvColor, geometry: WheelGeometry) {
    val angle = hsv.hue * PI / HALF_TURN
    val ringPosition = Offset(
        x = geometry.center.x + geometry.ringRadius * cos(angle).toFloat(),
        y = geometry.center.y + geometry.ringRadius * sin(angle).toFloat(),
    )
    drawThumbAt(ringPosition)

    val side = geometry.squareHalf * 2f
    val squarePosition = Offset(
        x = geometry.center.x - geometry.squareHalf + hsv.saturation * side,
        y = geometry.center.y - geometry.squareHalf + (1f - hsv.value) * side,
    )
    drawThumbAt(squarePosition)
}

private fun DrawScope.drawThumbAt(center: Offset) {
    val radius = ThumbRadius.toPx()
    val halo = ThumbHalo.toPx()
    drawCircle(color = Color.White, radius = radius + halo, center = center)
    drawCircle(
        color = Color.Black.copy(alpha = THUMB_OUTLINE_ALPHA),
        radius = radius,
        center = center,
        style = Stroke(width = halo),
    )
}

@Preview(showBackground = true)
@Composable
private fun ColourWheelPreview() {
    TxokaTheme {
        ColourWheel(
            hsv = HsvColor.fromRgb(CardPalette.Default.accent),
            onHsvChange = {},
            modifier = Modifier.padding(Spacing.xl),
        )
    }
}
