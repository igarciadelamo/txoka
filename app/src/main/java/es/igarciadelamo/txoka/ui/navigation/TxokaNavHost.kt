package es.igarciadelamo.txoka.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import es.igarciadelamo.txoka.ui.profile.ProfileEditorScreen
import es.igarciadelamo.txoka.ui.received.ReceivedDetailRoute
import es.igarciadelamo.txoka.ui.received.ReceivedRoute
import es.igarciadelamo.txoka.ui.settings.SettingsRoute
import es.igarciadelamo.txoka.ui.share.ShareRoute

@Composable
fun TxokaNavHost(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = ShareRoute,
        modifier = modifier,
    ) {
        composable<ShareRoute> {
            ShareRoute(
                onOpenReceived = { navController.navigate(ReceivedRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onCreateCard = { navController.navigate(ProfileEditorRoute) },
            )
        }
        composable<ReceivedRoute> {
            ReceivedRoute(
                onBack = navController::popBackStack,
                onOpenContact = { id -> navController.navigate(ReceivedDetailRoute(id)) },
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
                onEditProfile = { navController.navigate(ProfileEditorRoute) },
            )
        }
        composable<ProfileEditorRoute> {
            ProfileEditorScreen(onSaved = { navController.popBackStack() })
        }
    }
}
