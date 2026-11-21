package es.igarciadelamo.txoka.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.library.LibraryCategory
import es.igarciadelamo.txoka.domain.library.LibraryCategoryIcon
import es.igarciadelamo.txoka.domain.library.LibraryOrganization
import es.igarciadelamo.txoka.domain.library.MAX_CATEGORY_NAME_LENGTH
import es.igarciadelamo.txoka.domain.library.UNCATEGORIZED_CATEGORY_ID
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.RgbColor
import es.igarciadelamo.txoka.domain.received.ReceivedContact
import es.igarciadelamo.txoka.ui.components.PHYSICAL_CARD_RATIO
import es.igarciadelamo.txoka.ui.components.TxokaAppBar
import es.igarciadelamo.txoka.ui.components.contentWidth
import es.igarciadelamo.txoka.ui.components.toComposeColor
import es.igarciadelamo.txoka.ui.profile.CardColourPicker
import es.igarciadelamo.txoka.ui.received.ReceivedListUiState
import es.igarciadelamo.txoka.ui.theme.TxokaTheme
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun LibraryOrganizer(
    state: ReceivedListUiState,
    onSaveCategory: (LibraryCategory) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onMoveCard: (Long, String?) -> Unit,
    onReorderCards: (String?, List<Long>) -> Unit,
    onFavorite: (Long, Boolean) -> Unit,
    onDismiss: () -> Unit,
    snackbarHostState: SnackbarHostState? = null,
    onReorderCategories: (List<String>) -> Unit = {},
) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var newCategory by rememberSaveable { mutableStateOf(false) }
    val category = state.organization.categories.firstOrNull { it.id == selectedId }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TxokaAppBar(title = stringResource(R.string.library_organize), onBack = onDismiss)
                    if (editing) {
                        CategoryEditor(
                            initial = if (newCategory) null else category,
                            onSave = {
                                onSaveCategory(it)
                                selectedId = it.id
                                editing = false
                            },
                            onDelete = {
                                category?.let { onDeleteCategory(it.id) }
                                selectedId = null
                                editing = false
                            },
                            onCancel = { editing = false },
                            modifier = Modifier.weight(1f).contentWidth(),
                        )
                    } else {
                        OrganizerCategories(
                            categories = state.organization.categories,
                            selectedId = selectedId,
                            onSelect = { selectedId = it },
                            onCreate = {
                                newCategory = true
                                editing = true
                            },
                        )
                        OrganizerHeading(
                            category = category,
                            categories = state.organization.categories,
                            onReorderCategories = onReorderCategories,
                            onEdit = {
                                newCategory = false
                                editing = true
                            },
                        )
                        OrganizerCards(
                            contacts = organizerContacts(state.allContacts, state.organization, selectedId),
                            categoryId = selectedId,
                            organization = state.organization,
                            onMoveCard = onMoveCard,
                            onReorderCards = onReorderCards,
                            onFavorite = onFavorite,
                            modifier = Modifier.weight(1f).contentWidth(),
                        )
                    }
                }
                if (snackbarHostState != null) {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
                    )
                }
            }
        }
    }
}

@Composable
private fun OrganizerCategories(
    categories: List<LibraryCategory>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onCreate: () -> Unit,
) {
    Row(
        Modifier.contentWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        categories.forEach { category ->
            FilterChip(
                selected = selectedId == category.id,
                onClick = { onSelect(category.id) },
                label = { Text(category.name) },
                leadingIcon = { Icon(organizerIcon(category.icon), contentDescription = null) },
            )
        }
        FilterChip(
            selected = selectedId == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.library_uncategorized)) },
        )
        IconButton(onClick = onCreate) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.library_category_add))
        }
    }
}

