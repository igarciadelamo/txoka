package es.igarciadelamo.txoka.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.profile.CardAppearance
import es.igarciadelamo.txoka.domain.profile.CardBackground
import es.igarciadelamo.txoka.domain.profile.CardEffect
import es.igarciadelamo.txoka.domain.profile.CardMosaic
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.domain.profile.RgbColor
import es.igarciadelamo.txoka.domain.profile.TextFinish
import es.igarciadelamo.txoka.domain.profile.TextSize
import es.igarciadelamo.txoka.ui.components.CardMotion
import es.igarciadelamo.txoka.ui.components.CardMotionController
import es.igarciadelamo.txoka.ui.components.CardSurface
import es.igarciadelamo.txoka.ui.components.CardViewer
import es.igarciadelamo.txoka.ui.components.rememberCardMotionController
import es.igarciadelamo.txoka.ui.components.toComposeColor

private enum class AppearanceLayer(val label: Int) {
    BACKGROUND(R.string.appearance_background),
    MOSAIC(R.string.appearance_mosaic),
    EFFECT(R.string.appearance_effect),
    DATA(R.string.appearance_data),
}

internal enum class CardColourTarget(val label: Int) {
    FIRST(R.string.appearance_first_colour),
    SECOND(R.string.appearance_second_colour),
    TOP_RIGHT(R.string.appearance_top_right),
    BOTTOM_LEFT(R.string.appearance_bottom_left),
    NAME(R.string.field_name),
    DATA(R.string.appearance_contact_text),
}

private data class ColourSession(val target: CardColourTarget, val opening: CardPalette)

@Composable
internal fun AppearanceEditor(
    state: ProfileEditorUiState,
    onCardChange: (CardPalette) -> Unit,
    modifier: Modifier = Modifier,
    contactFields: @Composable () -> Unit,
    onColourEditingChange: (Boolean) -> Unit = {},
) {
    val controller = rememberCardMotionController()
    var paused by rememberSaveable { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var layer by rememberSaveable { mutableStateOf(AppearanceLayer.BACKGROUND) }
    var colourSession by remember { mutableStateOf<ColourSession?>(null) }
    var previewColour by remember { mutableStateOf<RgbColor?>(null) }
    LaunchedEffect(colourSession != null) { onColourEditingChange(colourSession != null) }
    BackHandler(enabled = colourSession != null) {
        colourSession?.let { onCardChange(it.opening) }
        colourSession = null
        previewColour = null
    }
    val previewCard = colourSession?.let { session ->
        previewColour?.let { state.card.withColour(session.target, it) }
    } ?: state.card
    val preview = state.toPreview().copy(card = previewCard).let {
        if (it.name.isBlank()) it.copy(name = stringResource(R.string.profile_name_placeholder)) else it
    }
    val previewContent: @Composable (Modifier, Dp) -> Unit = { previewModifier, maxCardHeight ->
        AppearancePreviewPane(
            profile = preview,
            controller = controller,
            paused = paused,
            suspended = expanded,
            onPausedChange = { paused = it },
            onExpand = { expanded = true },
            maxCardHeight = maxCardHeight,
            modifier = previewModifier,
        )
    }
    AppearanceAdaptiveLayout(
        modifier = modifier,
        preview = previewContent,
        settings = { settingsModifier ->
            AppearanceSettingsPane(
                card = state.card,
                layer = layer,
                session = colourSession,
                onLayerSelect = {
                    colourSession?.let { session -> onCardChange(session.opening) }
                    colourSession = null
                    previewColour = null
                    layer = it
                },
                onSessionChange = {
                    colourSession = it
                    previewColour = null
                },
                onColourPreview = { previewColour = it },
                onCardChange = onCardChange,
                contactFields = contactFields,
                modifier = settingsModifier,
            )
        },
    )
    if (expanded) CardViewer(profile = preview, onDismiss = { expanded = false })
}

@Composable
private fun AppearancePreviewPane(
    profile: Profile,
    controller: CardMotionController,
    paused: Boolean,
    suspended: Boolean,
    onPausedChange: (Boolean) -> Unit,
    onExpand: () -> Unit,
    maxCardHeight: Dp,
    modifier: Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ScaledCardPreview(
            profile,
            if (suspended) CardMotion() else controller.motion,
            paused || suspended,
            maxCardHeight,
        )
        AppearanceMotionControls(controller, paused, onPausedChange, onExpand)
    }
}

