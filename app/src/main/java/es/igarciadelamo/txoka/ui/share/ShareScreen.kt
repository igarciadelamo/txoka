package es.igarciadelamo.txoka.ui.share

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.nfc.NfcStatus
import es.igarciadelamo.txoka.ui.components.ContactCard
import es.igarciadelamo.txoka.ui.theme.TxokaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(
    profile: Profile?,
    state: ShareUiState,
    onSharingChanged: (Boolean) -> Unit,
    onRequireUnlockChanged: (Boolean) -> Unit,
    onOpenReceived: () -> Unit,
    onOpenSettings: () -> Unit,
    onCreateCard: () -> Unit,
    modifier: Modifier = Modifier,
    receivedCount: Int = 0,
) {
    val context = LocalContext.current

    if (profile != null) {
        KeepScreenOn(enabled = state.sharingEnabled)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenReceived) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ) {
                                    Text(text = receivedCount.toString())
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = stringResource(R.string.action_received),
                            )
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.action_settings),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                if (profile == null) {
                    NoCardState(onCreateCard = onCreateCard)
                } else {
                    ShareCardState(profile = profile, sharingEnabled = state.sharingEnabled)
                }
            }

            if (state.nfc != NfcStatus.Enabled) {
                NfcBanner(
                    status = state.nfc,
                    onOpenNfcSettings = {
                        context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                    },
                )
            }

            if (profile != null) {
                ShareControls(
                    state = state,
                    onSharingChanged = onSharingChanged,
                    onRequireUnlockChanged = onRequireUnlockChanged,
                )
            }
        }
    }
}

@Composable
private fun ShareCardState(profile: Profile, sharingEnabled: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ContactCard(profile = profile)
        Text(
            text = stringResource(if (sharingEnabled) R.string.share_hint else R.string.share_hint_disabled),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NoCardState(onCreateCard: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.share_no_card_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.share_no_card_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onCreateCard) {
            Text(text = stringResource(R.string.action_create_card))
        }
    }
}

@Composable
private fun ShareControls(
    state: ShareUiState,
    onSharingChanged: (Boolean) -> Unit,
    onRequireUnlockChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SwitchRow(
            label = stringResource(R.string.share_toggle),
            checked = state.sharingEnabled,
            onCheckedChange = onSharingChanged,
        )
        SwitchRow(
            label = stringResource(R.string.share_require_unlock),
            checked = state.requireUnlock,
            enabled = state.sharingEnabled,
            onCheckedChange = onRequireUnlockChanged,
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun NfcBanner(status: NfcStatus, onOpenNfcSettings: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(
                if (status == NfcStatus.Unavailable) R.string.nfc_unavailable else R.string.nfc_disabled,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        if (status == NfcStatus.Disabled) {
            Button(onClick = onOpenNfcSettings) {
                Text(text = stringResource(R.string.action_open_settings))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShareScreenPreview() {
    TxokaTheme {
        ShareScreen(
            profile = Profile(
                name = "Isidro García",
                phone = "+34600123456",
                email = "isidro@example.com",
                card = CardPalette.Default,
            ),
            state = ShareUiState(),
            onSharingChanged = {},
            onRequireUnlockChanged = {},
            onOpenReceived = {},
            onOpenSettings = {},
            onCreateCard = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShareScreenNoCardPreview() {
    TxokaTheme {
        ShareScreen(
            profile = null,
            state = ShareUiState(nfc = NfcStatus.Disabled),
            onSharingChanged = {},
            onRequireUnlockChanged = {},
            onOpenReceived = {},
            onOpenSettings = {},
            onCreateCard = {},
        )
    }
}