@Composable
private fun OrganizerHeading(
    category: LibraryCategory?,
    categories: List<LibraryCategory>,
    onReorderCategories: (List<String>) -> Unit,
    onEdit: () -> Unit,
) {
    Row(
        Modifier.contentWidth().padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            category?.name ?: stringResource(R.string.library_uncategorized),
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
        )
        if (category != null) {
            val index = categories.indexOfFirst { it.id == category.id }
            val ids = categories.map { it.id }
            IconButton(
                onClick = { onReorderCategories(ids.moved(index, index - 1)) },
                enabled = index > 0,
            ) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.library_category_move_up, category.name),
                )
            }
            IconButton(
                onClick = { onReorderCategories(ids.moved(index, index + 1)) },
                enabled = index >= 0 && index < categories.lastIndex,
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.library_category_move_down, category.name),
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.library_category_edit))
            }
        }
    }
    Text(
        stringResource(R.string.library_reorder_hint),
        Modifier.contentWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun OrganizerCards(
    contacts: List<ReceivedContact>,
    categoryId: String?,
    organization: LibraryOrganization,
    onMoveCard: (Long, String?) -> Unit,
    onReorderCards: (String?, List<Long>) -> Unit,
    onFavorite: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ids = contacts.map { it.id }
    LazyColumn(modifier.navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (contacts.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.library_category_empty),
                    Modifier.padding(20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        itemsIndexed(contacts, key = { _, contact -> contact.id }) { index, contact ->
            OrganizerCardRow(
                contact = contact,
                favorite = contact.id in organization.favorites,
                categories = organization.categories,
                categoryId = categoryId,
                canMoveUp = index > 0,
                canMoveDown = index < contacts.lastIndex,
                onReorder = { offset -> onReorderCards(categoryId, ids.moved(index, index + offset)) },
                onMove = { onMoveCard(contact.id, it) },
                onFavorite = { onFavorite(contact.id, !organization.favorites.contains(contact.id)) },
            )
        }
    }
}

@Composable
private fun OrganizerCardRow(
    contact: ReceivedContact,
    favorite: Boolean,
    categories: List<LibraryCategory>,
    categoryId: String?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onReorder: (Int) -> Unit,
    onMove: (String?) -> Unit,
    onFavorite: () -> Unit,
) {
    var dragY by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableFloatStateOf(1f) }
    val latestReorder by rememberUpdatedState(onReorder)
    val dragDescription = stringResource(R.string.library_drag_card, contact.name)
    Column(
        Modifier.padding(horizontal = 12.dp).onSizeChanged { rowHeight = it.height.toFloat() }
            .graphicsLayer { translationY = dragY }
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp)),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).semantics { contentDescription = dragDescription }
                    .pointerInput(contact.id, rowHeight) {
                        detectDragGestures(
                            onDragStart = { dragY = 0f },
                            onDragCancel = { dragY = 0f },
                            onDragEnd = {
                                val offset = (dragY / rowHeight).roundToInt()
                                if (offset != 0) latestReorder(offset)
                                dragY = 0f
                            },
                        ) { change, amount ->
                            change.consume()
                            dragY += amount.y
                        }
                    },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.Menu, contentDescription = null) }
            OrganizerThumbnail(contact)
            Text(
                contact.name,
                Modifier.weight(1f).padding(horizontal = 12.dp),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = onFavorite) {
                Icon(
                    if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(
                        if (favorite) R.string.library_favorite_remove else R.string.library_favorite_add,
                    ),
                )
            }
        }
        OrganizerCardActions(canMoveUp, canMoveDown, onReorder, categories, categoryId, onMove)
    }
}

@Composable
private fun OrganizerThumbnail(contact: ReceivedContact) {
    Box(
        Modifier.size(width = 64.dp, height = 64.dp / PHYSICAL_CARD_RATIO).clearAndSetSemantics {},
    ) {
        LibraryCard(contact)
    }
}

@Composable
private fun OrganizerCardActions(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onReorder: (Int) -> Unit,
    categories: List<LibraryCategory>,
    categoryId: String?,
    onMove: (String?) -> Unit,
) {
    var moveMenu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        IconButton(onClick = { onReorder(-1) }, enabled = canMoveUp) {
            Icon(Icons.Default.KeyboardArrowUp, stringResource(R.string.library_move_up))
        }
        IconButton(onClick = { onReorder(1) }, enabled = canMoveDown) {
            Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.library_move_down))
        }
        Box {
            TextButton(onClick = { moveMenu = true }) { Text(stringResource(R.string.library_move_to)) }
            DropdownMenu(expanded = moveMenu, onDismissRequest = { moveMenu = false }) {
                if (categoryId != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.library_uncategorized)) },
                        onClick = {
                            moveMenu = false
                            onMove(null)
                        },
                    )
                }
                categories.filter { it.id != categoryId }.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        onClick = {
                            moveMenu = false
                            onMove(category.id)
                        },
                    )
                }
                if (categories.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.library_category_create_first)) },
                        onClick = {},
                        enabled = false,
                    )
                }
            }
        }
    }
}

