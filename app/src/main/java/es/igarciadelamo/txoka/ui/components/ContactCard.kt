package es.igarciadelamo.txoka.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.profile.CardContrast
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.domain.profile.RgbColor
import es.igarciadelamo.txoka.domain.profile.SocialHandle
import es.igarciadelamo.txoka.domain.profile.SocialNetwork
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
 * Content colour that reads on top of the card gradient, mirrored by the hub overlay actions.
 */
fun Profile.cardContentColor(): Color = CardContrast.contentColor(card).toComposeColor()

/** Translucent decoration that never lowers the contrast of [cardContentColor]. */
fun Profile.cardOverlayTint(): Color = if (cardContentColor() == Color.White) Color.Black else Color.White

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
    val contentColor = if (placeholder) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = GHOST_CONTENT_ALPHA)
    } else {
        profile.cardContentColor()
    }
    val overlayTint = if (placeholder) Color.White else profile.cardOverlayTint()
    val accent = profile.card.accent.toComposeColor()
    val details = profile.details()

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // The ratio is a minimum: a long name or a long email grows the card instead of
                // being clipped by a fixed height. Sparse cards hug their content so a single
                // detail never leaves a dead gap at the bottom.
                .heightIn(min = layout.minHeight(width = maxWidth, rows = details.rowCount))
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
                            .highlight(overlayTint)
                    },
                )
                // TalkBack reads the card as one unit instead of five loose labels.
                .semantics(mergeDescendants = true) {},
        ) {
            when (layout) {
                CardLayout.Hero -> HeroContent(
                    profile = profile,
                    contentColor = contentColor,
                    overlayTint = overlayTint,
                )
                CardLayout.Preview -> PreviewContent(
                    profile = profile,
                    contentColor = contentColor,
                    overlayTint = overlayTint,
                )
            }
            if (!placeholder) {
                Text(
                    text = TXOKA_MARK,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 16.dp)
                        .clearAndSetSemantics {},
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                    color = contentColor.copy(alpha = BRANDMARK_ALPHA),
                )
            }
        }
    }
}

/** Width-to-height ratio of each layout, used as the minimum height of a full card. */
private val CardLayout.aspectRatio: Float
    get() = when (this) {
        CardLayout.Hero -> HERO_ASPECT_RATIO
        CardLayout.Preview -> PREVIEW_ASPECT_RATIO
    }

/** Height a card with fewer than [DENSE_ROW_COUNT] detail rows falls back to. */
private val CardLayout.sparseMinHeight: Dp
    get() = when (this) {
        CardLayout.Hero -> SparseHeroMinHeight
        CardLayout.Preview -> SparsePreviewMinHeight
    }

private fun CardLayout.minHeight(width: Dp, rows: Int): Dp =
    if (rows >= DENSE_ROW_COUNT) width / aspectRatio else sparseMinHeight

/** Details a card can show, in paint order. */
private data class CardDetails(val phone: String?, val email: String?, val socials: List<SocialDetail>) {
    val isEmpty: Boolean get() = phone == null && email == null && socials.isEmpty()

    /** Without a phone or an email the handles are the only contact, so they lead the card. */
    val promotesSocials: Boolean get() = phone == null && email == null && socials.isNotEmpty()

    /** Detail rows on screen, which decide how tall the card has to be. */
    val rowCount: Int
        get() = listOfNotNull(phone, email).size + when {
            socials.isEmpty() -> 0
            promotesSocials -> socials.size
            else -> 1
        }
}

private data class SocialDetail(val network: SocialNetwork, val handle: String)

private fun Profile.details(): CardDetails = CardDetails(
    phone = phone?.takeIf { it.isNotBlank() },
    email = email?.takeIf { it.isNotBlank() },
    socials = listOfNotNull(
        linkedin?.takeIf { it.isNotBlank() }?.let { SocialDetail(SocialNetwork.LinkedIn, it) },
        instagram?.takeIf { it.isNotBlank() }?.let { SocialDetail(SocialNetwork.Instagram, it) },
    ),
)

@Composable
private fun HeroContent(profile: Profile, contentColor: Color, overlayTint: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 20.dp),
    ) {
        Avatar(profile = profile, size = 52.dp, contentColor = contentColor, overlayTint = overlayTint)
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
        ContactRows(profile = profile, contentColor = contentColor, overlayTint = overlayTint)
    }
}

@Composable
private fun PreviewContent(profile: Profile, contentColor: Color, overlayTint: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(profile = profile, size = 46.dp, contentColor = contentColor, overlayTint = overlayTint)
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
        ContactRows(profile = profile, contentColor = contentColor, overlayTint = overlayTint)
    }
}

@Composable
private fun Avatar(profile: Profile, size: Dp, contentColor: Color, overlayTint: Color) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(overlayTint.copy(alpha = AVATAR_BACKGROUND_ALPHA))
            .border(width = 1.dp, color = contentColor.copy(alpha = AVATAR_RING_ALPHA), shape = CircleShape)
            // Initials are decoration: the name follows right away.
            .clearAndSetSemantics {},
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
private fun ContactRows(profile: Profile, contentColor: Color, overlayTint: Color) {
    val details = profile.details()
    if (details.isEmpty) return

    Column(
        modifier = Modifier.padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (details.promotesSocials) {
            // No phone or email: each handle takes a full row and the title size, because it is the
            // only way to reach the owner instead of a small footnote under the name.
            details.socials.forEach { social ->
                SocialPill(
                    social = social,
                    contentColor = contentColor,
                    overlayTint = overlayTint,
                    modifier = Modifier.fillMaxWidth(),
                    prominent = true,
                )
            }
        } else {
            details.phone?.let { phone ->
                ContactPill(text = phone, contentColor = contentColor, overlayTint = overlayTint) { size ->
                    Icon(
                        imageVector = Icons.Filled.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(size),
                        tint = contentColor,
                    )
                }
            }
            details.email?.let { email ->
                ContactPill(text = email, contentColor = contentColor, overlayTint = overlayTint) { size ->
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = null,
                        modifier = Modifier.size(size),
                        tint = contentColor,
                    )
                }
            }
            if (details.socials.isNotEmpty()) {
                // Both handles share one row and carry only the username, never the full link.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    details.socials.forEach { social ->
                        SocialPill(
                            social = social,
                            contentColor = contentColor,
                            overlayTint = overlayTint,
                            modifier = if (details.socials.size > 1) Modifier.weight(1f) else Modifier,
                        )
                    }
                }
            }
        }
        // Keeps the pills clear of the brandmark anchored to the bottom-right corner.
        Spacer(modifier = Modifier.height(BrandmarkReserve))
    }
}

