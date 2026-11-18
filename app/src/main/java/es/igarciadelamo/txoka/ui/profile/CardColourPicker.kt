package es.igarciadelamo.txoka.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.profile.RgbColor
import es.igarciadelamo.txoka.ui.components.PrimaryButton
import es.igarciadelamo.txoka.ui.components.toComposeColor

private val ColourPresets = listOf(
    "#FFFFFF", "#E2E8F0", "#9BA5B5", "#475569", "#1E293B", "#0F172A",
    "#111827", "#000000", "#FCA5A5", "#EF4444", "#B91C1C", "#7F1D1D",
    "#FDBA74", "#F97316", "#FED7AA", "#FDE047", "#EAB308", "#A16207",
    "#BEF264", "#84CC16", "#166534", "#2DD4BF", "#0D9488", "#0E7490",
    "#7DD3FC", "#38BDF8", "#2563EB", "#1E40AF", "#4F46E5", "#818CF8",
    "#C4B5FD", "#8B5CF6", "#581C87", "#F0ABFC", "#EC4899", "#9D174D",
).map { requireNotNull(RgbColor.parse(it)) }

@Composable
internal fun CardColourPicker(
    initial: RgbColor,
    title: String,
    onPreview: (RgbColor) -> Unit,
    onSelect: (RgbColor) -> Unit,
    onCancel: () -> Unit,
) {
    var draft by remember(initial) { mutableStateOf(ColourDraft.from(initial)) }
    var custom by remember(initial) { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(28.dp).background(draft.color.toComposeColor(), CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
        )
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f).padding(start = 8.dp))
        TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
    }
    if (!custom) {
        PresetColourGrid(selected = initial, onSelect = onSelect)
        TextButton(onClick = { custom = true }) { Text(stringResource(R.string.card_colour_custom)) }
    } else {
        CustomColourControls(
            draft = draft,
            onChange = {
                draft = it
                if (it.validHex) onPreview(it.color)
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { custom = false }) { Text(stringResource(R.string.appearance_presets)) }
            PrimaryButton(
                text = stringResource(R.string.card_colour_apply),
                enabled = draft.validHex,
                onClick = { onSelect(draft.color) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PresetColourGrid(selected: RgbColor, onSelect: (RgbColor) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = (maxWidth / 48.dp).toInt().coerceIn(1, 6)
        Column {
            ColourPresets.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { color ->
                        val description = stringResource(R.string.appearance_colour_value, color.hex)
                        Box(
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                .selectable(color == selected, role = Role.RadioButton, onClick = { onSelect(color) })
                                .semantics { contentDescription = description },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier.size(34.dp).background(color.toComposeColor(), CircleShape)
                                    .border(
                                        if (color == selected) 3.dp else 1.dp,
                                        if (color == selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        },
                                        CircleShape,
                                    ),
                            )
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun CustomColourControls(draft: ColourDraft, onChange: (ColourDraft) -> Unit) {
    val hsv = draft.hsv
    ColourSlider(
        title = stringResource(R.string.appearance_hue),
        value = hsv.hue / 360f,
        colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red),
        onChange = { onChange(draft.withHsv(hsv.copy(hue = it * 360f))) },
    )
    ColourSlider(
        title = stringResource(R.string.appearance_saturation),
        value = hsv.saturation,
        colors = listOf(Color.White, hsv.copy(saturation = 1f, value = 1f).toRgb().toComposeColor()),
        onChange = { onChange(draft.withHsv(hsv.copy(saturation = it))) },
    )
    ColourSlider(
        title = stringResource(R.string.appearance_brightness),
        value = hsv.value,
        colors = listOf(Color.Black, hsv.copy(value = 1f).toRgb().toComposeColor()),
        onChange = { onChange(draft.withHsv(hsv.copy(value = it))) },
    )
    OutlinedTextField(
        value = draft.hex,
        onValueChange = { onChange(draft.withHex(it)) },
        label = { Text(stringResource(R.string.appearance_hex)) },
        singleLine = true,
        isError = !draft.validHex,
        supportingText = if (!draft.validHex) {
            { Text(stringResource(R.string.appearance_hex_error)) }
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ColourSlider(title: String, value: Float, colors: List<Color>, onChange: (Float) -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.labelMedium)
        Slider(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(colors), CircleShape)
                .semantics { contentDescription = title },
        )
    }
}