private data class CategoryDraft(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val icon: LibraryCategoryIcon = LibraryCategoryIcon.PEOPLE,
    val accent: RgbColor = CardPalette.Default.accent,
    val accent2: RgbColor? = null,
)

private val CategoryDraftSaver = listSaver<CategoryDraft, String>(
    save = { listOf(it.id, it.name, it.icon.name, it.accent.hex, it.accent2?.hex.orEmpty()) },
    restore = { values ->
        val fields = values.iterator()
        CategoryDraft(
            id = fields.next(),
            name = fields.next(),
            icon = LibraryCategoryIcon.valueOf(fields.next()),
            accent = requireNotNull(RgbColor.parse(fields.next())),
            accent2 = RgbColor.parse(fields.next()),
        )
    },
)

@Composable
private fun CategoryEditor(
    initial: LibraryCategory?,
    onSave: (LibraryCategory) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by rememberSaveable(initial?.id, stateSaver = CategoryDraftSaver) {
        val value = initial?.let { CategoryDraft(it.id, it.name, it.icon, it.accent, it.accent2) } ?: CategoryDraft()
        mutableStateOf(value)
    }
    var picker by rememberSaveable(initial?.id) { mutableStateOf<Int?>(null) }
    var confirmDelete by rememberSaveable(initial?.id) { mutableStateOf(false) }
    Column(modifier) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryDraftPreview(draft)
            CategoryNameField(draft.name) { draft = draft.copy(name = it) }
            CategoryIconPicker(draft.icon) { draft = draft.copy(icon = it) }
            CategoryColourControls(
                draft = draft,
                onChange = { draft = it },
                onPick = { picker = it },
            )
            if (initial != null) {
                TextButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Text(stringResource(R.string.library_category_delete), Modifier.padding(start = 8.dp))
                }
            }
        }
        CategoryEditorFooter(
            valid = draft.name.trim().isNotBlank(),
            onCancel = onCancel,
            onSave = { onSave(LibraryCategory(draft.id, draft.name.trim(), draft.icon, draft.accent, draft.accent2)) },
        )
    }
    picker?.let { stop ->
        CategoryColourDialog(
            initial = if (stop == 0) draft.accent else draft.accent2 ?: draft.accent,
            onSelect = {
                draft = if (stop == 0) draft.copy(accent = it) else draft.copy(accent2 = it)
                picker = null
            },
            onDismiss = { picker = null },
        )
    }
    if (confirmDelete) CategoryDeleteDialog(onConfirm = onDelete, onDismiss = { confirmDelete = false })
}

