package es.igarciadelamo.txoka.ui.receive

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import es.igarciadelamo.txoka.ui.components.ContactCard
import es.igarciadelamo.txoka.ui.theme.TxokaTheme

@Composable
fun ReceiveConfirmScreen(
    payload: VCardPayload,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onDiscard)

    Surface(modifier = modifier.fillMaxSize()) {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            ) {
                Text(
                    text = stringResource(R.string.receive_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                ContactCard(profile = payload.toProfile())
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                        Text(text = stringResource(R.string.action_save_contact))
                    }
                    OutlinedButton(onClick = onDiscard, modifier = Modifier.fillMaxWidth()) {
                        Text(text = stringResource(R.string.action_discard))
                    }
                }
            }
        }
    }
}

private fun VCardPayload.toProfile(): Profile = Profile(
    name = name,
    phone = phone,
    email = email,
    sharePhone = phone != null,
    shareEmail = email != null,
    card = palette,
)

@Preview(showBackground = true)
@Composable
private fun ReceiveConfirmScreenPreview() {
    TxokaTheme {
        ReceiveConfirmScreen(
            payload = VCardPayload(
                name = "Marta Ruiz",
                phone = "+34600111222",
                email = "marta@example.com",
                palette = CardPalette.Presets[2],
            ),
            onSave = {},
            onDiscard = {},
        )
    }
}
