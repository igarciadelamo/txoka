package es.igarciadelamo.txoka.ui.receive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import es.igarciadelamo.txoka.domain.received.ReceivedContactRepository
import es.igarciadelamo.txoka.domain.received.SaveResult
import es.igarciadelamo.txoka.nfc.IncomingCards
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the user is told once a card is stored. */
enum class SaveOutcome { Saved, Updated }

/** The card on screen, whether the library already has it, and how many cards wait behind it. */
data class PendingCard(val payload: VCardPayload, val alreadyStored: Boolean, val waiting: Int)

@HiltViewModel
class ReceiveViewModel @Inject constructor(
    private val incomingCards: IncomingCards,
    private val repository: ReceivedContactRepository,
) : ViewModel() {
    /**
     * The head of the queue, looked up in the library as a whole: the flag travels with the card so
     * the screen never shows the verdict for the previous one.
     */
    val pending: StateFlow<PendingCard?> = incomingCards.queue
        .map { queue ->
            queue.firstOrNull()?.let { payload ->
                PendingCard(
                    payload = payload,
                    alreadyStored = repository.match(payload) != null,
                    waiting = queue.size - 1,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _outcome = MutableSharedFlow<SaveOutcome>(extraBufferCapacity = 1)
    val outcome: SharedFlow<SaveOutcome> = _outcome.asSharedFlow()

    fun save(payload: VCardPayload) {
        if (payload !in incomingCards.queue.value) return
        viewModelScope.launch {
            _outcome.tryEmit(
                when (repository.save(payload)) {
                    is SaveResult.Saved -> SaveOutcome.Saved
                    is SaveResult.Updated -> SaveOutcome.Updated
                },
            )
            incomingCards.consume(payload)
        }
    }

    fun discard(payload: VCardPayload) {
        incomingCards.consume(payload)
    }
}
