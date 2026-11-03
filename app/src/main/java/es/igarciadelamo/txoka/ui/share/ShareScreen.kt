package es.igarciadelamo.txoka.ui.share

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.nfc.NfcStatus
import es.igarciadelamo.txoka.ui.components.CardLayout
import es.igarciadelamo.txoka.ui.components.ContactCard
import es.igarciadelamo.txoka.ui.components.Eyebrow
import es.igarciadelamo.txoka.ui.components.NfcWaves
import es.igarciadelamo.txoka.ui.components.PrimaryButton
import es.igarciadelamo.txoka.ui.components.TxokaAppBar
import es.igarciadelamo.txoka.ui.components.cardContentColor
import es.igarciadelamo.txoka.ui.components.cardOverlayTint
import es.igarciadelamo.txoka.ui.theme.Corners
import es.igarciadelamo.txoka.ui.theme.Spacing
import es.igarciadelamo.txoka.ui.theme.TxokaTheme
import es.igarciadelamo.txoka.ui.theme.txoka

@Composable
fun ShareScreen(
    profile: Profile?,
    sharingEnabled: Boolean,
    requireUnlock: Boolean,
    nfc: NfcStatus,
    onOpenReceived: () -> Unit,
    onOpenSettings: () -> Unit,
    onCreateCard: () -> Unit,
    onEditProfile: () -> Unit,
    onSharingChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    if (profile != null) {
        KeepScreenOn(enabled = sharingEnabled)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TxokaAppBar {
            IconButton(onClick = onOpenReceived) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = stringResource(R.string.settings_received),
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.action_settings),
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = Spacing.xl),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                if (profile == null) {
                    EmptyHub(onCreateCard = onCreateCard)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                        Column {
                            Eyebrow(text = stringResource(R.string.hub_your_card))
                            Box(modifier = Modifier.padding(top = Spacing.sm)) {
                                ContactCard(
                                    profile = profile,
                                    layout = CardLayout.Hero,
                                )
                                IconButton(
                                    onClick = onEditProfile,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(Spacing.lg)
                                        .clip(CircleShape)
                                        .background(profile.cardOverlayTint().copy(alpha = 0.18f)),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = stringResource(R.string.action_edit_card),
                                        tint = profile.cardContentColor(),
                                    )
                                }
                            }
                        }
                        SharingModule(
                            sharingEnabled = sharingEnabled,
                            requireUnlock = requireUnlock,
                            nfc = nfc,
                            onSharingChanged = onSharingChanged,
                            onOpenNfcSettings = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                        )
                    }
                }
            }
        }

        PrivacyFooter()
    }
}

@Composable
private fun SharingModule(
    sharingEnabled: Boolean,
    requireUnlock: Boolean,
    nfc: NfcStatus,
    onSharingChanged: (Boolean) -> Unit,
    onOpenNfcSettings: () -> Unit,
) {
    val extras = MaterialTheme.txoka
    val unavailable = nfc == NfcStatus.Unavailable
    val tint = if (nfc == NfcStatus.Enabled) MaterialTheme.colorScheme.primary else extras.warning
    val background = if (nfc == NfcStatus.Enabled) extras.accentSoft else extras.warningSoft

    val title = when {
        unavailable -> stringResource(R.string.nfc_unavailable_title)
        nfc == NfcStatus.Disabled -> stringResource(R.string.nfc_disabled_title)
        else -> stringResource(R.string.share_toggle)
    }
    val hint = when {
        unavailable -> stringResource(R.string.nfc_unavailable_body)
        nfc == NfcStatus.Disabled -> stringResource(R.string.nfc_disabled_body)
        !sharingEnabled -> stringResource(R.string.share_hint_disabled)
        requireUnlock -> stringResource(R.string.share_module_unlock_hint)
        else -> stringResource(R.string.share_module_hint)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Corners.sheet))
            .background(MaterialTheme.colorScheme.surface)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        val switchEnabled = !unavailable && nfc == NfcStatus.Enabled
        Row(
            modifier = Modifier.toggleable(
                value = sharingEnabled,
                enabled = switchEnabled,
                role = Role.Switch,
                onValueChange = onSharingChanged,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(Corners.field))
                    .background(background),
                contentAlignment = Alignment.Center,
            ) {
                NfcWaves(color = tint)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Spacing.md, end = Spacing.sm),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!unavailable) {
                Switch(
                    checked = sharingEnabled,
                    onCheckedChange = null,
                    enabled = switchEnabled,
                )
            }
        }
        if (nfc == NfcStatus.Disabled) {
            TextButton(onClick = onOpenNfcSettings) {
                Text(text = stringResource(R.string.action_open_settings))
            }
        }
    }
}

@Composable
private fun EmptyHub(onCreateCard: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        EmptyIllustration()
        Text(
            text = stringResource(R.string.hub_empty_title),
            modifier = Modifier.padding(top = Spacing.sm),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.hub_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PrimaryButton(
            text = stringResource(R.string.action_create_card),
            onClick = onCreateCard,
            modifier = Modifier.padding(top = Spacing.md),
        )
    }
}

@Composable
private fun EmptyIllustration() {
    val shape = RoundedCornerShape(18.dp)
    val gradient = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))

    Box(modifier = Modifier.size(width = 200.dp, height = 160.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .offset(x = (-14).dp, y = (-14).dp)
                .size(width = 128.dp, height = 82.dp)
                .rotate(-11f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.primaryContainer),
        )
        Box(
            modifier = Modifier
                .offset(x = 12.dp, y = 10.dp)
                .size(width = 132.dp, height = 86.dp)
                .rotate(6f)
                .clip(shape)
                .background(gradient),
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, top = 16.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f)),
            )
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, top = 54.dp)
                    .size(width = 56.dp, height = 7.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.6f)),
            )
        }
        NfcWaves(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(56.dp),
        )
    }
}

@Composable
private fun PrivacyFooter() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.hub_privacy_footer),
            modifier = Modifier.padding(start = Spacing.xs),
            style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.1.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShareScreenPreview() {
    TxokaTheme {
        ShareScreen(
            profile = Profile(
                name = "Isidro García",
                phone = "+34 600 123 456",
                email = "isidro@example.com",
                card = CardPalette.Default,
            ),
            sharingEnabled = true,
            requireUnlock = false,
            nfc = NfcStatus.Enabled,
            onOpenReceived = {},
            onOpenSettings = {},
            onCreateCard = {},
            onEditProfile = {},
            onSharingChanged = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShareScreenNoCardPreview() {
    TxokaTheme {
        ShareScreen(
            profile = null,
            sharingEnabled = false,
            requireUnlock = false,
            nfc = NfcStatus.Unavailable,
            onOpenReceived = {},
            onOpenSettings = {},
            onCreateCard = {},
            onEditProfile = {},
            onSharingChanged = {},
        )
    }
}
