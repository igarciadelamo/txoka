package es.igarciadelamo.txoka

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import es.igarciadelamo.txoka.nfc.NfcReader
import es.igarciadelamo.txoka.ui.TxokaApp
import es.igarciadelamo.txoka.ui.theme.TxokaTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var nfcReader: NfcReader

    override fun onCreate(savedInstanceState: Bundle?) {
        // Keep the system splash only until the first frame; the branded splash lives in Compose.
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        nfcReader.handle(intent)
        setContent {
            TxokaTheme {
                TxokaApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcReader.start(this)
    }

    override fun onPause() {
        nfcReader.stop(this)
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        nfcReader.handle(intent)
    }
}
