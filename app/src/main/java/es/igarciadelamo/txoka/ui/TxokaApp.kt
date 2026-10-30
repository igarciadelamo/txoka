package es.igarciadelamo.txoka.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.igarciadelamo.txoka.ui.navigation.TxokaNavHost
import es.igarciadelamo.txoka.ui.receive.ReceiveConfirmScreen
import es.igarciadelamo.txoka.ui.receive.ReceiveViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

private const val SPLASH_MIN_MILLIS = 1700L
private const val SPLASH_LOAD_TIMEOUT_MILLIS = 400L
private const val SPLASH_FADE_MILLIS = 220

@Composable
fun TxokaApp(
    modifier: Modifier = Modifier,
    viewModel: ReceiveViewModel = hiltViewModel(),
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    var showSplash by rememberSaveable { mutableStateOf(true) }

    // Branded splash only on a cold start, and never longer than loading the stored profile.
    LaunchedEffect(Unit) {
        if (!showSplash) return@LaunchedEffect
        delay(SPLASH_MIN_MILLIS)
        withTimeoutOrNull(SPLASH_LOAD_TIMEOUT_MILLIS) {
            appViewModel.state.first { state -> !state.isLoading }
        }
        showSplash = false
    }

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

        AnimatedVisibility(
            visible = showSplash,
            enter = EnterTransition.None,
            exit = fadeOut(animationSpec = tween(SPLASH_FADE_MILLIS)),
        ) {
            SplashScreen()
        }
    }
}
