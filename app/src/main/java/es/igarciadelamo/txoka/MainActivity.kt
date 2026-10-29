package es.igarciadelamo.txoka

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import es.igarciadelamo.txoka.domain.protocol.VCardParseResult
import es.igarciadelamo.txoka.domain.protocol.VCardSerializer
import es.igarciadelamo.txoka.nfc.IncomingCards
import es.igarciadelamo.txoka.nfc.NfcReader
import es.igarciadelamo.txoka.ui.TxokaApp
import es.igarciadelamo.txoka.ui.theme.TxokaTheme
import javax.inject.Inject
import es.igarciadelamo.txoka.domain.protocol.NdefMessage as TxokaNdefMessage

private const val SPLASH_DURATION_MS = 2_000L

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var nfcReader: NfcReader

    @Inject
    lateinit var incomingCards: IncomingCards

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        val isColdStart = savedInstanceState == null
        val splashDeadline = SystemClock.uptimeMillis() + SPLASH_DURATION_MS

        splashScreen.setKeepOnScreenCondition {
            isColdStart && SystemClock.uptimeMillis() < splashDeadline
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleNdefIntent(intent)
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
        handleNdefIntent(intent)
    }

    /** Opens the confirmation screen when the system launches us from a `text/vcard` tag. */
    private fun handleNdefIntent(intent: Intent?) {
        if (intent?.action != NfcAdapter.ACTION_NDEF_DISCOVERED) return
        @Suppress("DEPRECATION")
        val messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES) ?: return
        val raw = (messages.firstOrNull() as? android.nfc.NdefMessage)?.toByteArray() ?: return
        val vCard = TxokaNdefMessage.decode(raw) ?: return
        val parsed = VCardSerializer.decode(vCard)
        if (parsed is VCardParseResult.Success) {
            incomingCards.offer(parsed.payload)
        }
    }
}
