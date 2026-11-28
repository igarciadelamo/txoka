package es.igarciadelamo.txoka.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import es.igarciadelamo.txoka.ui.profile.ProfileEditorScreen
import es.igarciadelamo.txoka.ui.received.ReceivedDetailRoute
import es.igarciadelamo.txoka.ui.received.ReceivedRoute
import es.igarciadelamo.txoka.ui.settings.AboutScreen
import es.igarciadelamo.txoka.ui.settings.SettingsRoute
import es.igarciadelamo.txoka.ui.share.ShareRoute

/** Set by the library when the user asks to receive, read once by the hub. */
private const val RECEIVE_REQUEST = "receive_request"

@Composable
fun TxokaNavHost(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = ShareRoute,
        modifier = modifier,
    ) {
        composable<ShareRoute> { entry ->
            val receiveRequested by entry.savedStateHandle
                .getStateFlow(RECEIVE_REQUEST, false)
                .collectAsStateWithLifecycle()
            ShareRoute(
                onOpenReceived = { navController.navigate(ReceivedRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onCreateCard = { navController.navigate(ProfileEditorRoute) },
                onEditProfile = { navController.navigate(ProfileEditorRoute) },
                receiveRequested = receiveRequested,
                onReceiveRequestConsumed = { entry.savedStateHandle[RECEIVE_REQUEST] = false },
            )
        }
        composable<ReceivedRoute> {
            ReceivedRoute(
                onBack = navController::popBackStack,
                onOpenContact = { id -> navController.navigate(ReceivedDetailRoute(id)) },
                onReceive = {
                    navController.getBackStackEntry<ShareRoute>().savedStateHandle[RECEIVE_REQUEST] = true
                    navController.popBackStack<ShareRoute>(inclusive = false)
                },
            )
        }
        composable<ReceivedDetailRoute> {
            es.igarciadelamo.txoka.ui.received.ReceivedDetailRoute(
                onBack = navController::popBackStack,
            )
        }
        composable<SettingsRoute> {
            SettingsRoute(
                onBack = navController::popBackStack,
                onOpenReceived = { navController.navigate(ReceivedRoute) },
                onOpenAbout = { navController.navigate(AboutRoute) },
            )
        }
        composable<AboutRoute> {
            AboutScreen(onBack = navController::popBackStack)
        }
        composable<ProfileEditorRoute> {
            ProfileEditorScreen(
                onSaved = { navController.popBackStack() },
                onCancel = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() },
            )
        }
    }
}
