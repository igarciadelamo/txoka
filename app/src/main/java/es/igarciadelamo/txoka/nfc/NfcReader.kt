package es.igarciadelamo.txoka.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.os.Build
import androidx.core.content.IntentCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import es.igarciadelamo.txoka.domain.protocol.VCardParseResult
import es.igarciadelamo.txoka.domain.protocol.VCardSerializer
import javax.inject.Inject
import javax.inject.Singleton
import es.igarciadelamo.txoka.domain.protocol.NdefMessage as TxokaNdefMessage

/**
 * Reads NDEF tags while the app is in the foreground.
 *
 * Foreground dispatch is used instead of reader mode on purpose. Reader mode limits the NFC
 * controller to polling, and AOSP only enables host card emulation routing while no reader-mode
 * request is active, so the same phone could not receive a card and send its own at once. With
 * foreground dispatch the controller keeps polling and emulating, which is what two phones need.
 */
@Singleton
class NfcReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val incomingCards: IncomingCards,
) {
    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    fun start(activity: Activity) {
        val adapter = adapter ?: return
        if (!adapter.isEnabled) return
        runCatching {
            adapter.enableForegroundDispatch(activity, dispatchIntent(activity), FILTERS, null)
            CardEmulation.getInstance(adapter)?.setPreferredService(activity, hceService(activity))
        }
    }

    fun stop(activity: Activity) {
        val adapter = adapter ?: return
        runCatching {
            adapter.disableForegroundDispatch(activity)
            CardEmulation.getInstance(adapter)?.unsetPreferredService(activity)
        }
    }

    /** Opens the confirmation screen when a `text/vcard` tag arrives. */
    fun handle(intent: Intent?) {
        if (intent?.action != NfcAdapter.ACTION_NDEF_DISCOVERED) return
        val messages = IntentCompat.getParcelableArrayExtra(
            intent,
            NfcAdapter.EXTRA_NDEF_MESSAGES,
            android.nfc.NdefMessage::class.java,
        ) ?: return
        val raw = (messages.firstOrNull() as? android.nfc.NdefMessage)?.toByteArray() ?: return
        val vCard = TxokaNdefMessage.decode(raw) ?: return
        val parsed = VCardSerializer.decode(vCard)
        if (parsed is VCardParseResult.Success) {
            incomingCards.offer(parsed.payload)
        }
    }

    private fun dispatchIntent(activity: Activity): PendingIntent {
        val intent = Intent(activity, activity.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val mutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        return PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or mutable)
    }

    private fun hceService(activity: Activity): ComponentName = ComponentName(activity, NdefHceService::class.java)

    private companion object {
        val FILTERS = arrayOf(
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
                addDataType(TxokaNdefMessage.MIME_TYPE)
                addDataType("text/x-vcard")
            },
        )
    }
}