@Composable
private fun SocialPill(
    social: SocialDetail,
    contentColor: Color,
    overlayTint: Color,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
) {
    val description = stringResource(
        R.string.social_handle_description,
        social.network.label(),
        SocialHandle.display(social.network, social.handle),
    )
    ContactPill(
        text = SocialHandle.display(social.network, social.handle),
        contentColor = contentColor,
        overlayTint = overlayTint,
        // The icon says which network it is, which TalkBack cannot see.
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        prominent = prominent,
    ) { size ->
        SocialIcon(network = social.network, tint = contentColor, modifier = Modifier.size(size))
    }
}

@Composable
private fun ContactPill(
    text: String,
    contentColor: Color,
    overlayTint: Color,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    icon: @Composable (Dp) -> Unit,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(overlayTint.copy(alpha = PILL_BACKGROUND_ALPHA))
            .padding(
                start = if (prominent) 16.dp else 12.dp,
                end = if (prominent) 18.dp else 14.dp,
                top = if (prominent) 9.dp else 6.dp,
                bottom = if (prominent) 9.dp else 6.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(if (prominent) 10.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon(if (prominent) ProminentIconSize else PillIconSize)
        Text(
            text = text,
            style = if (prominent) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Profile.displayName(): String = name.ifBlank { stringResource(R.string.profile_name_placeholder) }

/**
 * Soft light from the top-left corner, which gives the flat gradient some depth. The tint goes
 * against the content colour, so the glow always pushes the text towards more contrast.
 */
private fun Modifier.highlight(tint: Color): Modifier = drawWithCache {
    val glow = Brush.radialGradient(
        colors = listOf(tint.copy(alpha = HIGHLIGHT_ALPHA), Color.Transparent),
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

/** Rows a card needs before it keeps the layout ratio instead of hugging its content. */
private const val DENSE_ROW_COUNT = 2
private val SparseHeroMinHeight = 176.dp
private val SparsePreviewMinHeight = 152.dp
private val PillIconSize = 16.dp
private val ProminentIconSize = 20.dp
private val BrandmarkReserve = 16.dp
private const val TXOKA_MARK = "TXOKA"
private const val GHOST_CONTENT_ALPHA = 0.55f
private const val AVATAR_BACKGROUND_ALPHA = 0.16f
private const val AVATAR_RING_ALPHA = 0.26f
private const val PILL_BACKGROUND_ALPHA = 0.14f
private const val HIGHLIGHT_ALPHA = 0.30f
private const val HIGHLIGHT_X = 0.08f
private const val BRANDMARK_ALPHA = 0.45f

@Preview(showBackground = true, heightDp = 1600)
@Composable
private fun ContactCardMatrixPreview() {
    TxokaTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Every detail combination the card has to survive, from full to name only.
            ContactCard(profile = fullProfile())
            ContactCard(
                profile = Profile(
                    name = "María del Carmen Fernández de la Vega",
                    linkedin = "maria-fernandez",
                    instagram = "maria.png",
                ),
            )
            ContactCard(profile = Profile(name = "Marta Ruiz", instagram = "marta.ruiz", card = CardPalette.Presets[2]))
            ContactCard(profile = Profile(name = "Marta Ruiz", linkedin = "marta-ruiz", card = CardPalette.Presets[4]))
            ContactCard(profile = Profile(name = "Isidro García", email = "isidro@example.com"))
            ContactCard(profile = Profile(name = "Isidro García", phone = "+34 600 123 456"))
            ContactCard(profile = Profile(name = "Isidro García", phone = "+34 600 123 456", instagram = "isidro.png"))
            ContactCard(profile = Profile(name = "Legacy contact", card = CardPalette.Presets[5]))
        }
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun ContactCardPreviewMatrixPreview() {
    TxokaTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ContactCard(profile = fullProfile(), layout = CardLayout.Preview)
            ContactCard(
                profile = Profile(name = "Marta Ruiz", instagram = "marta.ruiz", card = CardPalette.Presets[2]),
                layout = CardLayout.Preview,
            )
            ContactCard(
                profile = Profile(name = "Marta Ruiz", linkedin = "marta-ruiz", instagram = "marta.ruiz"),
                layout = CardLayout.Preview,
            )
            ContactCard(
                profile = Profile(name = "Isidro García", phone = "+34 600 123 456"),
                layout = CardLayout.Preview,
            )
            ContactCard(profile = Profile(name = ""), placeholder = true, layout = CardLayout.Preview)
        }
    }
}

private fun fullProfile(): Profile = Profile(
    name = "Isidro García",
    phone = "+34 600 123 456",
    email = "isidro@example.com",
    linkedin = "isidro-garcia",
    instagram = "isidro.png",
    card = CardPalette.Default,
)
