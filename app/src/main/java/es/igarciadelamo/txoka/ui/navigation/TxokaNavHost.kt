package es.igarciadelamo.txoka.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.ui.profile.ProfileEditorScreen
import es.igarciadelamo.txoka.ui.received.ReceivedScreen
import es.igarciadelamo.txoka.ui.settings.SettingsRoute
import es.igarciadelamo.txoka.ui.share.ShareRoute

@Composable
fun TxokaNavHost(
    profile: Profile?,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = ShareRoute,
        modifier = modifier,
    ) {
        composable<ShareRoute> {
            ShareRoute(
                profile = profile,
                onOpenReceived = { navController.navigate(ReceivedRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onCreateCard = { navController.navigate(ProfileEditorRoute) },
            )
        }
        composable<ReceivedRoute> {
            ReceivedScreen(onBack = navController::popBackStack)
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