@Composable
private fun AppearanceAdaptiveLayout(
    modifier: Modifier,
    preview: @Composable (Modifier, Dp) -> Unit,
    settings: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        val availableWidth = maxWidth
        val availableHeight = maxHeight
        if (availableWidth > availableHeight) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                    preview(Modifier.fillMaxWidth(), (availableHeight - 112.dp).coerceAtLeast(32.dp))
                }
                settings(Modifier.weight(1f).fillMaxSize())
            }
        } else {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    preview(Modifier.fillMaxWidth(), (availableHeight * .5f - 88.dp).coerceAtLeast(48.dp))
                }
                settings(Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun AppearanceSettingsPane(
    card: CardPalette,
    layer: AppearanceLayer,
    session: ColourSession?,
    onLayerSelect: (AppearanceLayer) -> Unit,
    onSessionChange: (ColourSession?) -> Unit,
    onColourPreview: (RgbColor) -> Unit,
    onCardChange: (CardPalette) -> Unit,
    modifier: Modifier,
    contactFields: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        LayerTabs(layer, onSelect = onLayerSelect)
        val layerScroll = key(layer) { rememberScrollState() }
        val scroll = if (session == null) layerScroll else key(session.target) { rememberScrollState() }
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scroll).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (session != null) {
                CardColourPicker(
                    initial = session.opening.colourAt(session.target),
                    title = stringResource(session.target.labelFor(session.opening.editableAppearance())),
                    onPreview = onColourPreview,
                    onSelect = {
                        onCardChange(card.withColour(session.target, it))
                        onSessionChange(null)
                    },
                    onCancel = {
                        onCardChange(session.opening)
                        onSessionChange(null)
                    },
                )
            } else {
                LayerSettings(
                    layer = layer,
                    card = card,
                    onChange = onCardChange,
                    onColour = { onSessionChange(ColourSession(it, card)) },
                    contactFields = contactFields,
                )
            }
        }
    }
}

@Composable
private fun LayerTabs(selected: AppearanceLayer, onSelect: (AppearanceLayer) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        AppearanceLayer.entries.forEach { layer ->
            val isSelected = layer == selected
            val colors = MaterialTheme.colorScheme
            val container = if (isSelected) colors.primaryContainer else colors.surfaceVariant
            Box(
                modifier = Modifier.weight(1f).height(48.dp)
                    .background(container)
                    .selectable(isSelected, role = Role.Tab, onClick = { onSelect(layer) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(layer.label),
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColorFor(container),
                )
            }
        }
    }
}

@Composable
private fun LayerSettings(
    layer: AppearanceLayer,
    card: CardPalette,
    onChange: (CardPalette) -> Unit,
    onColour: (CardColourTarget) -> Unit,
    contactFields: @Composable () -> Unit,
) {
    val appearance = card.editableAppearance()
    when (layer) {
        AppearanceLayer.BACKGROUND -> BackgroundSettings(card, appearance, onChange, onColour)
        AppearanceLayer.MOSAIC -> MosaicSettings(card, appearance, onChange)
        AppearanceLayer.EFFECT -> EffectSettings(card, appearance, onChange)
        AppearanceLayer.DATA -> TextSettings(card, appearance, onChange, onColour, contactFields)
    }
}

