package es.igarciadelamo.txoka.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import es.igarciadelamo.txoka.ui.components.TxokaAppBar
import es.igarciadelamo.txoka.ui.components.TxokaField
import es.igarciadelamo.txoka.ui.components.toComposeColor
import es.igarciadelamo.txoka.ui.theme.Spacing
import es.igarciadelamo.txoka.ui.theme.TxokaTheme

/** Swatches must stay at least 48 dp so they are easy to hit. */
private val PaletteSwatchSize = 48.dp
private const val PALETTE_COLUMNS = 4

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
    var showColourSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onDeleted()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TxokaAppBar(
            title = stringResource(
                if (state.isEditing) R.string.profile_title_edit else R.string.profile_title_create,
            ),
            onBack = onCancel,
            actions = {
                ProfileEditorMenu(
                    visible = state.isEditing,
                    onDeleteCard = { confirmDelete = true },
                )
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
            onPresetSelected = viewModel::onPresetSelected,
            onCustomClick = { showColourSheet = true },
            modifier = Modifier.weight(1f),
        )
        BottomBar(
            canSave = state.canSave,
            onSave = viewModel::save,
        )
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

    if (showColourSheet) {
        ColourPickerSheet(
            profile = state.toPreview(),
            initial = state.card,
            onDismiss = { showColourSheet = false },
            onApply = viewModel::onCustomColourSelected,
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
    onPresetSelected: (CardPalette) -> Unit,
    onCustomClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCountryPicker by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        PreviewSection(state = state)
        ContactFields(
            state = state,
            onNameChange = onNameChange,
            onPhoneChange = onPhoneChange,
            onEmailChange = onEmailChange,
            onLinkedInChange = onLinkedInChange,
            onInstagramChange = onInstagramChange,
            onOpenCountryPicker = { showCountryPicker = true },
        )
        StyleSection(
            selected = state.card,
            onPresetSelected = onPresetSelected,
            onCustomClick = onCustomClick,
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
    }
}

@Composable
private fun StyleSection(selected: CardPalette, onPresetSelected: (CardPalette) -> Unit, onCustomClick: () -> Unit) {
    val customSelected = selected !in CardPalette.Presets
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Eyebrow(text = stringResource(R.string.section_style))
        PaletteGrid(
            selected = selected,
            customSelected = customSelected,
            onSelect = onPresetSelected,
            onCustomClick = onCustomClick,
        )
    }
}

@Composable
private fun PaletteGrid(
    selected: CardPalette,
    customSelected: Boolean,
    onSelect: (CardPalette) -> Unit,
    onCustomClick: () -> Unit,
) {
    val cellCount = CardPalette.Presets.size + 1
    val rows = (cellCount + PALETTE_COLUMNS - 1) / PALETTE_COLUMNS
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        repeat(rows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                repeat(PALETTE_COLUMNS) { column ->
                    val index = row * PALETTE_COLUMNS + column
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        when {
                            index < CardPalette.Presets.size -> {
                                val palette = CardPalette.Presets[index]
                                PresetSwatch(
                                    palette = palette,
                                    selected = !customSelected && palette == selected,
                                    label = stringResource(R.string.card_colour_preset, index + 1),
                                    onClick = { onSelect(palette) },
                                )
                            }
                            index == CardPalette.Presets.size -> CustomSwatch(
                                palette = if (customSelected) selected else null,
                                selected = customSelected,
                                label = stringResource(R.string.card_colour_custom),
                                onClick = onCustomClick,
                            )
                            else -> Spacer(modifier = Modifier.size(PaletteSwatchSize))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetSwatch(palette: CardPalette, selected: Boolean, label: String, onClick: () -> Unit) {
    val ring = if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onBackground, CircleShape) else Modifier
    Box(
        modifier = Modifier
            .size(PaletteSwatchSize)
            .clip(CircleShape)
            .background(brush = palette.brush())
            .then(ring)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label },
    )
}

@Composable
private fun CustomSwatch(palette: CardPalette?, selected: Boolean, label: String, onClick: () -> Unit) {
    val ring = if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onBackground, CircleShape) else Modifier
    Box(
        modifier = Modifier
            .size(PaletteSwatchSize)
            .clip(CircleShape)
            .then(
                if (palette != null) {
                    Modifier.background(brush = palette.brush())
                } else {
                    Modifier.background(MaterialTheme.colorScheme.surface)
                },
            )
            .then(ring)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        if (palette == null) {
            Text(
                text = "+",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun CardPalette.brush(): Brush = Brush.linearGradient(
    colors = listOf(accent.toComposeColor(), accent2.toComposeColor()),
)

@Composable
private fun BottomBar(canSave: Boolean, onSave: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Spacing.xl, vertical = Spacing.md),
    ) {
        PrimaryButton(
            text = stringResource(R.string.action_save),
            onClick = onSave,
            enabled = canSave,
        )
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
                    linkedin = "isidro-garcia",
                    instagram = "isidro.png",
                ).toPreview(),
                layout = CardLayout.Preview,
            )
            PaletteGrid(selected = CardPalette.Default, customSelected = false, onSelect = {}, onCustomClick = {})
            BottomBar(canSave = true, onSave = {})
        }
    }
}
