package es.igarciadelamo.txoka.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.nfc.NfcStatus
import es.igarciadelamo.txoka.ui.theme.TxokaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onSharingChanged: (Boolean) -> Unit,
    onRequireUnlockChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(
                            if (state.hasProfile) R.string.action_edit_profile else R.string.action_create_card,
                        ),
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEditProfile),
            )

            // Sharing controls make no sense on a device without NFC hardware.
            if (state.nfc != NfcStatus.Unavailable) {
                HorizontalDivider()
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

            if (state.nfc == NfcStatus.Disabled) {
                NfcDisabledNotice(onOpenNfcSettings = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) })
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
private fun NfcDisabledNotice(onOpenNfcSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.nfc_disabled),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Button(onClick = onOpenNfcSettings) {
            Text(text = stringResource(R.string.action_open_settings))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    TxokaTheme {
        SettingsScreen(
            state = SettingsUiState(hasProfile = true, nfc = NfcStatus.Enabled, isLoading = false),
            onBack = {},
            onEditProfile = {},
            onSharingChanged = {},
            onRequireUnlockChanged = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenNoNfcPreview() {
    TxokaTheme {
        SettingsScreen(
            state = SettingsUiState(hasProfile = false, nfc = NfcStatus.Unavailable, isLoading = false),
            onBack = {},
            onEditProfile = {},
            onSharingChanged = {},
            onRequireUnlockChanged = {},
        )
    }
}
