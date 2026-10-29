package es.igarciadelamo.txoka.ui.received

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ReceivedDetailRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReceivedDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }

    state.contact?.let { contact ->
        ReceivedDetailScreen(
            contact = contact,
            note = state.note,
            onNoteChange = viewModel::onNoteChange,
            onSaveNote = viewModel::saveNote,
            onDelete = viewModel::delete,
            onBack = onBack,
            modifier = modifier,
        )
    }
}
