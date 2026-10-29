package es.igarciadelamo.txoka.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.igarciadelamo.txoka.ui.navigation.TxokaNavHost
import es.igarciadelamo.txoka.ui.profile.ProfileEditorScreen

@Composable
fun TxokaApp(modifier: Modifier = Modifier, viewModel: AppViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profile = state.profile

    when {
        state.isLoading -> Box(modifier = modifier.fillMaxSize())
        profile == null -> ProfileEditorScreen(onSaved = {}, modifier = modifier)
        else -> TxokaNavHost(profile = profile, modifier = modifier)
    }
}
