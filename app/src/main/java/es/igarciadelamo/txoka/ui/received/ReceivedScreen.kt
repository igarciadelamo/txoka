package es.igarciadelamo.txoka.ui.received

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.library.LibraryCategory
import es.igarciadelamo.txoka.domain.library.LibraryViewMode
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.RgbColor
import es.igarciadelamo.txoka.domain.received.ReceivedContact
import es.igarciadelamo.txoka.ui.components.CardViewer
import es.igarciadelamo.txoka.ui.components.Eyebrow
import es.igarciadelamo.txoka.ui.components.TxokaAppBar
import es.igarciadelamo.txoka.ui.components.contentWidth
import es.igarciadelamo.txoka.ui.components.toComposeColor
import es.igarciadelamo.txoka.ui.library.LibraryContactActions
import es.igarciadelamo.txoka.ui.library.LibraryCoverFlow
import es.igarciadelamo.txoka.ui.library.LibraryFilter
import es.igarciadelamo.txoka.ui.library.LibraryModeControl
import es.igarciadelamo.txoka.ui.library.LibraryOrganizer
import es.igarciadelamo.txoka.ui.library.LibraryPagerControls
import es.igarciadelamo.txoka.ui.library.LibraryShelves
import es.igarciadelamo.txoka.ui.library.categoryName
import es.igarciadelamo.txoka.ui.library.libraryProfile
import es.igarciadelamo.txoka.ui.theme.Corners
import es.igarciadelamo.txoka.ui.theme.Spacing
import es.igarciadelamo.txoka.ui.theme.TxokaTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private data class LibraryCallbacks(
    val pager: PagerState,
    val openCard: (ReceivedContact) -> Unit,
    val openContact: (Long) -> Unit,
    val organize: () -> Unit,
    val filter: (LibraryFilter) -> Unit,
    val viewMode: (LibraryViewMode) -> Unit,
    val favorite: (Long, Boolean) -> Unit,
    val centered: (Long) -> Unit,
)

@Composable
fun ReceivedScreen(
    state: ReceivedListUiState,
    snackbarHostState: SnackbarHostState,
    onQueryChange: (String) -> Unit,
    onOpenContact: (Long) -> Unit,
    onClearLibrary: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onFilterChange: (LibraryFilter) -> Unit = {},
    onViewModeChange: (LibraryViewMode) -> Unit = {},
    onSaveCategory: (LibraryCategory) -> Unit = {},
    onDeleteCategory: (String) -> Unit = {},
    onReorderCategories: (List<String>) -> Unit = {},
    onMoveCard: (Long, String?) -> Unit = { _, _ -> },
    onReorderCards: (String?, List<Long>) -> Unit = { _, _ -> },
    onFavorite: (Long, Boolean) -> Unit = { _, _ -> },
) {
    var confirmClear by remember { mutableStateOf(false) }
    var organize by rememberSaveable { mutableStateOf(false) }
    var viewerId by rememberSaveable { mutableStateOf<Long?>(null) }
    val viewer = state.allContacts.firstOrNull { it.id == viewerId }
    LaunchedEffect(state.isLoading, state.allContacts, viewerId) {
        if (!state.isLoading && viewerId != null && viewer == null) viewerId = null
    }
    var centeredId by remember { mutableStateOf<Long?>(null) }
    val pager = rememberPagerState(pageCount = { state.contacts.size })
    val callbacks = LibraryCallbacks(
        pager = pager,
        openCard = { viewerId = it.id },
        openContact = onOpenContact,
        organize = { organize = true },
        filter = onFilterChange,
        viewMode = onViewModeChange,
        favorite = onFavorite,
        centered = { centeredId = it },
    )
    BoxWithConstraints(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (maxWidth > maxHeight) {
            LandscapeLibrary(
                state,
                centeredId,
                callbacks,
                onQueryChange,
                { confirmClear = true },
                onBack,
                viewer == null && !organize,
            )
        } else {
            PortraitLibrary(
                state,
                callbacks,
                onQueryChange,
                { confirmClear = true },
                onBack,
                viewer == null && !organize,
            )
        }
        if (!organize) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
            )
        }
    }
    if (organize && !state.isLoading) {
        LibraryOrganizer(
            state,
            onSaveCategory,
            onDeleteCategory,
            onMoveCard,
            onReorderCards,
            onFavorite,
            onDismiss = { organize = false },
            snackbarHostState = snackbarHostState,
            onReorderCategories = onReorderCategories,
        )
    }
    viewer?.let { CardViewer(it.libraryProfile(), onDismiss = { viewerId = null }) }
    if (confirmClear) {
        ClearLibraryDialog(
            count = state.allContacts.size,
            onConfirm = {
                confirmClear = false
                onClearLibrary()
            },
            onDismiss = { confirmClear = false },
        )
    }
}

