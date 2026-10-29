package es.igarciadelamo.txoka.nfc

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import dagger.hilt.android.qualifiers.ApplicationContext
import es.igarciadelamo.txoka.domain.protocol.VCardParseResult
import es.igarciadelamo.txoka.domain.protocol.VCardSerializer
import javax.inject.Inject
import javax.inject.Singleton
import es.igarciadelamo.txoka.domain.protocol.NdefMessage as TxokaNdefMessage

/**
 * Reads NDEF tags while the app is in the foreground. The card is parsed with the same pure code
 * used by the tests and pushed to [IncomingCards] so the UI can ask for confirmation.
 */
@Singleton
class NfcReader @Inject constructor(@ApplicationContext context: Context, private val incomingCards: IncomingCards) {
    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    fun start(activity: Activity) {
        val adapter = adapter ?: return
        adapter.enableReaderMode(activity, ::onTagDiscovered, READER_FLAGS, null)
    }

    fun stop(activity: Activity) {
        runCatching { adapter?.disableReaderMode(activity) }
    }

    private fun onTagDiscovered(tag: Tag) {
        val ndef = Ndef.get(tag) ?: return
        val vCard = runCatching {
            ndef.connect()
            ndef.ndefMessage?.let { message -> TxokaNdefMessage.decode(message.toByteArray()) }
        }.getOrNull()
        runCatching { ndef.close() }

        val parsed = vCard?.let(VCardSerializer::decode) ?: return
        if (parsed is VCardParseResult.Success) {
            incomingCards.offer(parsed.payload)
        }
    }

    private companion object {
        const val READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V or
            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
    }
}
