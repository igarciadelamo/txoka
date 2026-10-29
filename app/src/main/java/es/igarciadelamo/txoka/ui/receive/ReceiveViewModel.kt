package es.igarciadelamo.txoka.ui.receive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import es.igarciadelamo.txoka.domain.received.ReceivedContactRepository
import es.igarciadelamo.txoka.nfc.IncomingCards
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiveViewModel @Inject constructor(
    private val incomingCards: IncomingCards,
    private val repository: ReceivedContactRepository,
) : ViewModel() {
    val pending: StateFlow<VCardPayload?> = incomingCards.pending

    fun save() {
        val payload = pending.value ?: return
        viewModelScope.launch {
            repository.save(payload)
            incomingCards.consume()
        }
    }

    fun discard() {
        incomingCards.consume()
    }
}