@Composable
private fun PortraitLibrary(
    state: ReceivedListUiState,
    callbacks: LibraryCallbacks,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    visible: Boolean,
) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        LibraryAppBar(state.hasContacts, onClear, onBack, callbacks.organize)
        SearchField(
            state.query,
            onQueryChange,
            Modifier.contentWidth().padding(horizontal = Spacing.xl, vertical = Spacing.sm),
        )
        LibraryFilters(state.filter, callbacks.filter, Modifier.contentWidth())
        LibraryPresentation(state, callbacks, landscape = false, visible, Modifier.weight(1f))
        LibraryModeControl(
            state.organization.viewMode,
            callbacks.viewMode,
            Modifier.contentWidth().navigationBarsPadding(),
        )
    }
}

@Composable
private fun LandscapeLibrary(
    state: ReceivedListUiState,
    centeredId: Long?,
    callbacks: LibraryCallbacks,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    visible: Boolean,
) {
    Row(Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(
            Modifier.width(176.dp).fillMaxHeight().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LibraryRailHeader(state.hasContacts, onClear, onBack, callbacks.organize)
            SearchField(state.query, onQueryChange, Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
            LibraryFilters(state.filter, callbacks.filter)
            val contact = state.contacts.firstOrNull { it.id == centeredId } ?: state.contacts.firstOrNull()
            if (state.organization.viewMode == LibraryViewMode.COVER_FLOW && contact != null) {
                Text(
                    categoryName(contact, state.organization),
                    Modifier.padding(8.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
                TextButton(onClick = { callbacks.openContact(contact.id) }) {
                    Text(contact.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                LibraryPagerControls(callbacks.pager, state.contacts.size, onDark = false)
                LibraryContactActions(
                    contact,
                    state.organization,
                    callbacks.favorite,
                    callbacks.organize,
                    onDark = false,
                    stacked = true,
                )
            }
            if (state.organization.viewMode != LibraryViewMode.COVER_FLOW || contact == null) {
                TextButton(onClick = callbacks.organize) { Text(stringResource(R.string.library_organize)) }
            }
            LibraryModeControl(state.organization.viewMode, callbacks.viewMode)
        }
        LibraryPresentation(state, callbacks, landscape = true, visible, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun LibraryRailHeader(canClear: Boolean, onClear: () -> Unit, onBack: () -> Unit, onOrganize: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
        }
        IconButton(onClick = onOrganize) {
            Icon(Icons.Default.Edit, stringResource(R.string.library_organize))
        }
        Box {
            IconButton(onClick = { menuOpen = true }, enabled = canClear) {
                Icon(Icons.Default.MoreVert, stringResource(R.string.action_more))
            }
            DropdownMenu(menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_clear)) },
                    onClick = {
                        menuOpen = false
                        onClear()
                    },
                )
            }
        }
    }
    Text(
        stringResource(R.string.received_title),
        Modifier.padding(horizontal = 8.dp),
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun LibraryPresentation(
    state: ReceivedListUiState,
    callbacks: LibraryCallbacks,
    landscape: Boolean,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    if (state.contacts.isEmpty()) {
        EmptyState(state.isLoading, state.query.isNotBlank() || state.filter == LibraryFilter.FAVORITES, modifier)
    } else {
        when (state.organization.viewMode) {
            LibraryViewMode.COVER_FLOW -> LibraryCoverFlow(
                contacts = state.contacts,
                organization = state.organization,
                pager = callbacks.pager,
                landscape = landscape,
                onOpenCard = callbacks.openCard,
                onOpenContact = callbacks.openContact,
                onFavorite = callbacks.favorite,
                onOrganize = callbacks.organize,
                onCentered = callbacks.centered,
                modifier = modifier,
                visible = visible,
            )
            LibraryViewMode.SHELVES -> LibraryShelves(state.sections, callbacks.openCard, callbacks.organize, modifier)
            LibraryViewMode.LIST -> ContactList(state.contacts, callbacks.openContact, modifier.contentWidth())
        }
    }
}

@Composable
private fun LibraryFilters(filter: LibraryFilter, onChange: (LibraryFilter) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LibraryFilter.entries.forEach { option ->
            FilterChip(
                selected = filter == option,
                onClick = { onChange(option) },
                label = {
                    Text(
                        stringResource(
                            when (option) {
                                LibraryFilter.ALL -> R.string.library_filter_all
                                LibraryFilter.FAVORITES -> R.string.library_filter_favorites
                                LibraryFilter.RECENT -> R.string.library_filter_recent
                            },
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun LibraryAppBar(canClear: Boolean, onClear: () -> Unit, onBack: () -> Unit, onOrganize: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    TxokaAppBar(
        title = stringResource(R.string.received_title),
        onBack = onBack,
        actions = {
            IconButton(onClick = onOrganize) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.library_organize))
            }
            if (canClear) {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.action_more),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.library_clear)) },
                        onClick = {
                            menuOpen = false
                            onClear()
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun ClearLibraryDialog(count: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.library_clear_title)) },
        text = {
            Text(text = pluralStringResource(R.plurals.library_clear_message, count, count))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.library_clear))
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
private fun SearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val keyboard = LocalSoftwareKeyboardController.current
    val label = stringResource(R.string.received_search)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Corners.field))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = label },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun ContactList(
    contacts: List<ReceivedContact>,
    onOpenContact: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val groups = contacts.groupBy { contact ->
        val date = Instant.ofEpochMilli(contact.receivedAt).atZone(zone).toLocalDate()
        if (date == today) Group.Today else Group.Earlier
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl),
    ) {
        Group.entries.forEach { group ->
            val items = groups[group].orEmpty()
            if (items.isEmpty()) return@forEach
            Eyebrow(
                text = stringResource(group.label),
                modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.sm),
            )
            items.forEach { contact ->
                ContactRow(contact = contact, onClick = { onOpenContact(contact.id) })
            }
        }
    }
}

