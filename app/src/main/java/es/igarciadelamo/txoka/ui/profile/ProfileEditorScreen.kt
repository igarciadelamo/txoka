package es.igarciadelamo.txoka.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import es.igarciadelamo.txoka.ui.components.CardLayout
import es.igarciadelamo.txoka.ui.components.ContactCard
import es.igarciadelamo.txoka.ui.components.CountryChip
import es.igarciadelamo.txoka.ui.components.Eyebrow
import es.igarciadelamo.txoka.ui.components.PrimaryButton
import es.igarciadelamo.txoka.ui.components.ScreenTopBar
import es.igarciadelamo.txoka.ui.components.ShareChip
import es.igarciadelamo.txoka.ui.components.TxokaField
import es.igarciadelamo.txoka.ui.components.toComposeColor
import es.igarciadelamo.txoka.ui.theme.Spacing
import es.igarciadelamo.txoka.ui.theme.TxokaTheme

@Composable
fun ProfileEditorScreen(
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ScreenTopBar(
            title = stringResource(
                if (state.isEditing) R.string.profile_title_edit else R.string.profile_title_create,
            ),
            onBack = onCancel,
        )
        EditorContent(
            state = state,
            onNameChange = viewModel::onNameChange,
            onPhoneChange = viewModel::onPhoneChange,
            onEmailChange = viewModel::onEmailChange,
            onCountrySelected = viewModel::onCountrySelected,
            onSharePhoneChange = viewModel::onSharePhoneChange,
            onShareEmailChange = viewModel::onShareEmailChange,
            onPresetSelected = viewModel::onPresetSelected,
            onCustomHexChange = viewModel::onCustomHexChange,
            modifier = Modifier.weight(1f),
        )
        BottomBar(
            canSave = state.canSave,
            onSave = viewModel::save,
            onDiscard = onCancel,
        )
    }
}

@Composable
private fun EditorContent(
    state: ProfileEditorUiState,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onCountrySelected: (PhoneCountry) -> Unit,
    onSharePhoneChange: (Boolean) -> Unit,
    onShareEmailChange: (Boolean) -> Unit,
    onPresetSelected: (CardPalette) -> Unit,
    onCustomHexChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCountryPicker by rememberSaveable { mutableStateOf(false) }
    var showCustomColour by rememberSaveable { mutableStateOf(state.card !in CardPalette.Presets) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        PreviewSection(state = state)
        IdentitySection(state = state, onNameChange = onNameChange)
        ContactSection(
            state = state,
            onPhoneChange = onPhoneChange,
            onEmailChange = onEmailChange,
            onSharePhoneChange = onSharePhoneChange,
            onShareEmailChange = onShareEmailChange,
            onOpenCountryPicker = { showCountryPicker = true },
        )
        StyleSection(
            state = state,
            customSelected = showCustomColour,
            onPresetSelected = { palette ->
                showCustomColour = false
                onPresetSelected(palette)
            },
            onCustomClick = { showCustomColour = true },
            onCustomHexChange = onCustomHexChange,
        )
    }

    if (showCountryPicker) {
        CountryPickerDialog(
            selected = state.country,
            onSelect = onCountrySelected,
            onDismiss = { showCountryPicker = false },
        )
    }
}

