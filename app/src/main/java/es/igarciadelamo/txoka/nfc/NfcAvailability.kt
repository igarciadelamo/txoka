package es.igarciadelamo.txoka.nfc

import android.content.Context
import android.nfc.NfcAdapter
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

enum class NfcStatus {
    Unavailable,
    Disabled,
    Enabled,
}

interface NfcAvailability {
    fun status(): NfcStatus

    /** False when the platform stopped handing NFC tag intents to the app. */
    fun tagIntentsAllowed(): Boolean
}

class AndroidNfcAvailability @Inject constructor(@ApplicationContext private val context: Context) : NfcAvailability {
    override fun status(): NfcStatus {
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return NfcStatus.Unavailable
        return if (adapter.isEnabled) NfcStatus.Enabled else NfcStatus.Disabled
    }

    override fun tagIntentsAllowed(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return true
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return true
        // Some vendor builds throw from isTagIntentAllowed (vivo asks for WRITE_SECURE_SETTINGS).
        return runCatching { adapter.isTagIntentAllowed }.getOrDefault(true)
    }
}
