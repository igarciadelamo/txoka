package es.igarciadelamo.txoka.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.nfc.tech.Ndef
import android.os.Build
import androidx.core.content.IntentCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import es.igarciadelamo.txoka.di.ApplicationScope
import es.igarciadelamo.txoka.domain.profile.ShareRole
import es.igarciadelamo.txoka.domain.protocol.VCardParseResult
import es.igarciadelamo.txoka.domain.protocol.VCardSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import es.igarciadelamo.txoka.domain.protocol.NdefMessage as TxokaNdefMessage

/**
 * Reads cards while the app is on screen and keeps host card emulation claimed while sending.
 *
 * The two modes are exclusive because reader mode turns host card emulation off: on Receive the app
 * reads tags directly through [NfcAdapter.enableReaderMode], which bypasses the tag intent dispatch
 * entirely (some vendor builds never hand NDEF intents to third-party apps). On Send it keeps
 * foreground dispatch and the preferred HCE service, so the phone can still be read and read back.
 */
@Singleton
class NfcReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val incomingCards: IncomingCards,
    private val roles: ShareRoleController,
    private val diagnostics: NfcDiagnostics,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)
    private var resumed: Activity? = null

    private val readerCallback = NfcAdapter.ReaderCallback { tag ->
        diagnostics.record("tag: ${tag.techList.joinToString("/") { it.substringAfterLast('.') }}")
        val ndef = Ndef.get(tag)
        if (ndef == null) {
            diagnostics.record("not ndef")
            return@ReaderCallback
        }
        runCatching {
            ndef.connect()
            ndef.ndefMessage?.let { message -> offer(message.toByteArray()) }
        }.onFailure { error -> diagnostics.record("read failed: ${error.javaClass.simpleName}") }
        runCatching { ndef.close() }
    }

    init {
        scope.launch {
            roles.state.map { it.role }.distinctUntilChanged().collect {
                withContext(Dispatchers.Main) { applyMode() }
            }
        }
    }

    fun start(activity: Activity) {
        resumed = activity
        applyMode()
    }

    fun stop(activity: Activity) {
        if (resumed === activity) resumed = null
        val adapter = adapter ?: return
        runCatching {
            adapter.disableReaderMode(activity)
            adapter.disableForegroundDispatch(activity)
            CardEmulation.getInstance(adapter)?.unsetPreferredService(activity)
        }
    }

    /** Opens the confirmation screen when a `text/vcard` tag arrives as an intent. */
    fun handle(intent: Intent?) {
        diagnostics.record("${intent?.action ?: "none"} · ${intent?.type ?: "-"}")
        if (intent?.action != NfcAdapter.ACTION_NDEF_DISCOVERED) return
        val messages = IntentCompat.getParcelableArrayExtra(
            intent,
            NfcAdapter.EXTRA_NDEF_MESSAGES,
            android.nfc.NdefMessage::class.java,
        ) ?: return
        val raw = (messages.firstOrNull() as? android.nfc.NdefMessage)?.toByteArray() ?: return
        offer(raw)
    }

    private fun applyMode() {
        val adapter = adapter ?: return
        val activity = resumed ?: return
        if (!adapter.isEnabled) return
        val receiving = roles.state.value.role == ShareRole.Receive
        runCatching {
            if (receiving) {
                adapter.disableForegroundDispatch(activity)
                CardEmulation.getInstance(adapter)?.unsetPreferredService(activity)
                adapter.enableReaderMode(activity, readerCallback, READER_FLAGS, null)
            } else {
                adapter.disableReaderMode(activity)
                adapter.enableForegroundDispatch(activity, dispatchIntent(activity), FILTERS, null)
                CardEmulation.getInstance(adapter)?.setPreferredService(activity, hceService(activity))
            }
        }
    }

    private fun offer(raw: ByteArray) {
        val vCard = TxokaNdefMessage.decode(raw) ?: return
        val parsed = VCardSerializer.decode(vCard)
        if (parsed is VCardParseResult.Success) {
            diagnostics.record("card: ${parsed.payload.name}")
            incomingCards.offer(parsed.payload)
            roles.onCardReceived()
        } else {
            diagnostics.record("decode failed")
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

        const val READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V
    }
}
