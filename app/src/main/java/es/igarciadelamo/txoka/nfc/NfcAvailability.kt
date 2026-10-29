package es.igarciadelamo.txoka.nfc

import android.content.Context
import android.nfc.NfcAdapter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

enum class NfcStatus {
    Unavailable,
    Disabled,
    Enabled,
}

interface NfcAvailability {
    fun status(): NfcStatus
}

class AndroidNfcAvailability @Inject constructor(@ApplicationContext private val context: Context) : NfcAvailability {
    override fun status(): NfcStatus {
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return NfcStatus.Unavailable
        return if (adapter.isEnabled) NfcStatus.Enabled else NfcStatus.Disabled
    }
}
