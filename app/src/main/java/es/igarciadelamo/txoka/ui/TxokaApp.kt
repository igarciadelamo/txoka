package es.igarciadelamo.txoka.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.ui.navigation.TxokaNavHost
import es.igarciadelamo.txoka.ui.receive.ReceiveConfirmScreen
import es.igarciadelamo.txoka.ui.receive.ReceiveViewModel
import es.igarciadelamo.txoka.ui.receive.SaveOutcome

@Composable
fun TxokaApp(modifier: Modifier = Modifier, viewModel: ReceiveViewModel = hiltViewModel()) {
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    val alreadyStored by viewModel.alreadyStored.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.received_saved)
    val updatedMessage = stringResource(R.string.received_updated)

    LaunchedEffect(viewModel, savedMessage, updatedMessage) {
        viewModel.outcome.collect { outcome ->
            snackbarHostState.showSnackbar(
                when (outcome) {
                    SaveOutcome.Saved -> savedMessage
                    SaveOutcome.Updated -> updatedMessage
                },
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        TxokaNavHost()

        // A received card covers the current screen so the user decides before anything else.
        pending?.let { payload ->
            ReceiveConfirmScreen(
                payload = payload,
                alreadyStored = alreadyStored,
                onSave = viewModel::save,
                onDiscard = viewModel::discard,
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
    }
}
