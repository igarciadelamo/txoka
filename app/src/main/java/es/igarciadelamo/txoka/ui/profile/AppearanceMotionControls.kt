package es.igarciadelamo.txoka.ui.profile

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.ui.components.CardMotionController
import kotlin.math.roundToInt

private enum class MotionGlyph { PHONE, CENTER, PAUSE, EXPAND }

@Composable
internal fun AppearanceMotionControls(
    controller: CardMotionController,
    paused: Boolean,
    onPausedChange: (Boolean) -> Unit,
    onExpand: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    stacked: Boolean = false,
) {
    if (stacked) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MotionButtonGroup(controller, paused, onPausedChange, onExpand, Modifier.fillMaxWidth())
            MotionJoystick(controller)
        }
        return
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        MotionButtonGroup(controller, paused, onPausedChange, onExpand, Modifier.weight(1f))
        MotionJoystick(controller)
    }
}

@Composable
private fun MotionButtonGroup(
    controller: CardMotionController,
    paused: Boolean,
    onPausedChange: (Boolean) -> Unit,
    onExpand: (() -> Unit)?,
    modifier: Modifier,
) {
    Column(modifier = modifier) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MotionButton(
                glyph = MotionGlyph.PHONE,
                label = stringResource(R.string.appearance_sensors),
                selected = controller.sensorsEnabled,
                enabled = controller.sensorsAvailable,
                rotation = if (LocalConfiguration.current.orientation ==
                    Configuration.ORIENTATION_LANDSCAPE
                ) {
                    90f
                } else {
                    0f
                },
                onClick = controller::toggleSensors,
            )
            MotionButton(
                MotionGlyph.CENTER,
                stringResource(R.string.appearance_center),
                onClick = controller::center,
            )
            MotionButton(
                MotionGlyph.PAUSE,
                stringResource(R.string.appearance_pause),
                selected = paused,
                onClick = { onPausedChange(!paused) },
            )
            if (onExpand != null) {
                MotionButton(MotionGlyph.EXPAND, stringResource(R.string.appearance_expand), onClick = onExpand)
            }
        }
        Text(
            stringResource(
                when {
                    !controller.sensorsAvailable -> R.string.appearance_no_sensors
                    controller.sensorsEnabled -> R.string.appearance_sensor_mode
                    else -> R.string.appearance_manual_mode
                },
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MotionButton(
    glyph: MotionGlyph,
    label: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    rotation: Float = 0f,
    onClick: () -> Unit,
) {
    val color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val interaction = if (glyph == MotionGlyph.PHONE || glyph == MotionGlyph.PAUSE) {
        Modifier.toggleable(selected, enabled = enabled, role = Role.Switch, onValueChange = { onClick() })
    } else {
        Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    }
    Canvas(
        modifier = Modifier.size(48.dp)
            .then(interaction)
            .padding(2.dp)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(10.dp),
            )
            .border(1.dp, color.copy(alpha = .5f), RoundedCornerShape(10.dp))
            .semantics { contentDescription = label }
            .graphicsLayer { rotationZ = rotation },
    ) {
        val stroke = 1.6.dp.toPx()
        when (glyph) {
            MotionGlyph.PHONE -> {
                drawRoundRect(
                    color,
                    Offset(size.width * .35f, size.height * .2f),
                    Size(size.width * .3f, size.height * .6f),
                    CornerRadius(3.dp.toPx()),
                    style = Stroke(stroke),
                )
                drawLine(
                    color,
                    Offset(size.width * .44f, size.height * .72f),
                    Offset(size.width * .56f, size.height * .72f),
                    stroke,
                )
            }
            MotionGlyph.CENTER -> {
                drawCircle(color, size.width * .17f, center, style = Stroke(stroke))
                drawLine(color, Offset(center.x, size.height * .2f), Offset(center.x, size.height * .8f), stroke)
                drawLine(color, Offset(size.width * .2f, center.y), Offset(size.width * .8f, center.y), stroke)
            }
            MotionGlyph.PAUSE -> {
                drawLine(
                    color,
                    Offset(size.width * .4f, size.height * .3f),
                    Offset(size.width * .4f, size.height * .7f),
                    stroke,
                )
                drawLine(
                    color,
                    Offset(size.width * .6f, size.height * .3f),
                    Offset(size.width * .6f, size.height * .7f),
                    stroke,
                )
            }
            MotionGlyph.EXPAND -> {
                for (x in listOf(.3f, .7f)) {
                    for (y in listOf(.3f, .7f)) {
                        val dx = if (x < .5f) .14f else -.14f
                        val dy = if (y < .5f) .14f else -.14f
                        val corner = Offset(size.width * x, size.height * y)
                        drawLine(color, corner, Offset(size.width * (x + dx), corner.y), stroke)
                        drawLine(color, corner, Offset(corner.x, size.height * (y + dy)), stroke)
                    }
                }
            }
        }
    }
}

@Composable
private fun MotionJoystick(controller: CardMotionController) {
    val current by rememberUpdatedState(controller)
    val motion = controller.motion
    val moveLeft = stringResource(R.string.appearance_move_left)
    val moveRight = stringResource(R.string.appearance_move_right)
    val moveUp = stringResource(R.string.appearance_move_up)
    val moveDown = stringResource(R.string.appearance_move_down)
    val centerLabel = stringResource(R.string.appearance_center)
    val description = stringResource(R.string.appearance_joystick)
    val color = MaterialTheme.colorScheme.primary
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier.size(64.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .semantics {
                    contentDescription = description
                    customActions = listOf(
                        CustomAccessibilityAction(moveLeft) {
                            controller.updateManual(motion.x - .15f, motion.y)
                            true
                        },
                        CustomAccessibilityAction(moveRight) {
                            controller.updateManual(motion.x + .15f, motion.y)
                            true
                        },
                        CustomAccessibilityAction(moveUp) {
                            controller.updateManual(motion.x, motion.y - .15f)
                            true
                        },
                        CustomAccessibilityAction(moveDown) {
                            controller.updateManual(motion.x, motion.y + .15f)
                            true
                        },
                        CustomAccessibilityAction(centerLabel) {
                            controller.center()
                            true
                        },
                    )
                }
                .pointerInput(Unit) { trackManualMotion { current } },
        ) {
            drawLine(
                color.copy(alpha = .22f),
                Offset(8.dp.toPx(), center.y),
                Offset(size.width - 8.dp.toPx(), center.y),
                1.dp.toPx(),
            )
            drawLine(
                color.copy(alpha = .22f),
                Offset(center.x, 8.dp.toPx()),
                Offset(center.x, size.height - 8.dp.toPx()),
                1.dp.toPx(),
            )
            drawCircle(
                color.copy(alpha = .65f),
                radius = 7.dp.toPx(),
                center = Offset(center.x + motion.x * size.width * .32f, center.y + motion.y * size.height * .32f),
            )
        }
        Text(
            stringResource(R.string.appearance_tilt, (motion.x * 100).roundToInt(), (motion.y * 100).roundToInt()),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

private suspend fun PointerInputScope.trackManualMotion(controller: () -> CardMotionController) {
    awaitEachGesture {
        val down = awaitFirstDown()
        fun update(position: Offset) {
            val x = (position.x / size.width * 2f - 1f).coerceIn(-1f, 1f)
            val y = (position.y / size.height * 2f - 1f).coerceIn(-1f, 1f)
            controller().updateManual(x, y)
        }
        update(down.position)
        down.consume()
        do {
            val event = awaitPointerEvent()
            val pointer = event.changes.firstOrNull { it.id == down.id }
            if (pointer != null && pointer.pressed) {
                update(pointer.position)
                pointer.consume()
            }
        } while (pointer != null && pointer.pressed)
    }
}
