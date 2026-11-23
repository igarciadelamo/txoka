package es.igarciadelamo.txoka.ui.components

import android.animation.ValueAnimator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.domain.protocol.QrVCardCodec
import es.igarciadelamo.txoka.ui.theme.Corners
import es.igarciadelamo.txoka.ui.theme.Spacing
import es.igarciadelamo.txoka.ui.theme.TxokaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.floor

@Composable
internal fun CardQrFlip(
    profile: Profile,
    modifier: Modifier,
    motion: CardMotion?,
    paused: Boolean,
    qrVisible: Boolean?,
    onQrVisibleChange: ((Boolean) -> Unit)?,
    showQrControls: Boolean,
    front: @Composable (Modifier, Boolean) -> Unit,
) {
    var flipped by rememberSaveable(profile) { mutableStateOf(false) }
    val available = remember(profile) { profile.canShowQr() }
    val visible = available && (qrVisible ?: flipped)
    val toggle: () -> Unit = {
        val next = !visible
        flipped = next
        onQrVisibleChange?.invoke(next)
    }
    val label = stringResource(if (visible) R.string.qr_back_to_card else R.string.qr_show)
    val angle by animateFloatAsState(
        targetValue = if (visible) 180f else 0f,
        animationSpec = tween(if (ValueAnimator.areAnimatorsEnabled()) 420 else 0),
        label = "cardFlip",
    )
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Box(
            Modifier.fillMaxWidth()
                .pointerInput(profile, visible, available) {
                    if (available) detectTapGestures(onDoubleTap = { toggle() })
                }
                .semantics {
                    if (available) {
                        customActions = listOf(
                            CustomAccessibilityAction(label) {
                                toggle()
                                true
                            },
                        )
                    }
                }
                .graphicsLayer {
                    rotationY = angle
                    cameraDistance = 12f * density
                },
        ) {
            front(
                Modifier.graphicsLayer { alpha = if (angle <= 90f) 1f else 0f }
                    .then(if (angle > 90f) Modifier.clearAndSetSemantics {} else Modifier),
                paused || angle > 90f,
            )
            if (angle > 90f) {
                CardQrBack(
                    profile = profile,
                    motion = motion,
                    paused = paused,
                    modifier = Modifier.matchParentSize().graphicsLayer { rotationY = 180f },
                )
            }
        }
        if (showQrControls) CardQrControls(visible, available, label, toggle)
    }
}

@Composable
private fun CardQrControls(visible: Boolean, available: Boolean, label: String, toggle: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        TextButton(onClick = toggle, enabled = available, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(label)
        }
        if (!available) {
            Text(
                stringResource(R.string.qr_too_large),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        } else if (visible) {
            Text(
                stringResource(R.string.qr_shared_fields),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CardQrBack(profile: Profile, motion: CardMotion?, paused: Boolean, modifier: Modifier) {
    val shape = RoundedCornerShape(Corners.card)
    val pose = motion ?: rememberCardMotionController().motion
    val matrix by produceState<BitMatrix?>(null, profile) {
        value = withContext(Dispatchers.Default) {
            QRCodeWriter().encode(
                QrVCardCodec.encode(profile),
                BarcodeFormat.QR_CODE,
                0,
                0,
                mapOf(
                    EncodeHintType.CHARACTER_SET to "UTF-8",
                    EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN to QR_QUIET_ZONE_MODULES,
                ),
            )
        }
    }
    BoxWithConstraints(
        modifier.fillMaxWidth().shadow(16.dp, shape).clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        CardSurface(profile.card, pose, paused, Modifier.matchParentSize())
        val side = (minOf(maxWidth, maxHeight) - 24.dp).coerceAtLeast(1.dp)
        val code = matrix
        if (code == null) {
            CircularProgressIndicator(Modifier.background(Color.White).padding(Spacing.md))
        } else if (with(LocalDensity.current) { side.toPx() } < code.width) {
            Text(
                text = stringResource(R.string.qr_needs_more_space),
                modifier = Modifier.padding(Spacing.md).background(Color.White).padding(Spacing.md),
                color = Color.Black,
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            QrModules(code, Modifier.size(side))
        }
    }
}

@Composable
private fun QrModules(matrix: BitMatrix, modifier: Modifier) {
    val description = stringResource(R.string.qr_code_description)
    Canvas(modifier.background(Color.White).semantics { contentDescription = description }) {
        val scale = floor(minOf(size.width, size.height) / matrix.width)
        if (scale < 1f) return@Canvas
        val start = Offset(
            floor((size.width - matrix.width * scale) / 2f),
            floor((size.height - matrix.height * scale) / 2f),
        )
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix[x, y]) {
                    drawRect(Color.Black, start + Offset(x * scale, y * scale), Size(scale, scale))
                }
            }
        }
    }
}

internal fun Profile.canShowQr(): Boolean = name.isNotBlank() && QrVCardCodec.fits(this)

private const val QR_QUIET_ZONE_MODULES = 4

@Preview(showBackground = true)
@Composable
private fun CardQrFlipPreview() {
    TxokaTheme {
        ContactCard(
            profile = Profile(name = "Marta Ruiz", email = "marta@example.com"),
            qrEnabled = true,
        )
    }
}
