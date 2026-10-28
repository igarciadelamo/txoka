package es.igarciadelamo.txoka.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import es.igarciadelamo.txoka.ui.received.ReceivedScreen
import es.igarciadelamo.txoka.ui.settings.SettingsScreen
import es.igarciadelamo.txoka.ui.share.ShareScreen

@Composable
fun TxokaNavHost(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = ShareRoute,
        modifier = modifier,
    ) {
        composable<ShareRoute> {
            ShareScreen(
                onOpenReceived = { navController.navigate(ReceivedRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
            )
        }
        composable<ReceivedRoute> {
            ReceivedScreen(onBack = navController::popBackStack)
        }
        composable<SettingsRoute> {
            SettingsScreen(onBack = navController::popBackStack)
        }
    }
}
