package es.igarciadelamo.txoka.ui.received

import android.content.Intent
import android.provider.ContactsContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.domain.received.ReceivedContact
import es.igarciadelamo.txoka.ui.components.ContactCard
import es.igarciadelamo.txoka.ui.theme.TxokaTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = contact.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        ReceivedDetailContent(
            contact = contact,
            note = note,
            onNoteChange = onNoteChange,
            onSaveNote = onSaveNote,
            modifier = Modifier.padding(innerPadding),
        )
    }

    if (confirmDelete) {
        DeleteDialog(
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun ReceivedDetailContent(
    contact: ReceivedContact,
    note: String,
    onNoteChange: (String) -> Unit,
    onSaveNote: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ContactCard(profile = contact.toProfile())

        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.received_note)) },
            minLines = 3,
        )
        Button(onClick = onSaveNote, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.action_save_note))
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            contact.phone?.let { phone ->
                OutlinedButton(
                    onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$phone".toUri())) },
                ) {
                    Text(text = stringResource(R.string.action_call))
                }
            }
            contact.email?.let { email ->
                OutlinedButton(
                    onClick = { context.startActivity(Intent(Intent.ACTION_SENDTO, "mailto:$email".toUri())) },
                ) {
                    Text(text = stringResource(R.string.action_email))
                }
            }
        }

        OutlinedButton(
            onClick = { context.startActivity(contact.toInsertIntent()) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.action_add_to_contacts))
        }
    }
}

@Composable
private fun DeleteDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.delete_dialog_title)) },
        text = { Text(text = stringResource(R.string.delete_dialog_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    )
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
                phone = "+34600111222",
                email = "marta@example.com",
                note = "Conocida en el meetup",
            ),
            note = "Conocida en el meetup",
            onNoteChange = {},
            onSaveNote = {},
            onDelete = {},
            onBack = {},
        )
    }
}
