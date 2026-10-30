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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import es.igarciadelamo.txoka.ui.components.SurfaceButton
import es.igarciadelamo.txoka.ui.components.TxokaHeader
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
    receivedCount: Int,
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
        TxokaHeader {
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
                            ContactCard(
                                profile = profile,
                                modifier = Modifier.padding(top = Spacing.sm),
                                layout = CardLayout.Hero,
                            )
                        }
                        SharingModule(
                            sharingEnabled = sharingEnabled,
                            requireUnlock = requireUnlock,
                            nfc = nfc,
                            onSharingChanged = onSharingChanged,
                            onOpenNfcSettings = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                        )
                        QuickActions(
                            receivedCount = receivedCount,
                            onOpenReceived = onOpenReceived,
                            onOpenMyCard = onEditProfile,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                    onCheckedChange = onSharingChanged,
                    enabled = nfc == NfcStatus.Enabled,
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
private fun QuickActions(receivedCount: Int, onOpenReceived: () -> Unit, onOpenMyCard: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        SurfaceButton(
            text = stringResource(R.string.hub_my_card),
            onClick = onOpenMyCard,
            modifier = Modifier.weight(1f),
        )
        SurfaceButton(
            text = stringResource(R.string.hub_received_count, receivedCount),
            onClick = onOpenReceived,
            modifier = Modifier.weight(1f),
        )
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
                .size(width = 132.dp, height = 86.dp)
                .rotate(-9f)
                .clip(shape)
                .background(gradient)
                .alpha(0.22f),
        )
        Box(
            modifier = Modifier
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
                sharePhone = true,
                shareEmail = true,
                card = CardPalette.Default,
            ),
            sharingEnabled = true,
            requireUnlock = false,
            nfc = NfcStatus.Enabled,
            receivedCount = 3,
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
            receivedCount = 0,
            onOpenReceived = {},
            onOpenSettings = {},
            onCreateCard = {},
            onEditProfile = {},
            onSharingChanged = {},
        )
    }
}
