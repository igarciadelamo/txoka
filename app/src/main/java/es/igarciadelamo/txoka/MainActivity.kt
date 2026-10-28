package es.igarciadelamo.txoka

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import es.igarciadelamo.txoka.ui.TxokaApp
import es.igarciadelamo.txoka.ui.theme.TxokaTheme

private const val SPLASH_DURATION_MS = 2_000L

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        val isColdStart = savedInstanceState == null
        val splashDeadline = SystemClock.uptimeMillis() + SPLASH_DURATION_MS

        splashScreen.setKeepOnScreenCondition {
            isColdStart && SystemClock.uptimeMillis() < splashDeadline
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TxokaTheme {
                TxokaApp()
            }
        }
    }
}
