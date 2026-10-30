package es.igarciadelamo.txoka.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenReceived: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshNfcStatus()
        onPauseOrDispose { }
    }

    SettingsScreen(
        state = state,
        onBack = onBack,
        onEditProfile = onEditProfile,
        onOpenReceived = onOpenReceived,
        onSharingChanged = viewModel::onSharingChanged,
        onRequireUnlockChanged = viewModel::onRequireUnlockChanged,
        onSimulateIncomingCard = viewModel::onSimulateIncomingCard,
        modifier = modifier,
    )
}
