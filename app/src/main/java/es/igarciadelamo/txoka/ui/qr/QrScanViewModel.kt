package es.igarciadelamo.txoka.ui.qr

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import es.igarciadelamo.txoka.nfc.IncomingCards
import javax.inject.Inject

@HiltViewModel
class QrScanViewModel @Inject constructor(private val incomingCards: IncomingCards) : ViewModel() {
    fun offer(payload: VCardPayload) {
        incomingCards.offer(payload)
    }
}
