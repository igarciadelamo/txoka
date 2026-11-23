package es.igarciadelamo.txoka.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.library.LibraryCategoryIcon
import es.igarciadelamo.txoka.domain.received.ReceivedContact
import es.igarciadelamo.txoka.ui.components.PHYSICAL_CARD_RATIO
import es.igarciadelamo.txoka.ui.components.contentWidth
import es.igarciadelamo.txoka.ui.components.toComposeColor

@Composable
internal fun LibraryShelves(
    sections: List<LibrarySection>,
    onOpenCard: (ReceivedContact) -> Unit,
    onOrganize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier.contentWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(sections.filter { it.category != null || it.contacts.isNotEmpty() }, key = { it.category?.id ?: "" }) {
            LibraryShelf(it, onOpenCard, onOrganize)
        }
    }
}

@Composable
private fun LibraryShelf(section: LibrarySection, onOpenCard: (ReceivedContact) -> Unit, onOrganize: () -> Unit) {
    val first = section.category?.accent?.toComposeColor() ?: MaterialTheme.colorScheme.secondary
    val second = section.category?.accent2?.toComposeColor() ?: first
    val surface = MaterialTheme.colorScheme.surface
    val colors = listOf(lerp(first, surface, .72f), lerp(second, surface, .72f))
    Column(
        Modifier.fillMaxWidth().background(
            Brush.linearGradient(colors),
            RoundedCornerShape(24.dp),
        ).padding(vertical = 8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                libraryCategoryIcon(section.category?.icon ?: LibraryCategoryIcon.OTHER),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Text(
                section.category?.name ?: stringResource(R.string.library_uncategorized),
                Modifier.weight(1f).padding(horizontal = 12.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(section.contacts.size.toString(), style = MaterialTheme.typography.labelLarge)
            IconButton(onClick = onOrganize) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.library_organize))
            }
        }
        if (section.contacts.isEmpty()) {
            Text(
                stringResource(R.string.library_category_empty),
                Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(section.contacts, key = { it.id }) { contact ->
                val description = stringResource(R.string.library_open_card, contact.name)
                Column(Modifier.width(288.dp)) {
                    Box(
                        Modifier.fillMaxWidth().height(288.dp / PHYSICAL_CARD_RATIO)
                            .clickable(role = Role.Button, onClick = { onOpenCard(contact) })
                            .semantics { contentDescription = description },
                    ) { LibraryCard(contact) }
                    Text(
                        contact.name,
                        Modifier.padding(vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

internal fun libraryCategoryIcon(icon: LibraryCategoryIcon): ImageVector = when (icon) {
    LibraryCategoryIcon.PEOPLE -> Icons.Default.Person
    LibraryCategoryIcon.WORK -> Icons.Default.Build
    LibraryCategoryIcon.FRIENDS -> Icons.Default.Star
    LibraryCategoryIcon.FAMILY -> Icons.Default.Home
    LibraryCategoryIcon.EVENT -> Icons.Default.DateRange
    LibraryCategoryIcon.TRAVEL -> Icons.Default.Place
    LibraryCategoryIcon.STUDY -> Icons.Default.Create
    LibraryCategoryIcon.OTHER -> Icons.Default.Person
}