@Composable
private fun CategoryDraftPreview(draft: CategoryDraft) {
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp).background(
            Brush.linearGradient(
                listOf(draft.accent.toComposeColor(), (draft.accent2 ?: draft.accent).toComposeColor()),
            ),
            RoundedCornerShape(20.dp),
        ).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val color = es.igarciadelamo.txoka.domain.profile.CardContrast.contentColor(
            CardPalette(draft.accent, draft.accent2 ?: draft.accent),
        ).toComposeColor()
        Icon(organizerIcon(draft.icon), contentDescription = null, tint = color)
        Text(
            draft.name.ifBlank { stringResource(R.string.library_category_new) },
            color = color,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun CategoryNameField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onChange(text.take(MAX_CATEGORY_NAME_LENGTH).filterNot(Char::isISOControl)) },
        label = { Text(stringResource(R.string.library_category_name)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CategoryIconPicker(selected: LibraryCategoryIcon, onSelect: (LibraryCategoryIcon) -> Unit) {
    Text(stringResource(R.string.library_category_icon), style = MaterialTheme.typography.titleSmall)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LibraryCategoryIcon.entries.forEach { icon ->
            FilterChip(
                selected = icon == selected,
                onClick = { onSelect(icon) },
                label = { Icon(organizerIcon(icon), contentDescription = stringResource(organizerIconLabel(icon))) },
            )
        }
    }
}

@Composable
private fun CategoryColourControls(draft: CategoryDraft, onChange: (CategoryDraft) -> Unit, onPick: (Int) -> Unit) {
    Text(stringResource(R.string.library_category_background), style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilterChip(
            selected = draft.accent2 == null,
            onClick = { onChange(draft.copy(accent2 = null)) },
            label = { Text(stringResource(R.string.library_category_flat)) },
        )
        FilterChip(
            selected = draft.accent2 != null,
            onClick = { onChange(draft.copy(accent2 = draft.accent2 ?: draft.accent.darken(.35))) },
            label = { Text(stringResource(R.string.library_category_gradient)) },
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = { onPick(0) }) {
            ColourSwatch(draft.accent)
            Text(draft.accent.hex, Modifier.padding(start = 8.dp))
        }
        draft.accent2?.let { color ->
            TextButton(onClick = { onPick(1) }) {
                ColourSwatch(color)
                Text(color.hex, Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun ColourSwatch(color: RgbColor) {
    Box(Modifier.size(28.dp).background(color.toComposeColor(), RoundedCornerShape(8.dp)))
}

@Composable
private fun CategoryEditorFooter(valid: Boolean, onCancel: () -> Unit, onSave: () -> Unit) {
    HorizontalDivider()
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
        Button(onClick = onSave, enabled = valid, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.library_category_save))
        }
    }
}

@Composable
private fun CategoryColourDialog(initial: RgbColor, onSelect: (RgbColor) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                CardColourPicker(
                    initial = initial,
                    title = stringResource(R.string.library_category_colour),
                    onPreview = {},
                    onSelect = onSelect,
                    onCancel = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun CategoryDeleteDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.library_category_delete)) },
        text = { Text(stringResource(R.string.library_category_delete_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

private fun organizerContacts(
    contacts: List<ReceivedContact>,
    organization: LibraryOrganization,
    categoryId: String?,
): List<ReceivedContact> {
    val items = contacts.filter { organization.assignments[it.id] == categoryId }
    val order = organization.cardOrder[categoryId ?: UNCATEGORIZED_CATEGORY_ID].orEmpty().withIndex()
        .associate { it.value to it.index }
    return items.sortedBy { order[it.id] ?: Int.MAX_VALUE }
}

private fun <T> List<T>.moved(from: Int, to: Int): List<T> = toMutableList().apply {
    val item = removeAt(from)
    add(to.coerceIn(0, size), item)
}

private fun organizerIcon(icon: LibraryCategoryIcon): ImageVector = when (icon) {
    LibraryCategoryIcon.PEOPLE -> Icons.Default.Person
    LibraryCategoryIcon.WORK -> Icons.Default.Build
    LibraryCategoryIcon.FRIENDS -> Icons.Default.Favorite
    LibraryCategoryIcon.FAMILY -> Icons.Default.Home
    LibraryCategoryIcon.EVENT -> Icons.Default.DateRange
    LibraryCategoryIcon.TRAVEL -> Icons.Default.Place
    LibraryCategoryIcon.STUDY -> Icons.Default.Create
    LibraryCategoryIcon.OTHER -> Icons.Default.Star
}

private fun organizerIconLabel(icon: LibraryCategoryIcon): Int = when (icon) {
    LibraryCategoryIcon.PEOPLE -> R.string.library_icon_people
    LibraryCategoryIcon.WORK -> R.string.library_icon_work
    LibraryCategoryIcon.FRIENDS -> R.string.library_icon_friends
    LibraryCategoryIcon.FAMILY -> R.string.library_icon_family
    LibraryCategoryIcon.EVENT -> R.string.library_icon_event
    LibraryCategoryIcon.TRAVEL -> R.string.library_icon_travel
    LibraryCategoryIcon.STUDY -> R.string.library_icon_study
    LibraryCategoryIcon.OTHER -> R.string.library_icon_other
}

@Preview(showBackground = true)
@Composable
private fun LibraryOrganizerPreview() {
    TxokaTheme {
        CategoryEditor(initial = null, onSave = {}, onDelete = {}, onCancel = {}, modifier = Modifier.fillMaxSize())
    }
}