@Composable
private fun BackgroundSettings(
    card: CardPalette,
    appearance: CardAppearance,
    onChange: (CardPalette) -> Unit,
    onColour: (CardColourTarget) -> Unit,
) {
    BackgroundPalette(card, appearance, onChange, onColour)
    TileGrid(CardBackground.entries.toList()) { background, modifier ->
        AppearanceTile(
            selected = appearance.background == background,
            label = stringResource(background.label),
            modifier = modifier,
            onClick = {
                val next = appearance.copy(
                    background = background,
                    topRight = appearance.topRight.takeUnless { background == CardBackground.FLAT },
                    bottomLeft = appearance.bottomLeft.takeUnless { background == CardBackground.FLAT },
                )
                onChange(card.copy(appearance = next))
            },
        ) {
            CardSurface(
                palette = card.copy(
                    appearance = appearance.copy(
                        background = background,
                        mosaic = CardMosaic.NONE,
                        effect = CardEffect.NONE,
                        topRight = appearance.topRight.takeUnless { background == CardBackground.FLAT },
                        bottomLeft = appearance.bottomLeft.takeUnless { background == CardBackground.FLAT },
                    ),
                ),
                motion = CardMotion(.12f, -.08f),
                paused = true,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun EffectSettings(card: CardPalette, appearance: CardAppearance, onChange: (CardPalette) -> Unit) {
    TileGrid(CardEffect.entries.toList()) { effect, modifier ->
        AppearanceTile(
            selected = effect == appearance.effect,
            label = stringResource(effect.label),
            modifier = modifier,
            onClick = { onChange(card.copy(appearance = appearance.copy(effect = effect))) },
        ) {
            if (effect == CardEffect.NONE) {
                Text(stringResource(R.string.appearance_none_symbol), style = MaterialTheme.typography.headlineLarge)
            } else {
                EffectPreview(effect = effect, card = card)
            }
        }
    }
}

@Composable
private fun MosaicSettings(card: CardPalette, appearance: CardAppearance, onChange: (CardPalette) -> Unit) {
    var category by rememberSaveable { mutableStateOf(MosaicCategory.ALL) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        MosaicCategory.entries.forEach { item ->
            FilterChip(
                selected = category == item,
                onClick = { category = item },
                label = { Text(stringResource(item.label)) },
            )
        }
    }
    val visible = CardMosaic.entries.filter { it == CardMosaic.NONE || category.includes(it) }
    TileGrid(visible) { mosaic, modifier ->
        AppearanceTile(
            selected = mosaic == appearance.mosaic,
            label = stringResource(mosaic.label),
            modifier = modifier,
            onClick = { onChange(card.copy(appearance = appearance.copy(mosaic = mosaic))) },
        ) {
            if (mosaic == CardMosaic.NONE) {
                Text(stringResource(R.string.appearance_none_symbol), style = MaterialTheme.typography.headlineLarge)
            } else {
                CardSurface(
                    palette = card.copy(appearance = appearance.copy(mosaic = mosaic, effect = CardEffect.NONE)),
                    motion = CardMotion(.12f, -.08f),
                    paused = true,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun TextSettings(
    card: CardPalette,
    appearance: CardAppearance,
    onChange: (CardPalette) -> Unit,
    onColour: (CardColourTarget) -> Unit,
    contactFields: @Composable () -> Unit,
) {
    var editingContent by rememberSaveable { mutableStateOf(false) }
    var isName by rememberSaveable { mutableStateOf(true) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(editingContent, onClick = { editingContent = true }, label = {
            Text(stringResource(R.string.appearance_content))
        })
        FilterChip(!editingContent, onClick = { editingContent = false }, label = {
            Text(stringResource(R.string.appearance_lettering))
        })
    }
    if (editingContent) {
        contactFields()
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(isName, onClick = { isName = true }, label = { Text(stringResource(R.string.field_name)) })
        FilterChip(!isName, onClick = { isName = false }, label = {
            Text(stringResource(R.string.appearance_contact_text))
        })
    }
    val text = if (isName) appearance.name else appearance.data
    TileGrid(TextFinish.entries.toList()) { finish, modifier ->
        AppearanceTile(
            selected = text.finish == finish,
            label = stringResource(finish.label),
            modifier = modifier,
            onClick = {
                val next = text.copy(finish = finish)
                onChange(
                    card.copy(appearance = if (isName) appearance.copy(name = next) else appearance.copy(data = next)),
                )
            },
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CardSurface(
                    palette = card.copy(
                        appearance = appearance.copy(mosaic = CardMosaic.NONE, effect = CardEffect.NONE),
                    ),
                    motion = CardMotion(.12f, -.08f),
                    paused = true,
                    modifier = Modifier.fillMaxSize(),
                )
                LetteringPreview(finish, text.color)
            }
        }
    }
    TextSizeChoices(selected = text.size, onSelect = { size ->
        val next = text.copy(size = size)
        onChange(card.copy(appearance = if (isName) appearance.copy(name = next) else appearance.copy(data = next)))
    })
    ColourButton(text.effectiveColor, stringResource(R.string.appearance_text_colour), Modifier.fillMaxWidth()) {
        onColour(if (isName) CardColourTarget.NAME else CardColourTarget.DATA)
    }
    TextButton(onClick = {
        val next = text.copy(color = null)
        onChange(card.copy(appearance = if (isName) appearance.copy(name = next) else appearance.copy(data = next)))
    }) { Text(stringResource(R.string.appearance_default_text_colour)) }
}

@Composable
private fun TextSizeChoices(selected: TextSize, onSelect: (TextSize) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TextSize.entries.forEachIndexed { index, size ->
            FilterChip(
                selected = selected == size,
                onClick = { onSelect(size) },
                label = {
                    Text(
                        stringResource(size.label),
                        fontWeight = FontWeight.SemiBold,
                        style = when (index) {
                            0 -> MaterialTheme.typography.labelSmall
                            1 -> MaterialTheme.typography.labelMedium
                            2 -> MaterialTheme.typography.labelLarge
                            else -> MaterialTheme.typography.titleMedium
                        },
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun <T> TileGrid(items: List<T>, content: @Composable (T, Modifier) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { content(it, Modifier.weight(1f)) }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
internal fun AppearanceTile(
    selected: Boolean,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    preview: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) colors.primaryContainer else colors.surface,
        border = BorderStroke(1.dp, if (selected) colors.primary else colors.outlineVariant),
    ) {
        Column(
            modifier = Modifier.selectable(selected, role = Role.RadioButton, onClick = onClick).padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(1.586f).clip(RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) { preview() }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
            )
        }
    }
}

@Composable
internal fun ColourButton(color: RgbColor, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(color.toComposeColor())
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
        )
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
    }
}

internal fun CardPalette.colourAt(target: CardColourTarget): RgbColor {
    val style = editableAppearance()
    return when (target) {
        CardColourTarget.FIRST -> accent
        CardColourTarget.SECOND -> accent2
        CardColourTarget.TOP_RIGHT -> style.topRight ?: accent2
        CardColourTarget.BOTTOM_LEFT -> style.bottomLeft ?: accent
        CardColourTarget.NAME -> style.name.effectiveColor
        CardColourTarget.DATA -> style.data.effectiveColor
    }
}

internal fun CardPalette.withColour(target: CardColourTarget, color: RgbColor): CardPalette {
    val style = editableAppearance()
    return when (target) {
        CardColourTarget.FIRST -> copy(accent = color, appearance = style)
        CardColourTarget.SECOND -> copy(accent2 = color, appearance = style)
        CardColourTarget.TOP_RIGHT -> copy(appearance = style.copy(topRight = color))
        CardColourTarget.BOTTOM_LEFT -> copy(appearance = style.copy(bottomLeft = color))
        CardColourTarget.NAME -> copy(appearance = style.copy(name = style.name.copy(color = color)))
        CardColourTarget.DATA -> copy(appearance = style.copy(data = style.data.copy(color = color)))
    }
}
