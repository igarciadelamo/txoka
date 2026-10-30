package es.igarciadelamo.txoka.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.domain.profile.RgbColor
import es.igarciadelamo.txoka.ui.theme.Corners
import es.igarciadelamo.txoka.ui.theme.TxokaTheme

enum class CardLayout {
    /** Hub and detail: avatar on top, name and contact rows anchored to the bottom. */
    Hero,

    /** Editor preview and incoming sheet: avatar and name on one row, details underneath. */
    Preview,
}

private val CardShape = RoundedCornerShape(Corners.card)
private val CardCorner = Corners.card
private val CardBorderWidth = 2.dp
private val CardBorderDash = 8.dp

/**
 * Renders the card exactly as recipients will see it. While the profile is not saved yet the
 * card is drawn as a ghost so the onboarding preview cannot be mistaken for a stored card.
 */
@Composable
fun ContactCard(
    profile: Profile,
    modifier: Modifier = Modifier,
    placeholder: Boolean = false,
    layout: CardLayout = CardLayout.Hero,
) {
    val contentColor = when {
        placeholder -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = GHOST_CONTENT_ALPHA)
        profile.card.accent.prefersDarkContent() -> Color(0xFF1C1B1F)
        else -> Color.White
    }
    val accent = profile.card.accent.toComposeColor()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (placeholder) {
                    Modifier
                } else {
                    Modifier.shadow(
                        elevation = 22.dp,
                        shape = CardShape,
                        ambientColor = accent,
                        spotColor = accent,
                    )
                },
            )
            .clip(CardShape)
            .then(
                if (placeholder) {
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, CardShape)
                        .dashedOutline(MaterialTheme.colorScheme.outline)
                } else {
                    Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    accent,
                                    profile.card.accent2.toComposeColor(),
                                ),
                            ),
                        )
                        .highlight()
                },
            )
            .then(
                when (layout) {
                    CardLayout.Hero -> Modifier.aspectRatio(HERO_ASPECT_RATIO)
                    CardLayout.Preview -> Modifier.aspectRatio(PREVIEW_ASPECT_RATIO)
                },
            ),
    ) {
        when (layout) {
            CardLayout.Hero -> HeroContent(profile = profile, contentColor = contentColor)
            CardLayout.Preview -> PreviewContent(profile = profile, contentColor = contentColor)
        }
        if (!placeholder) {
            Text(
                text = TXOKA_MARK,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 16.dp),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                color = contentColor.copy(alpha = BRANDMARK_ALPHA),
            )
        }
    }
}

@Composable
private fun HeroContent(profile: Profile, contentColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 20.dp),
    ) {
        Avatar(profile = profile, size = 52.dp, contentColor = contentColor)
        Text(
            text = profile.displayName(),
            modifier = Modifier
                .padding(top = 10.dp)
                .semantics { heading() },
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 27.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.8).sp,
                fontWeight = FontWeight.Bold,
            ),
            color = contentColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        ContactRows(profile = profile, contentColor = contentColor)
    }
}

@Composable
private fun PreviewContent(profile: Profile, contentColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(profile = profile, size = 46.dp, contentColor = contentColor)
            Text(
                text = profile.displayName(),
                modifier = Modifier
                    .padding(start = 14.dp)
                    .semantics { heading() },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ContactRows(profile = profile, contentColor = contentColor)
    }
}

@Composable
private fun Avatar(profile: Profile, size: Dp, contentColor: Color) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(contentColor.copy(alpha = AVATAR_BACKGROUND_ALPHA))
            .border(width = 1.dp, color = contentColor.copy(alpha = AVATAR_RING_ALPHA), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (profile.initials.isEmpty()) {
            Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = contentColor)
        } else {
            Text(
                text = profile.initials,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = contentColor,
            )
        }
    }
}

@Composable
private fun ContactRows(profile: Profile, contentColor: Color) {
    val rows = listOfNotNull(
        profile.sharedPhone?.let { phone -> Icons.Filled.Phone to phone },
        profile.sharedEmail?.let { email -> Icons.Filled.Email to email },
    )
    if (rows.isEmpty()) return

    Column(
        modifier = Modifier.padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rows.forEach { (icon, text) ->
            ContactPill(icon = icon, text = text, contentColor = contentColor)
        }
    }
}

@Composable
private fun ContactPill(icon: ImageVector, text: String, contentColor: Color) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(contentColor.copy(alpha = PILL_BACKGROUND_ALPHA))
            .padding(start = 12.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = contentColor,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Profile.displayName(): String = name.ifBlank { stringResource(R.string.profile_name_placeholder) }

/** Soft light from the top-left corner, which gives the flat gradient some depth. */
private fun Modifier.highlight(): Modifier = drawWithCache {
    val glow = Brush.radialGradient(
        colors = listOf(Color.White.copy(alpha = HIGHLIGHT_ALPHA), Color.Transparent),
        center = Offset(size.width * HIGHLIGHT_X, 0f),
        radius = size.maxDimension,
    )
    onDrawBehind { drawRect(brush = glow) }
}

@Composable
private fun Modifier.dashedOutline(color: Color): Modifier {
    val density = LocalDensity.current
    val strokeWidth = with(density) { CardBorderWidth.toPx() }
    val dash = with(density) { CardBorderDash.toPx() }
    val corner = with(density) { CardCorner.toPx() }

    return drawWithCache {
        val effect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))
        onDrawBehind {
            drawRoundRect(
                color = color,
                cornerRadius = CornerRadius(corner, corner),
                style = Stroke(width = strokeWidth, pathEffect = effect),
            )
        }
    }
}

internal fun RgbColor.toComposeColor(): Color = Color(argb)

private const val HERO_ASPECT_RATIO = 1.58f
private const val PREVIEW_ASPECT_RATIO = 1.95f
private const val TXOKA_MARK = "TXOKA"
private const val GHOST_CONTENT_ALPHA = 0.55f
private const val AVATAR_BACKGROUND_ALPHA = 0.16f
private const val AVATAR_RING_ALPHA = 0.26f
private const val PILL_BACKGROUND_ALPHA = 0.14f
private const val HIGHLIGHT_ALPHA = 0.30f
private const val HIGHLIGHT_X = 0.08f
private const val BRANDMARK_ALPHA = 0.45f

@Preview(showBackground = true)
@Composable
private fun ContactCardPreview() {
    TxokaTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ContactCard(
                profile = Profile(
                    name = "Isidro García",
                    phone = "+34 600 123 456",
                    email = "isidro@example.com",
                    sharePhone = true,
                    shareEmail = true,
                    card = CardPalette.Default,
                ),
            )
            ContactCard(
                profile = Profile(name = "Isidro García", phone = "+34 600 123 456"),
                layout = CardLayout.Preview,
            )
            ContactCard(profile = Profile(name = ""), placeholder = true)
        }
    }
}
