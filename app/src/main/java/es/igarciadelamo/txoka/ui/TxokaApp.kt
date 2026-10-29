package es.igarciadelamo.txoka.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.igarciadelamo.txoka.ui.navigation.TxokaNavHost
import es.igarciadelamo.txoka.ui.receive.ReceiveConfirmScreen
import es.igarciadelamo.txoka.ui.receive.ReceiveViewModel

@Composable
fun TxokaApp(modifier: Modifier = Modifier, viewModel: ReceiveViewModel = hiltViewModel()) {
    val pending by viewModel.pending.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        TxokaNavHost()

        // A received card covers the current screen so the user decides before anything else.
        pending?.let { payload ->
            ReceiveConfirmScreen(
                payload = payload,
                onSave = viewModel::save,
                onDiscard = viewModel::discard,
            )
        }
    }
}
