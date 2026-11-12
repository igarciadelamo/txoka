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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import es.igarciadelamo.txoka.domain.profile.ShareRole
import es.igarciadelamo.txoka.nfc.NfcStatus
import es.igarciadelamo.txoka.nfc.ShareRoleState
import es.igarciadelamo.txoka.ui.components.CardLayout
import es.igarciadelamo.txoka.ui.components.ContactCard
import es.igarciadelamo.txoka.ui.components.Eyebrow
import es.igarciadelamo.txoka.ui.components.NfcWaves
import es.igarciadelamo.txoka.ui.components.PrimaryButton
import es.igarciadelamo.txoka.ui.components.TxokaAppBar
import es.igarciadelamo.txoka.ui.components.cardContentColor
import es.igarciadelamo.txoka.ui.components.cardOverlayTint
import es.igarciadelamo.txoka.ui.components.contentWidth
import es.igarciadelamo.txoka.ui.theme.Corners
import es.igarciadelamo.txoka.ui.theme.Spacing
import es.igarciadelamo.txoka.ui.theme.TxokaTheme
import es.igarciadelamo.txoka.ui.theme.txoka

@Composable
fun ShareScreen(
    profile: Profile?,
    role: ShareRoleState,
    requireUnlock: Boolean,
    nfc: NfcStatus,
    onOpenReceived: () -> Unit,
    onOpenSettings: () -> Unit,
    onCreateCard: () -> Unit,
    onEditProfile: () -> Unit,
    onRoleSelected: (ShareRole) -> Unit,
    onCancelReturn: () -> Unit,
    modifier: Modifier = Modifier,
    cardTooLarge: Boolean = false,
) {
    val context = LocalContext.current

    if (profile != null) {
        KeepScreenOn(enabled = nfc == NfcStatus.Enabled)
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
                modifier = Modifier
                    .contentWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                if (profile == null) {
                    EmptyHub(onCreateCard = onCreateCard)
                } else {
                    HubContent(
                        profile = profile,
                        role = role,
                        requireUnlock = requireUnlock,
                        cardTooLarge = cardTooLarge,
                        nfc = nfc,
                        onEditProfile = onEditProfile,
                        onRoleSelected = onRoleSelected,
                        onCancelReturn = onCancelReturn,
                        onOpenNfcSettings = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                    )
                }
            }
        }

        PrivacyFooter()
    }
}

@Composable
private fun HubContent(
    profile: Profile,
    role: ShareRoleState,
    requireUnlock: Boolean,
    cardTooLarge: Boolean,
    nfc: NfcStatus,
    onEditProfile: () -> Unit,
    onRoleSelected: (ShareRole) -> Unit,
    onCancelReturn: () -> Unit,
    onOpenNfcSettings: () -> Unit,
) {
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
            role = role,
            requireUnlock = requireUnlock,
            cardTooLarge = cardTooLarge,
            nfc = nfc,
            onRoleSelected = onRoleSelected,
            onCancelReturn = onCancelReturn,
            onOpenNfcSettings = onOpenNfcSettings,
        )
    }
}

@Composable
private fun SharingModule(
    role: ShareRoleState,
    requireUnlock: Boolean,
    cardTooLarge: Boolean,
    nfc: NfcStatus,
    onRoleSelected: (ShareRole) -> Unit,
    onCancelReturn: () -> Unit,
    onOpenNfcSettings: () -> Unit,
) {
    val extras = MaterialTheme.txoka
    val unavailable = nfc == NfcStatus.Unavailable
    val tint = if (nfc == NfcStatus.Enabled) MaterialTheme.colorScheme.primary else extras.warning
    val background = if (nfc == NfcStatus.Enabled) extras.accentSoft else extras.warningSoft

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Corners.sheet))
            .background(MaterialTheme.colorScheme.surface)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
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
                    .padding(start = Spacing.md),
            ) {
                Text(
                    text = moduleTitle(nfc),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = moduleHint(nfc, role, requireUnlock, cardTooLarge),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    // Two lines are always reserved so switching the role cannot resize the module
                    // and shift the card above it.
                    minLines = 2,
                )
            }
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (role.returning) {
                    IconButton(onClick = onCancelReturn) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.action_cancel),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (!unavailable) {
            RoleSelector(
                role = role.role,
                enabled = nfc == NfcStatus.Enabled,
                onSelect = onRoleSelected,
            )
        }
        if (nfc == NfcStatus.Disabled) {
            TextButton(onClick = onOpenNfcSettings) {
                Text(text = stringResource(R.string.action_open_settings))
            }
        }
    }
}

