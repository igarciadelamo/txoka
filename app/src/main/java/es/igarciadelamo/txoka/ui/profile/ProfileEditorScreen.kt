package es.igarciadelamo.txoka.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.PhoneCountry
import es.igarciadelamo.txoka.ui.components.ContactCard
import es.igarciadelamo.txoka.ui.components.toComposeColor
import es.igarciadelamo.txoka.ui.theme.TxokaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorScreen(
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (state.isEditing) R.string.profile_title_edit else R.string.profile_title_create,
                        ),
                    )
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ContactCard(profile = state.toPreview(), placeholder = !state.isEditing)
            if (!state.isEditing) {
                Text(
                    text = stringResource(R.string.profile_card_footer),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            CardSection(
                state = state,
                onPresetSelected = viewModel::onPresetSelected,
                onCustomHexChange = viewModel::onCustomHexChange,
            )
            IdentityFields(
                state = state,
                onNameChange = viewModel::onNameChange,
                onPhoneChange = viewModel::onPhoneChange,
                onEmailChange = viewModel::onEmailChange,
                onCountrySelected = viewModel::onCountrySelected,
                onSharePhoneChange = viewModel::onSharePhoneChange,
                onShareEmailChange = viewModel::onShareEmailChange,
            )
            Button(
                onClick = viewModel::save,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.action_save))
            }
        }
    }
}

@Composable
private fun CardSection(
    state: ProfileEditorUiState,
    onPresetSelected: (CardPalette) -> Unit,
    onCustomHexChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.card_colour),
            style = MaterialTheme.typography.titleSmall,
        )
        PalettePicker(selected = state.card, onSelect = onPresetSelected)
        OutlinedTextField(
            value = state.customHex,
            onValueChange = onCustomHexChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.card_colour_custom)) },
            placeholder = { Text(text = stringResource(R.string.card_colour_custom_hint)) },
            singleLine = true,
            isError = state.customHexError,
            supportingText = if (state.customHexError) {
                { Text(text = stringResource(R.string.card_colour_custom_error)) }
            } else {
                null
            },
        )
    }
}

@Composable
private fun IdentityFields(
    state: ProfileEditorUiState,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onCountrySelected: (PhoneCountry) -> Unit,
    onSharePhoneChange: (Boolean) -> Unit,
    onShareEmailChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.field_name)) },
            singleLine = true,
            isError = state.nameError,
            supportingText = if (state.nameError) {
                { Text(text = stringResource(R.string.error_name_required)) }
            } else {
                null
            },
        )
        PhoneField(
            value = state.phone,
            onValueChange = onPhoneChange,
            country = state.country,
            onCountrySelected = onCountrySelected,
            shared = state.sharePhone,
            onSharedChange = onSharePhoneChange,
            isError = state.phoneError,
        )
        ShareableField(
            value = state.email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.field_email),
            shareLabel = stringResource(R.string.share_email),
            shared = state.shareEmail,
            onSharedChange = onShareEmailChange,
            keyboardType = KeyboardType.Email,
            isError = state.emailError,
            supportingText = if (state.emailError) stringResource(R.string.error_email_invalid) else null,
        )
    }
}

@Composable
private fun PhoneField(
    value: String,
    onValueChange: (String) -> Unit,
    country: PhoneCountry,
    onCountrySelected: (PhoneCountry) -> Unit,
    shared: Boolean,
    onSharedChange: (Boolean) -> Unit,
    isError: Boolean,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = { showPicker = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            ) {
                Text(text = "${country.flag} +${country.dialCode}")
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = stringResource(R.string.phone_country),
                )
            }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                label = { Text(text = stringResource(R.string.field_phone)) },
                singleLine = true,
                isError = isError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                supportingText = if (isError) {
                    { Text(text = stringResource(R.string.error_phone_invalid)) }
                } else {
                    null
                },
            )
        }
        ShareRow(
            label = stringResource(R.string.share_phone),
            shared = shared,
            enabled = value.isNotBlank(),
            onSharedChange = onSharedChange,
        )
    }

    if (showPicker) {
        CountryPickerDialog(
            selected = country,
            onSelect = onCountrySelected,
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun PalettePicker(selected: CardPalette, onSelect: (CardPalette) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CardPalette.Presets.forEachIndexed { index, palette ->
            val isSelected = palette == selected
            val presetLabel = stringResource(R.string.card_colour_preset, index + 1)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                palette.accent.toComposeColor(),
                                palette.accent2.toComposeColor(),
                            ),
                        ),
                    )
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = CircleShape,
                    )
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(palette) },
                    )
                    .semantics { contentDescription = presetLabel },
            )
        }
    }
}

@Composable
private fun ShareableField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    shareLabel: String,
    shared: Boolean,
    onSharedChange: (Boolean) -> Unit,
    keyboardType: KeyboardType,
    isError: Boolean,
    supportingText: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = label) },
            singleLine = true,
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            supportingText = supportingText?.let { text -> { Text(text = text) } },
        )
        ShareRow(
            label = shareLabel,
            shared = shared,
            enabled = value.isNotBlank(),
            onSharedChange = onSharedChange,
        )
    }
}

/** Label and switch behave as one control, so the whole row can be toggled by touch and TalkBack. */
@Composable
private fun ShareRow(label: String, shared: Boolean, enabled: Boolean, onSharedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = shared,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onSharedChange,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
        )
        Switch(checked = shared, onCheckedChange = null, enabled = enabled)
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileEditorScreenPreview() {
    TxokaTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ContactCard(
                profile = ProfileEditorUiState(
                    name = "Isidro García",
                    phone = "+34600123456",
                    email = "isidro@example.com",
                ).toPreview(),
            )
            PalettePicker(selected = CardPalette.Default, onSelect = {})
        }
    }
}
