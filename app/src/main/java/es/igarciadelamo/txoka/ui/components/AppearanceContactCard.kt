package es.igarciadelamo.txoka.ui.components

import android.animation.ValueAnimator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.profile.CardAppearance
import es.igarciadelamo.txoka.domain.profile.CardEffect
import es.igarciadelamo.txoka.domain.profile.CardTextStyle
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.domain.profile.SocialHandle
import es.igarciadelamo.txoka.domain.profile.SocialNetwork
import es.igarciadelamo.txoka.ui.theme.Corners

@Composable
internal fun AppearanceContactCard(profile: Profile, modifier: Modifier, motion: CardMotion?, paused: Boolean) {
    val appearance = requireNotNull(profile.card.appearance)
    val pose = (motion ?: rememberCardMotionController().motion).bounded()
    val time = rememberCardTime(paused || appearance.effect == CardEffect.NONE)
    val shape = RoundedCornerShape(Corners.card)
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = maxWidth / PHYSICAL_CARD_RATIO)
                .shadow(16.dp, shape)
                .clip(shape)
                .semantics(mergeDescendants = true) {},
        ) {
            CardMaterialLayer(profile.card, pose, time, Modifier.matchParentSize())
            Column(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 34.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    FinishedCardText(
                        text = profile.name.ifBlank { stringResource(R.string.profile_name_placeholder) },
                        finish = appearance.name,
                        motion = pose,
                        baseSize = 29.sp,
                        modifier = Modifier.weight(1f).semantics { heading() },
                        maxLines = if (fontScale > LARGE_FONT_SCALE) 4 else 2,
                    )
                    Text(
                        text = profile.initials,
                        modifier = Modifier.padding(start = 8.dp)
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = .15f), CircleShape)
                            .border(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = .25f), CircleShape)
                            .padding(9.dp).clearAndSetSemantics {},
                        color = appearance.name.effectiveColor.toComposeColor(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                AppearanceContactRows(profile, appearance, pose)
            }
            Text(
                text = "TXOKA",
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 14.dp)
                    .clearAndSetSemantics {},
                color = appearance.data.effectiveColor.toComposeColor().copy(alpha = .65f),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
            )
        }
    }
}

@Composable
private fun AppearanceContactRows(profile: Profile, appearance: CardAppearance, pose: CardMotion) {
    val color = appearance.data.effectiveColor.toComposeColor()
    profile.phone?.takeIf(String::isNotBlank)?.let {
        AppearanceDetail(it, appearance.data, pose) {
            Icon(Icons.Filled.Phone, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
    }
    profile.email?.takeIf(String::isNotBlank)?.let {
        AppearanceDetail(it, appearance.data, pose) {
            Icon(Icons.Filled.Email, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
    }
    listOf(SocialNetwork.LinkedIn to profile.linkedin, SocialNetwork.Instagram to profile.instagram)
        .forEach { (network, handle) ->
            handle?.takeIf(String::isNotBlank)?.let {
                AppearanceDetail(
                    SocialHandle.display(network, it),
                    appearance.data,
                    pose,
                    description = stringResource(
                        R.string.social_handle_description,
                        network.label(),
                        SocialHandle.display(network, it),
                    ),
                ) {
                    SocialIcon(network, color, Modifier.size(16.dp))
                }
            }
        }
}

@Composable
private fun AppearanceDetail(
    value: String,
    style: CardTextStyle,
    motion: CardMotion,
    description: String? = null,
    icon: @Composable () -> Unit,
) {
    FinishedCardText(
        text = value,
        finish = style,
        motion = motion,
        baseSize = 14.sp,
        maxLines = 2,
        leading = icon,
        modifier = if (description == null) {
            Modifier
        } else {
            Modifier.clearAndSetSemantics { contentDescription = description }
        },
    )
}

@Composable
internal fun rememberCardTime(paused: Boolean): Float {
    val owner = LocalLifecycleOwner.current
    var active by remember(owner) { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    androidx.compose.runtime.DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, _ ->
            active = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(paused, active) {
        if (!paused && active && ValueAnimator.areAnimatorsEnabled()) {
            var previous = withFrameNanos { it }
            while (true) {
                withFrameNanos { now ->
                    if (now - previous >= FRAME_INTERVAL_NANOS) {
                        time += ((now - previous).toDouble() / NANOS_PER_SECOND).toFloat().coerceAtMost(MAX_FRAME_STEP)
                        previous = now
                    }
                }
            }
        }
    }
    return time
}

internal const val PHYSICAL_CARD_RATIO = 85.6f / 53.98f
private const val FRAME_INTERVAL_NANOS = 33_000_000L
private const val NANOS_PER_SECOND = 1_000_000_000.0
private const val MAX_FRAME_STEP = .1f
private const val LARGE_FONT_SCALE = 1.4f
