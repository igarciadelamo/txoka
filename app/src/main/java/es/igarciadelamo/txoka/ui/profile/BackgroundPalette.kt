package es.igarciadelamo.txoka.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.profile.CardAppearance
import es.igarciadelamo.txoka.domain.profile.CardBackground
import es.igarciadelamo.txoka.domain.profile.CardEffect
import es.igarciadelamo.txoka.domain.profile.CardMosaic
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.RgbColor
import es.igarciadelamo.txoka.ui.components.CardSurface
import es.igarciadelamo.txoka.ui.components.PHYSICAL_CARD_RATIO
import es.igarciadelamo.txoka.ui.components.hasNativeTexture
import es.igarciadelamo.txoka.ui.components.toComposeColor

@Composable
internal fun BackgroundPalette(
    card: CardPalette,
    appearance: CardAppearance,
    onChange: (CardPalette) -> Unit,
    onColour: (CardColourTarget) -> Unit,
) {
    val flat = appearance.background == CardBackground.FLAT
    val single = flat && appearance.mosaic == CardMosaic.NONE
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier.width(176.dp).aspectRatio(PHYSICAL_CARD_RATIO)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .semantics { isTraversalGroup = true },
        ) {
            CardSurface(
                palette = card.copy(appearance = appearance.copy(effect = CardEffect.NONE)),
                modifier = Modifier.fillMaxSize(),
            )
            val positions = when {
                single -> listOf(CardColourTarget.FIRST to Alignment.Center)
                appearance.hasFourCorners -> listOf(
                    CardColourTarget.FIRST to AbsoluteAlignment.TopLeft,
                    CardColourTarget.TOP_RIGHT to AbsoluteAlignment.TopRight,
                    CardColourTarget.BOTTOM_LEFT to AbsoluteAlignment.BottomLeft,
                    CardColourTarget.SECOND to AbsoluteAlignment.BottomRight,
                )
                else -> listOf(
                    CardColourTarget.FIRST to AbsoluteAlignment.TopLeft,
                    CardColourTarget.SECOND to AbsoluteAlignment.BottomRight,
                )
            }
            positions.forEach { (target, position) ->
                PaletteHandle(
                    color = card.colourAt(target),
                    label = stringResource(target.labelFor(appearance)),
                    modifier = Modifier.align(position),
                    onClick = { onColour(target) },
                )
            }
        }
        Column(Modifier.widthIn(max = 154.dp)) {
            val title = when {
                appearance.background.hasNativeTexture -> R.string.appearance_material_tints
                flat && !single -> R.string.appearance_mosaic_and_background
                else -> R.string.appearance_background_colour
            }
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            if (appearance.background.hasNativeTexture) {
                Text(
                    stringResource(R.string.appearance_material_tints_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!flat) {
                Column(
                    modifier = Modifier.toggleable(
                        value = appearance.hasFourCorners,
                        role = Role.Switch,
                        onValueChange = { enabled ->
                            val next = appearance.copy(
                                topRight = if (enabled) card.accent2 else null,
                                bottomLeft = if (enabled) card.accent else null,
                            )
                            onChange(card.copy(appearance = next))
                        },
                    ).padding(vertical = 4.dp),
                ) {
                    Text(stringResource(R.string.appearance_four_corners), style = MaterialTheme.typography.labelMedium)
                    Switch(checked = appearance.hasFourCorners, onCheckedChange = null)
                }
            }
        }
    }
}

@Composable
private fun PaletteHandle(color: RgbColor, label: String, modifier: Modifier, onClick: () -> Unit) {
    val description = stringResource(R.string.appearance_edit_palette_colour, label, color.hex)
    val fill = color.toComposeColor()
    Box(
        modifier = modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(32.dp)
                .border(1.dp, Color.Black, CircleShape).padding(1.dp)
                .border(2.dp, Color.White, CircleShape).background(fill, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                tint = if (fill.luminance() > .18f) Color.Black else Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

internal fun CardColourTarget.labelFor(appearance: CardAppearance): Int = when {
    this != CardColourTarget.FIRST && this != CardColourTarget.SECOND -> label
    appearance.background.hasNativeTexture -> label
    appearance.background == CardBackground.FLAT -> {
        if (this == CardColourTarget.FIRST && appearance.mosaic != CardMosaic.NONE) {
            R.string.appearance_mosaic
        } else {
            R.string.appearance_background
        }
    }
    this == CardColourTarget.FIRST -> R.string.appearance_top_left
    else -> R.string.appearance_bottom_right
}
