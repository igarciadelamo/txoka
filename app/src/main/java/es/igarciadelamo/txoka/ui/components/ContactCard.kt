package es.igarciadelamo.txoka.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.domain.profile.RgbColor
import es.igarciadelamo.txoka.ui.theme.TxokaTheme

private val CardShape = RoundedCornerShape(28.dp)
private val CardCorner = 28.dp
private val CardBorderWidth = 2.dp
private val CardBorderDash = 8.dp

/**
 * Renders the card exactly as recipients will see it. While the profile is not saved yet the
 * card is drawn as a ghost so the onboarding preview cannot be mistaken for a stored card.
 */
@Composable
fun ContactCard(profile: Profile, modifier: Modifier = Modifier, placeholder: Boolean = false) {
    val contentColor = when {
        placeholder -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = GHOST_CONTENT_ALPHA)
        profile.card.accent.prefersDarkContent() -> Color(0xFF1C1B1F)
        else -> Color.White
    }
    val displayName = profile.name.ifBlank { stringResource(R.string.profile_name_placeholder) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .then(
                if (placeholder) {
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, CardShape)
                        .dashedOutline(MaterialTheme.colorScheme.outline)
                } else {
                    Modifier.background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                profile.card.accent.toComposeColor(),
                                profile.card.accent2.toComposeColor(),
                            ),
                        ),
                    )
                },
            )
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(contentColor.copy(alpha = AVATAR_BACKGROUND_ALPHA)),
            contentAlignment = Alignment.Center,
        ) {
            if (profile.initials.isEmpty()) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = contentColor,
                )
            } else {
                Text(
                    text = profile.initials,
                    style = MaterialTheme.typography.headlineSmall,
                    color = contentColor,
                )
            }
        }
        Text(
            text = displayName,
            style = MaterialTheme.typography.headlineSmall,
            color = contentColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        profile.sharedPhone?.let { phone ->
            ContactCardRow(icon = Icons.Filled.Phone, text = phone, contentColor = contentColor)
        }
        profile.sharedEmail?.let { email ->
            ContactCardRow(icon = Icons.Filled.Email, text = email, contentColor = contentColor)
        }
    }
}

@Composable
private fun ContactCardRow(icon: ImageVector, text: String, contentColor: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = contentColor)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Modifier.dashedOutline(color: Color): Modifier {
    val density = LocalDensity.current
    val strokeWidth = with(density) { CardBorderWidth.toPx() }
    val dash = with(density) { CardBorderDash.toPx() }
    val corner = with(density) { CardCorner.toPx() }

    return drawBehind {
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(
                width = strokeWidth,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
            ),
        )
    }
}

internal fun RgbColor.toComposeColor(): Color = Color(argb)

private const val GHOST_CONTENT_ALPHA = 0.55f
private const val AVATAR_BACKGROUND_ALPHA = 0.22f

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
                    phone = "+34600123456",
                    email = "isidro@example.com",
                    sharePhone = true,
                    shareEmail = true,
                    card = CardPalette.Default,
                ),
            )
            ContactCard(profile = Profile(name = ""), placeholder = true)
        }
    }
}
