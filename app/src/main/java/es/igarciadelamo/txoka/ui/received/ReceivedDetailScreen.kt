package es.igarciadelamo.txoka.ui.received

import android.content.Intent
import android.provider.ContactsContract
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.domain.received.ReceivedContact
import es.igarciadelamo.txoka.ui.components.CardLayout
import es.igarciadelamo.txoka.ui.components.ContactCard
import es.igarciadelamo.txoka.ui.components.Eyebrow
import es.igarciadelamo.txoka.ui.components.ScreenTopBar
import es.igarciadelamo.txoka.ui.theme.Corners
import es.igarciadelamo.txoka.ui.theme.Spacing
import es.igarciadelamo.txoka.ui.theme.TxokaTheme
import es.igarciadelamo.txoka.ui.theme.txoka
import java.text.DateFormat
import java.util.Date

@Composable
fun ReceivedDetailScreen(
    contact: ReceivedContact,
    note: String,
    onNoteChange: (String) -> Unit,
    onSaveNote: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ScreenTopBar(
            title = contact.name,
            onBack = onBack,
            actions = {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.action_more),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.action_delete)) },
                        onClick = {
                            menuOpen = false
                            confirmDelete = true
                        },
                    )
                }
            },
        )
        DetailContent(
            contact = contact,
            note = note,
            onNoteChange = onNoteChange,
            onSaveNote = onSaveNote,
            modifier = Modifier
                .weight(1f)
                .navigationBarsPadding(),
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(text = stringResource(R.string.delete_dialog_title)) },
            text = { Text(text = stringResource(R.string.delete_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                ) {
                    Text(text = stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun DetailContent(
    contact: ReceivedContact,
    note: String,
    onNoteChange: (String) -> Unit,
    onSaveNote: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var saved by remember(note) { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val received = remember(contact.receivedAt, locale) {
        DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(contact.receivedAt))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        ContactCard(profile = contact.toProfile(), layout = CardLayout.Hero)

        Text(
            text = stringResource(R.string.received_meta, received),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ActionBar(
            hasPhone = contact.phone != null,
            hasEmail = contact.email != null,
            onCall = { contact.phone?.let { context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$it".toUri())) } },
            onEmail = {
                contact.email?.let { context.startActivity(Intent(Intent.ACTION_SENDTO, "mailto:$it".toUri())) }
            },
            onAdd = { context.startActivity(contact.toInsertIntent()) },
        )

        NotesCard(
            note = note,
            saved = saved,
            onNoteChange = {
                saved = false
                onNoteChange(it)
            },
            onSave = {
                onSaveNote()
                saved = true
            },
        )
    }
}

@Composable
private fun ActionBar(
    hasPhone: Boolean,
    hasEmail: Boolean,
    onCall: () -> Unit,
    onEmail: () -> Unit,
    onAdd: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Corners.sheet))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        if (hasPhone) {
            ActionButton(
                icon = Icons.Filled.Phone,
                label = stringResource(R.string.action_call),
                onClick = onCall,
                modifier = Modifier.weight(1f),
            )
        }
        if (hasEmail) {
            ActionButton(
                icon = Icons.Filled.Email,
                label = stringResource(R.string.action_email),
                onClick = onEmail,
                modifier = Modifier.weight(1f),
            )
        }
        ActionButton(
            icon = Icons.Filled.Add,
            label = stringResource(R.string.action_add),
            onClick = onAdd,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Corners.field))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun NotesCard(note: String, saved: Boolean, onNoteChange: (String) -> Unit, onSave: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Corners.sheet))
            .background(MaterialTheme.colorScheme.surface)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(text = stringResource(R.string.received_note), modifier = Modifier.weight(1f))
            if (saved) {
                Text(
                    text = stringResource(R.string.note_saved),
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(MaterialTheme.txoka.successSoft)
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.txoka.success,
                )
            }
        }
        BasicTextField(
            value = note,
            onValueChange = onNoteChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Normal,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                androidx.compose.foundation.layout.Box {
                    if (note.isEmpty()) {
                        Text(
                            text = stringResource(R.string.received_note_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                    inner()
                }
            },
        )
        TextButton(onClick = onSave, modifier = Modifier.align(Alignment.End)) {
            Text(text = stringResource(R.string.action_save_note), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Prefilled contact editor, so the app never needs WRITE_CONTACTS. */
private fun ReceivedContact.toInsertIntent(): Intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
    type = ContactsContract.RawContacts.CONTENT_TYPE
    putExtra(ContactsContract.Intents.Insert.NAME, name)
    phone?.let { putExtra(ContactsContract.Intents.Insert.PHONE, it) }
    email?.let { putExtra(ContactsContract.Intents.Insert.EMAIL, it) }
}

private fun ReceivedContact.toProfile(): Profile = Profile(
    name = name,
    phone = phone,
    email = email,
    sharePhone = phone != null,
    shareEmail = email != null,
    card = card,
)

@Preview(showBackground = true)
@Composable
private fun ReceivedDetailScreenPreview() {
    TxokaTheme {
        ReceivedDetailScreen(
            contact = ReceivedContact(
                id = 1,
                name = "Marta Ruiz",
                phone = "+34 600 111 222",
                email = "marta@example.com",
                note = "Met at the Compose meetup.",
                receivedAt = System.currentTimeMillis(),
            ),
            note = "Met at the Compose meetup.",
            onNoteChange = {},
            onSaveNote = {},
            onDelete = {},
            onBack = {},
        )
    }
}