@Composable
private fun PreviewSection(state: ProfileEditorUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        ContactCard(
            profile = state.toPreview(),
            placeholder = !state.isEditing,
            layout = CardLayout.Preview,
        )
        if (!state.isEditing) {
            Text(
                text = stringResource(R.string.profile_card_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun IdentitySection(state: ProfileEditorUiState, onNameChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Eyebrow(text = stringResource(R.string.section_identity))
        TxokaField(
            value = state.name,
            onValueChange = onNameChange,
            label = stringResource(R.string.field_name),
            placeholder = stringResource(R.string.profile_name_placeholder),
            isError = state.nameError,
            supportingText = if (state.nameError) stringResource(R.string.error_name_required) else null,
        )
    }
}

@Composable
private fun ContactSection(
    state: ProfileEditorUiState,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSharePhoneChange: (Boolean) -> Unit,
    onShareEmailChange: (Boolean) -> Unit,
    onOpenCountryPicker: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Eyebrow(text = stringResource(R.string.section_contact))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            CountryChip(
                flag = state.country.flag,
                dialCode = state.country.dialCode,
                onClick = onOpenCountryPicker,
            )
            TxokaField(
                value = state.phone,
                onValueChange = onPhoneChange,
                label = stringResource(R.string.field_phone),
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = state.phoneError,
                supportingText = if (state.phoneError) stringResource(R.string.error_phone_invalid) else null,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            ShareChip(
                text = stringResource(R.string.field_phone),
                selected = state.sharePhone,
                enabled = state.phone.isNotBlank(),
                onClick = { onSharePhoneChange(!state.sharePhone) },
            )
            ShareChip(
                text = stringResource(R.string.field_email),
                selected = state.shareEmail,
                enabled = state.email.isNotBlank(),
                onClick = { onShareEmailChange(!state.shareEmail) },
            )
        }
        TxokaField(
            value = state.email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.field_email),
            placeholder = stringResource(R.string.field_optional),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = state.emailError,
            supportingText = if (state.emailError) stringResource(R.string.error_email_invalid) else null,
        )
    }
}

@Composable
private fun StyleSection(
    state: ProfileEditorUiState,
    customSelected: Boolean,
    onPresetSelected: (CardPalette) -> Unit,
    onCustomClick: () -> Unit,
    onCustomHexChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Eyebrow(text = stringResource(R.string.section_style))
        PaletteRow(
            selected = state.card,
            customSelected = customSelected,
            onSelect = onPresetSelected,
            onCustomClick = onCustomClick,
        )
        if (customSelected) {
            TxokaField(
                value = state.customHex,
                onValueChange = onCustomHexChange,
                label = stringResource(R.string.card_colour_custom),
                placeholder = stringResource(R.string.card_colour_custom_hint),
                isError = state.customHexError,
                supportingText = if (state.customHexError) {
                    stringResource(R.string.card_colour_custom_error)
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun PaletteRow(
    selected: CardPalette,
    customSelected: Boolean,
    onSelect: (CardPalette) -> Unit,
    onCustomClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        CardPalette.Presets.forEachIndexed { index, palette ->
            val isSelected = !customSelected && palette == selected
            val presetLabel = stringResource(R.string.card_colour_preset, index + 1)
            Box(
                modifier = Modifier
                    .size(46.dp)
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
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        shape = CircleShape,
                    )
                    .padding(if (isSelected) 3.dp else 0.dp)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(palette) },
                    )
                    .semantics { contentDescription = presetLabel },
            )
        }
        val customLabel = stringResource(R.string.card_colour_custom)
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = if (customSelected) 2.dp else 0.dp,
                    color = MaterialTheme.colorScheme.onBackground,
                    shape = CircleShape,
                )
                .selectable(
                    selected = customSelected,
                    role = Role.RadioButton,
                    onClick = onCustomClick,
                )
                .semantics { contentDescription = customLabel },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BottomBar(canSave: Boolean, onSave: () -> Unit, onDiscard: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Spacing.xl, vertical = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PrimaryButton(
            text = stringResource(R.string.action_save),
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onDiscard) {
            Text(
                text = stringResource(R.string.action_discard),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileEditorPreview() {
    TxokaTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            ContactCard(
                profile = ProfileEditorUiState(
                    name = "Isidro García",
                    phone = "+34 600 123 456",
                    email = "isidro@example.com",
                ).toPreview(),
                layout = CardLayout.Preview,
            )
            PaletteRow(selected = CardPalette.Default, customSelected = false, onSelect = {}, onCustomClick = {})
            BottomBar(canSave = true, onSave = {}, onDiscard = {})
        }
    }
}
