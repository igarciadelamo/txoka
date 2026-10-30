package es.igarciadelamo.txoka.ui.received

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.RgbColor
import es.igarciadelamo.txoka.domain.received.ReceivedContact
import es.igarciadelamo.txoka.ui.components.Eyebrow
import es.igarciadelamo.txoka.ui.components.ScreenTopBar
import es.igarciadelamo.txoka.ui.components.toComposeColor
import es.igarciadelamo.txoka.ui.theme.Corners
import es.igarciadelamo.txoka.ui.theme.Spacing
import es.igarciadelamo.txoka.ui.theme.TxokaTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ReceivedScreen(
    state: ReceivedListUiState,
    onQueryChange: (String) -> Unit,
    onOpenContact: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ScreenTopBar(
            title = stringResource(R.string.received_title),
            onBack = onBack,
        )
        SearchField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.padding(horizontal = Spacing.xl, vertical = Spacing.md),
        )
        if (state.contacts.isEmpty()) {
            EmptyState(
                isLoading = state.isLoading,
                searching = state.query.isNotBlank(),
                modifier = Modifier
                    .weight(1f)
                    .navigationBarsPadding(),
            )
        } else {
            ContactList(
                contacts = state.contacts,
                onOpenContact = onOpenContact,
                modifier = Modifier
                    .weight(1f)
                    .navigationBarsPadding(),
            )
        }
    }
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
            .clickable(onClick = onClick)
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
            ),
            onQueryChange = {},
            onOpenContact = {},
            onBack = {},
        )
    }
}
