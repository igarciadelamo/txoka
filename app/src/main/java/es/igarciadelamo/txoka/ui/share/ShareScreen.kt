package es.igarciadelamo.txoka.ui.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.ui.components.ContactCard
import es.igarciadelamo.txoka.ui.theme.TxokaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(
    profile: Profile?,
    onOpenReceived: () -> Unit,
    onOpenSettings: () -> Unit,
    onCreateCard: () -> Unit,
    modifier: Modifier = Modifier,
    receivedCount: Int = 0,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenReceived) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ) {
                                    Text(text = receivedCount.toString())
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = stringResource(R.string.action_received),
                            )
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.action_settings),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (profile == null) {
                NoCardState(onCreateCard = onCreateCard)
            } else {
                ShareCardState(profile = profile)
            }
        }
    }
}

@Composable
private fun ShareCardState(profile: Profile, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ContactCard(profile = profile)
        Text(
            text = stringResource(R.string.share_hint),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NoCardState(onCreateCard: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.share_no_card_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.share_no_card_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onCreateCard) {
            Text(text = stringResource(R.string.action_create_card))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShareScreenPreview() {
    TxokaTheme {
        ShareScreen(
            profile = Profile(
                name = "Isidro García",
                phone = "+34600123456",
                email = "isidro@example.com",
                sharePhone = true,
                shareEmail = true,
                card = CardPalette.Default,
            ),
            onOpenReceived = {},
            onOpenSettings = {},
            onCreateCard = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShareScreenNoCardPreview() {
    TxokaTheme {
        ShareScreen(
            profile = null,
            onOpenReceived = {},
            onOpenSettings = {},
            onCreateCard = {},
        )
    }
}
