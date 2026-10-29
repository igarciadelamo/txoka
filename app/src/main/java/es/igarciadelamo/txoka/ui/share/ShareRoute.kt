package es.igarciadelamo.txoka.ui.share

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.igarciadelamo.txoka.domain.profile.Profile

@Composable
fun ShareRoute(
    profile: Profile?,
    onOpenReceived: () -> Unit,
    onOpenSettings: () -> Unit,
    onCreateCard: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShareViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshNfcStatus()
        onPauseOrDispose { }
    }

    ShareScreen(
        profile = profile,
        sharingEnabled = state.sharingEnabled,
        nfc = state.nfc,
        onOpenReceived = onOpenReceived,
        onOpenSettings = onOpenSettings,
        onCreateCard = onCreateCard,
        modifier = modifier,
    )
}

/**
 * Keeps the screen awake while the card is being advertised, because HCE only answers with the
 * display on.
 */
@Composable
fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}
