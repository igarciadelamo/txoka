package es.igarciadelamo.txoka.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.PhoneCountry
import es.igarciadelamo.txoka.ui.components.CountryChip
import es.igarciadelamo.txoka.ui.components.PrimaryButton
import es.igarciadelamo.txoka.ui.components.TxokaAppBar
import es.igarciadelamo.txoka.ui.components.TxokaField
import es.igarciadelamo.txoka.ui.theme.ContentMaxWidth
import es.igarciadelamo.txoka.ui.theme.Spacing
import es.igarciadelamo.txoka.ui.theme.TxokaTheme

@Composable
fun ProfileEditorScreen(
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var colourEditing by remember { mutableStateOf(false) }
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }
    LaunchedEffect(state.deleted) { if (state.deleted) onDeleted() }

    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TxokaAppBar(
            title = stringResource(if (state.isEditing) R.string.profile_title_edit else R.string.profile_title_create),
            onBack = onCancel,
            actions = {
                ProfileEditorMenu(visible = state.isEditing, onDeleteCard = { confirmDelete = true })
            },
        )
        EditorContent(
            state = state,
            onNameChange = viewModel::onNameChange,
            onPhoneChange = viewModel::onPhoneChange,
            onEmailChange = viewModel::onEmailChange,
            onLinkedInChange = viewModel::onLinkedInChange,
            onInstagramChange = viewModel::onInstagramChange,
            onCountrySelected = viewModel::onCountrySelected,
            onCardChange = viewModel::onCardChange,
            onColourEditingChange = { colourEditing = it },
            modifier = Modifier.weight(1f),
        )
        if (state.cardTooLarge) {
            Text(
                text = stringResource(R.string.error_card_too_long),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = Spacing.xl),
            )
        } else if (!state.ndefFits) {
            Text(
                text = stringResource(R.string.qr_nfc_too_large_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.xl),
            )
        }
        BottomBar(canSave = state.canSave && !colourEditing, onSave = viewModel::save)
    }
    if (confirmDelete) {
        DeleteCardDialog(
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
        )
    }
}

@Composable
private fun ProfileEditorMenu(visible: Boolean, onDeleteCard: () -> Unit) {
    if (!visible) return
    var menuOpen by remember { mutableStateOf(false) }
    IconButton(onClick = { menuOpen = true }) {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = stringResource(R.string.action_more),
        )
    }
    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.action_delete_card),
                    color = MaterialTheme.colorScheme.error,
                )
            },
            onClick = {
                menuOpen = false
                onDeleteCard()
            },
        )
    }
}

@Composable
private fun DeleteCardDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.delete_card_dialog_title)) },
        text = { Text(text = stringResource(R.string.delete_card_dialog_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.action_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun EditorContent(
    state: ProfileEditorUiState,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onLinkedInChange: (String) -> Unit,
    onInstagramChange: (String) -> Unit,
    onCountrySelected: (PhoneCountry) -> Unit,
    onCardChange: (CardPalette) -> Unit,
    onColourEditingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCountryPicker by rememberSaveable { mutableStateOf(false) }
    val fields: @Composable () -> Unit = {
        ContactFields(
            state = state,
            onNameChange = onNameChange,
            onPhoneChange = onPhoneChange,
            onEmailChange = onEmailChange,
            onLinkedInChange = onLinkedInChange,
            onInstagramChange = onInstagramChange,
            onOpenCountryPicker = { showCountryPicker = true },
        )
    }
    AppearanceEditor(
        state = state,
        onCardChange = onCardChange,
        modifier = modifier,
        contactFields = fields,
        onColourEditingChange = onColourEditingChange,
    )
    if (showCountryPicker) {
        CountryPickerDialog(
            selected = state.country,
            onSelect = onCountrySelected,
            onDismiss = { showCountryPicker = false },
        )
    }
}

@Composable
private fun ContactFields(
    state: ProfileEditorUiState,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onLinkedInChange: (String) -> Unit,
    onInstagramChange: (String) -> Unit,
    onOpenCountryPicker: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        TxokaField(
            value = state.name,
            onValueChange = onNameChange,
            label = stringResource(R.string.field_name),
            placeholder = stringResource(R.string.profile_name_placeholder),
            isError = state.nameError,
            supportingText = if (state.nameError) stringResource(R.string.error_name_required) else null,
        )
        TxokaField(
            value = state.phone,
            onValueChange = onPhoneChange,
            label = stringResource(R.string.field_phone),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = state.phoneError,
            supportingText = if (state.phoneError) stringResource(R.string.error_phone_invalid) else null,
            leading = {
                CountryChip(
                    flag = state.country.flag,
                    dialCode = state.country.dialCode,
                    onClick = onOpenCountryPicker,
                    countryName = state.country.label,
                )
            },
        )
        TxokaField(
            value = state.email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.field_email),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = state.emailError,
            supportingText = if (state.emailError) stringResource(R.string.error_email_invalid) else null,
        )
        TxokaField(
            value = state.linkedin,
            onValueChange = onLinkedInChange,
            label = stringResource(R.string.field_linkedin),
            placeholder = stringResource(R.string.profile_social_placeholder),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            isError = state.linkedinError,
            supportingText = if (state.linkedinError) stringResource(R.string.error_social_invalid) else null,
        )
        TxokaField(
            value = state.instagram,
            onValueChange = onInstagramChange,
            label = stringResource(R.string.field_instagram),
            placeholder = stringResource(R.string.profile_social_placeholder),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            isError = state.instagramError,
            supportingText = if (state.instagramError) stringResource(R.string.error_social_invalid) else null,
        )
        if (!state.hasContact) {
            Text(
                text = stringResource(R.string.profile_contact_required),
                modifier = Modifier.padding(start = Spacing.xs),
                style = MaterialTheme.typography.bodySmall,
                color = if (state.contactError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        if (state.cardTooLarge) {
            Text(
                text = stringResource(R.string.error_card_too_long),
                modifier = Modifier.padding(start = Spacing.xs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun BottomBar(canSave: Boolean, onSave: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Spacing.xl, vertical = Spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        PrimaryButton(
            text = stringResource(R.string.action_save),
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier.widthIn(max = ContentMaxWidth),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileEditorPreview() {
    TxokaTheme {
        AppearanceEditor(
            state = ProfileEditorUiState(
                name = "Isidro García",
                phone = "+34 600 123 456",
                email = "isidro@example.com",
            ),
            onCardChange = {},
            contactFields = {},
        )
    }
}
