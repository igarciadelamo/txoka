package es.igarciadelamo.txoka.ui.received

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

@Composable
fun ReceivedDetailRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReceivedDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val errorMessage = stringResource(R.string.library_operation_failed)
    LaunchedEffect(viewModel, errorMessage) {
        viewModel.errors.collect { snackbar.showSnackbar(errorMessage) }
    }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }

    Box(modifier.fillMaxSize()) {
        state.contact?.let { contact ->
            ReceivedDetailScreen(
                contact = contact,
                note = state.note,
                onNoteChange = viewModel::onNoteChange,
                onSaveNote = viewModel::saveNote,
                onDelete = viewModel::delete,
                onBack = onBack,
            )
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }
}
