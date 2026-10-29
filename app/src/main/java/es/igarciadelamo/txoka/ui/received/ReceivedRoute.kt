package es.igarciadelamo.txoka.ui.received

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ReceivedRoute(
    onBack: () -> Unit,
    onOpenContact: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReceivedListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ReceivedScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onOpenContact = onOpenContact,
        onBack = onBack,
        modifier = modifier,
    )
}