@Composable
private fun ContactRow(contact: ReceivedContact, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Corners.field))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(contact = contact)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val detail = contact.phone ?: contact.email
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Avatar(contact: ReceivedContact) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(Corners.chip))
            .background(
                Brush.linearGradient(
                    listOf(contact.card.accent.toComposeColor(), contact.card.accent2.toComposeColor()),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = contact.initials(),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.surface,
        )
    }
}

@Composable
private fun EmptyState(isLoading: Boolean, searching: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.xxl),
        contentAlignment = Alignment.Center,
    ) {
        if (!isLoading) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(
                        if (searching) R.string.received_empty else R.string.received_empty_title,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (!searching) {
                    Text(
                        text = stringResource(R.string.received_empty_body),
                        modifier = Modifier.padding(top = Spacing.sm),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private enum class Group(val label: Int) {
    Today(R.string.group_today),
    Earlier(R.string.group_earlier),
}

private fun ReceivedContact.initials(): String = name
    .split(' ')
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { word -> word.first().uppercase() }

@Preview(showBackground = true)
@Composable
private fun ReceivedScreenPreview() {
    val now = System.currentTimeMillis()
    val snackbarHostState = remember { SnackbarHostState() }
    TxokaTheme {
        ReceivedScreen(
            state = ReceivedListUiState(
                contacts = listOf(
                    ReceivedContact(id = 1, name = "Marta Ruiz", phone = "+34 600 111 222", receivedAt = now),
                    ReceivedContact(
                        id = 2,
                        name = "Ana López",
                        email = "ana@example.com",
                        receivedAt = now - 86400000 * 3,
                        card = CardPalette(
                            accent = RgbColor(0x7C, 0x3A, 0xED),
                            accent2 = RgbColor(0xC0, 0x26, 0xD3),
                        ),
                    ),
                ),
                isLoading = false,
                hasContacts = true,
            ),
            snackbarHostState = snackbarHostState,
            onQueryChange = {},
            onOpenContact = {},
            onClearLibrary = {},
            onBack = {},
        )
    }
}
