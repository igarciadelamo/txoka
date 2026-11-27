package es.igarciadelamo.txoka.ui.received

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.igarciadelamo.txoka.R

@Composable
fun ReceivedRoute(
    onBack: () -> Unit,
    onOpenContact: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReceivedListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pendingDelete by viewModel.pendingDelete.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clearedMessage = stringResource(R.string.library_cleared)
    val deletedMessage = stringResource(R.string.received_deleted)
    val undoLabel = stringResource(R.string.action_undo)

    LaunchedEffect(pendingDelete?.id, deletedMessage, undoLabel) {
        if (pendingDelete == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = deletedMessage,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete() else viewModel.discardDelete()
    }

    DisposableEffect(viewModel) {
        onDispose { viewModel.discardDelete() }
    }

    LaunchedEffect(viewModel, clearedMessage, undoLabel) {
        viewModel.events.collect { event ->
            when (event) {
                ReceivedListEvent.LibraryCleared -> {
                    val result = snackbarHostState.showSnackbar(
                        message = clearedMessage,
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoClear()
                }
            }
        }
    }

    ReceivedScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onQueryChange = viewModel::onQueryChange,
        onOpenContact = onOpenContact,
        onClearLibrary = viewModel::clearLibrary,
        onBack = onBack,
        modifier = modifier,
    )
}
