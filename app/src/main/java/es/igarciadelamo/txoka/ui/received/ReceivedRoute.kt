package es.igarciadelamo.txoka.ui.received

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
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
    val snackbarHostState = remember { SnackbarHostState() }
    val clearedMessage = stringResource(R.string.library_cleared)
    val undoLabel = stringResource(R.string.action_undo)
    val errorMessage = stringResource(R.string.library_operation_failed)

    LaunchedEffect(viewModel, clearedMessage, undoLabel, errorMessage) {
        viewModel.events.collect { event ->
            when (event) {
                ReceivedListEvent.OperationFailed -> snackbarHostState.showSnackbar(errorMessage)
                ReceivedListEvent.LibraryCleared, ReceivedListEvent.RestoreFailed -> {
                    val result = snackbarHostState.showSnackbar(
                        message = if (event == ReceivedListEvent.LibraryCleared) clearedMessage else errorMessage,
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
        onFilterChange = viewModel::onFilterChange,
        onViewModeChange = viewModel::onViewModeChange,
        onSaveCategory = viewModel::onSaveCategory,
        onDeleteCategory = viewModel::onDeleteCategory,
        onReorderCategories = viewModel::onReorderCategories,
        onMoveCard = viewModel::onMoveCard,
        onReorderCards = viewModel::onReorderCards,
        onFavorite = viewModel::onFavorite,
        modifier = modifier,
    )
}
