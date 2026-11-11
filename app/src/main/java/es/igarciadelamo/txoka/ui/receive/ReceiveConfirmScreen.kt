package es.igarciadelamo.txoka.ui.receive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import es.igarciadelamo.txoka.ui.components.CardLayout
import es.igarciadelamo.txoka.ui.components.ContactCard
import es.igarciadelamo.txoka.ui.components.NfcWaves
import es.igarciadelamo.txoka.ui.components.PrimaryButton
import es.igarciadelamo.txoka.ui.components.toComposeColor
import es.igarciadelamo.txoka.ui.theme.Corners
import es.igarciadelamo.txoka.ui.theme.Spacing
import es.igarciadelamo.txoka.ui.theme.TxokaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveConfirmScreen(
    payload: VCardPayload,
    alreadyStored: Boolean,
    waiting: Int,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDiscard,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = Corners.card, topEnd = Corners.card),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            // The body scrolls and the actions stay pinned: in landscape the sheet is shorter than
            // its content, and the user must still be able to accept or discard the card.
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                IncomingHeader(payload = payload, alreadyStored = alreadyStored)

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NfcWaves(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text(
                        text = stringResource(R.string.sheet_shared_via_nfc),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                ContactCard(profile = payload.toProfile(), layout = CardLayout.Preview)
            }

            Column(
                modifier = Modifier.padding(horizontal = Spacing.xl, vertical = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                if (waiting > 0) {
                    Text(
                        text = pluralStringResource(R.plurals.sheet_waiting_cards, waiting, waiting),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                PrimaryButton(
                    text = if (alreadyStored) {
                        stringResource(R.string.action_update_contact)
                    } else {
                        stringResource(R.string.action_save_contact)
                    },
                    onClick = onSave,
                )
                TextButton(
                    onClick = onDiscard,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.action_discard),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun IncomingHeader(payload: VCardPayload, alreadyStored: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        SenderAvatar(payload = payload)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (alreadyStored) {
                    stringResource(R.string.sheet_incoming_title_existing, payload.name)
                } else {
                    stringResource(R.string.sheet_incoming_title, payload.name)
                },
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = if (alreadyStored) {
                    stringResource(R.string.sheet_incoming_body_existing)
                } else {
                    stringResource(R.string.sheet_incoming_body)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SenderAvatar(payload: VCardPayload) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(payload.palette.accent.toComposeColor(), payload.palette.accent2.toComposeColor()),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = payload.initials(),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.surface,
        )
    }
}

private fun VCardPayload.initials(): String = name
    .split(' ')
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { word -> word.first().uppercase() }

private fun VCardPayload.toProfile(): Profile = Profile(
    name = name,
    phone = phone,
    email = email,
    linkedin = linkedin,
    instagram = instagram,
    card = palette,
)

@Preview(showBackground = true)
@Composable
private fun ReceiveConfirmScreenPreview() {
    TxokaTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(
                text = stringResource(R.string.sheet_incoming_title, "Marta Ruiz"),
                style = MaterialTheme.typography.titleLarge,
            )
            ContactCard(
                profile = VCardPayload(
                    name = "Marta Ruiz",
                    phone = "+34 600 111 222",
                    email = "marta@example.com",
                    linkedin = "marta-ruiz",
                    instagram = "marta.ruiz",
                    palette = CardPalette.Presets[2],
                ).toProfile(),
                layout = CardLayout.Preview,
            )
        }
    }
}
