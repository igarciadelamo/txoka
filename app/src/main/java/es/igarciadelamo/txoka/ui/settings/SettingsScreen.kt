package es.igarciadelamo.txoka.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.igarciadelamo.txoka.BuildConfig
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.nfc.NfcStatus
import es.igarciadelamo.txoka.ui.components.Eyebrow
import es.igarciadelamo.txoka.ui.components.NfcWaves
import es.igarciadelamo.txoka.ui.components.ScreenTopBar
import es.igarciadelamo.txoka.ui.theme.Corners
import es.igarciadelamo.txoka.ui.theme.Spacing
import es.igarciadelamo.txoka.ui.theme.TxokaTheme
import es.igarciadelamo.txoka.ui.theme.txoka

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onOpenReceived: () -> Unit,
    onOpenAbout: () -> Unit,
    onSharingChanged: (Boolean) -> Unit,
    onRequireUnlockChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onSimulateIncomingCard: () -> Unit = {},
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ScreenTopBar(title = stringResource(R.string.settings_title), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = Spacing.xl, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            if (state.nfc == NfcStatus.Disabled) {
                WarningCard(
                    title = stringResource(R.string.nfc_disabled),
                    body = stringResource(R.string.nfc_disabled_body),
                    action = stringResource(R.string.action_open_settings),
                    onAction = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                )
            }

            // Sharing controls make no sense on a device without NFC hardware.
            if (state.nfc != NfcStatus.Unavailable) {
                SharingGroup(
                    state = state,
                    onSharingChanged = onSharingChanged,
                    onRequireUnlockChanged = onRequireUnlockChanged,
                )
            }

            LibraryGroup(
                receivedCount = state.receivedCount,
                onOpenReceived = onOpenReceived,
                onSimulateIncomingCard = onSimulateIncomingCard,
            )

            PrivacyGroup()

            AboutGroup(version = BuildConfig.VERSION_NAME, onOpenAbout = onOpenAbout)
        }
    }
}

@Composable
private fun SharingGroup(
    state: SettingsUiState,
    onSharingChanged: (Boolean) -> Unit,
    onRequireUnlockChanged: (Boolean) -> Unit,
) {
    SettingsGroup(title = stringResource(R.string.settings_sharing)) {
        SwitchRow(
            title = stringResource(R.string.share_toggle),
            checked = state.sharingEnabled,
            enabled = state.nfc == NfcStatus.Enabled,
            onCheckedChange = onSharingChanged,
            leading = { NfcWaves(color = MaterialTheme.colorScheme.primary) },
        )
        SwitchRow(
            title = stringResource(R.string.share_require_unlock),
            checked = state.requireUnlock,
            enabled = state.sharingEnabled && state.nfc == NfcStatus.Enabled,
            onCheckedChange = onRequireUnlockChanged,
            leading = {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
        )
    }
}

@Composable
private fun LibraryGroup(receivedCount: Int, onOpenReceived: () -> Unit, onSimulateIncomingCard: () -> Unit) {
    SettingsGroup(title = stringResource(R.string.settings_library)) {
        SettingsRow(
            title = stringResource(R.string.settings_received),
            subtitle = pluralStringResource(R.plurals.received_count, receivedCount, receivedCount),
            onClick = onOpenReceived,
            fallbackIcon = Icons.Filled.Person,
        )
        if (BuildConfig.DEBUG) {
            SettingsRow(
                title = stringResource(R.string.settings_demo_card),
                subtitle = stringResource(R.string.settings_debug_only),
                onClick = onSimulateIncomingCard,
                fallbackIcon = Icons.Filled.Add,
                tint = MaterialTheme.txoka.warning,
                background = MaterialTheme.txoka.warningSoft,
            )
        }
    }
}

@Composable
private fun PrivacyGroup() {
    SettingsGroup(title = stringResource(R.string.settings_privacy)) {
        InfoRow(
            icon = Icons.Filled.Lock,
            title = stringResource(R.string.settings_no_internet_title),
            body = stringResource(R.string.settings_no_internet_body),
        )
        InfoRow(
            icon = Icons.Filled.Person,
            title = stringResource(R.string.settings_storage_title),
            body = stringResource(R.string.settings_storage_body),
        )
    }
}

@Composable
private fun AboutGroup(version: String, onOpenAbout: () -> Unit) {
    SettingsGroup(title = stringResource(R.string.settings_about)) {
        SettingsRow(
            title = stringResource(R.string.app_name),
            subtitle = stringResource(R.string.settings_version, version),
            onClick = onOpenAbout,
            fallbackIcon = Icons.Filled.Info,
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Eyebrow(text = title)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Corners.sheet))
                .background(MaterialTheme.colorScheme.surface),
            content = { content() },
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    onClick: (() -> Unit)?,
    fallbackIcon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    background: Color = MaterialTheme.txoka.accentSoft,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, role = Role.Button, onClick = { onClick?.invoke() })
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(background = background) {
            if (leading != null) {
                leading()
            } else {
                Icon(
                    imageVector = fallbackIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = tint,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        IconBadge {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun IconBadge(background: Color = MaterialTheme.txoka.accentSoft, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(Corners.chip))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    leading: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge { leading() }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun WarningCard(title: String, body: String, action: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Corners.sheet))
            .background(MaterialTheme.colorScheme.surface)
            .padding(Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NfcWaves(color = MaterialTheme.txoka.warning)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = action,
                modifier = Modifier
                    .padding(top = Spacing.xs)
                    .clip(RoundedCornerShape(Corners.chip))
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(vertical = Spacing.xs),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    TxokaTheme {
        SettingsScreen(
            state = SettingsUiState(hasProfile = true, nfc = NfcStatus.Enabled, receivedCount = 3),
            onBack = {},
            onOpenReceived = {},
            onOpenAbout = {},
            onSharingChanged = {},
            onRequireUnlockChanged = {},
        )
    }
}
