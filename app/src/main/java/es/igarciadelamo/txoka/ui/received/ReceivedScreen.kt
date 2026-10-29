package es.igarciadelamo.txoka.ui.received

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.received.ReceivedContact
import es.igarciadelamo.txoka.ui.theme.TxokaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceivedScreen(
    state: ReceivedListUiState,
    onQueryChange: (String) -> Unit,
    onOpenContact: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.received_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(text = stringResource(R.string.received_search)) },
                singleLine = true,
            )

            if (state.contacts.isEmpty()) {
                EmptyState(isLoading = state.isLoading)
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = state.contacts, key = { contact -> contact.id }) { contact ->
                        ListItem(
                            headlineContent = { Text(text = contact.name) },
                            supportingContent = contact.phone?.let { phone -> { Text(text = phone) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenContact(contact.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(isLoading: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (!isLoading) {
            Text(
                text = stringResource(R.string.received_empty),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReceivedScreenPreview() {
    TxokaTheme {
        ReceivedScreen(
            state = ReceivedListUiState(
                contacts = listOf(
                    ReceivedContact(id = 1, name = "Marta Ruiz", phone = "+34600111222"),
                    ReceivedContact(id = 2, name = "Ana López"),
                ),
                isLoading = false,
            ),
            onQueryChange = {},
            onOpenContact = {},
            onBack = {},
        )
    }
}
