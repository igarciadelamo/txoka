package es.igarciadelamo.txoka.nfc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

enum class NfcStatus {
    Unavailable,
    Disabled,
    Enabled,
}

interface NfcAvailability {
    fun status(): NfcStatus

    /** Emits now and every time the adapter state changes, so the hub never shows a stale state. */
    fun stateChanges(): Flow<NfcStatus>
}

class AndroidNfcAvailability @Inject constructor(@ApplicationContext private val context: Context) : NfcAvailability {
    override fun status(): NfcStatus {
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return NfcStatus.Unavailable
        return if (adapter.isEnabled) NfcStatus.Enabled else NfcStatus.Disabled
    }

    override fun stateChanges(): Flow<NfcStatus> = callbackFlow {
        // The broadcast extras are ignored on purpose: the adapter is asked again, so a broadcast
        // from any sender can only make the app read the truth.
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                trySend(status())
            }
        }
        val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        trySend(status())
        awaitClose { context.unregisterReceiver(receiver) }
    }.distinctUntilChanged()
}