@Composable
private fun moduleTitle(nfc: NfcStatus): String = when (nfc) {
    NfcStatus.Unavailable -> stringResource(R.string.nfc_unavailable_title)
    NfcStatus.Disabled -> stringResource(R.string.nfc_disabled_title)
    NfcStatus.Enabled -> stringResource(R.string.share_module_title)
}

@Composable
private fun moduleHint(nfc: NfcStatus, role: ShareRoleState, requireUnlock: Boolean, cardTooLarge: Boolean): String =
    when {
        nfc == NfcStatus.Unavailable -> stringResource(R.string.nfc_unavailable_body)
        nfc == NfcStatus.Disabled -> stringResource(R.string.nfc_disabled_body)
        cardTooLarge -> stringResource(R.string.share_module_too_long_hint)
        role.returning && role.remainingSeconds > 0 ->
            stringResource(R.string.share_module_return_hint_countdown, role.remainingSeconds)
        role.returning -> stringResource(R.string.share_module_return_hint)
        role.role == ShareRole.Receive -> stringResource(R.string.share_module_receive_hint)
        requireUnlock -> stringResource(R.string.share_module_unlock_hint)
        else -> stringResource(R.string.share_module_hint)
    }

/** Two states instead of a switch: the label says what happens on the next tap. */
@Composable
private fun RoleSelector(role: ShareRole, enabled: Boolean, onSelect: (ShareRole) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Corners.field))
            .background(MaterialTheme.txoka.surfaceMuted)
            .padding(Spacing.xs)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        RoleOption(
            label = stringResource(R.string.share_role_send),
            selected = role == ShareRole.Send,
            enabled = enabled,
            onClick = { onSelect(ShareRole.Send) },
            modifier = Modifier.weight(1f),
        )
        RoleOption(
            label = stringResource(R.string.share_role_receive),
            selected = role == ShareRole.Receive,
            enabled = enabled,
            onClick = { onSelect(ShareRole.Receive) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RoleOption(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(Corners.chip))
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = Spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
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
                linkedin = "isidro-garcia",
                instagram = "isidro.png",
                card = CardPalette.Default,
            ),
            role = ShareRoleState(),
            requireUnlock = false,
            nfc = NfcStatus.Enabled,
            onOpenReceived = {},
            onOpenSettings = {},
            onCreateCard = {},
            onEditProfile = {},
            onRoleSelected = {},
            onCancelReturn = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShareScreenNoCardPreview() {
    TxokaTheme {
        ShareScreen(
            profile = null,
            role = ShareRoleState(role = ShareRole.Receive),
            requireUnlock = false,
            nfc = NfcStatus.Unavailable,
            onOpenReceived = {},
            onOpenSettings = {},
            onCreateCard = {},
            onEditProfile = {},
            onRoleSelected = {},
            onCancelReturn = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 808, heightDp = 360)
@Composable
private fun ShareScreenLandscapePreview() {
    TxokaTheme {
        ShareScreen(
            profile = Profile(
                name = "Isidro García",
                phone = "+34 600 123 456",
                email = "isidro@example.com",
                card = CardPalette.Default,
            ),
            role = ShareRoleState(),
            requireUnlock = false,
            nfc = NfcStatus.Enabled,
            onOpenReceived = {},
            onOpenSettings = {},
            onCreateCard = {},
            onEditProfile = {},
            onRoleSelected = {},
            onCancelReturn = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShareScreenReturningPreview() {
    TxokaTheme {
        ShareScreen(
            profile = Profile(name = "Isidro García", phone = "+34 600 123 456", card = CardPalette.Default),
            role = ShareRoleState(role = ShareRole.Send, returning = true, remainingSeconds = 42),
            requireUnlock = false,
            nfc = NfcStatus.Enabled,
            onOpenReceived = {},
            onOpenSettings = {},
            onCreateCard = {},
            onEditProfile = {},
            onRoleSelected = {},
            onCancelReturn = {},
        )
    }
}
