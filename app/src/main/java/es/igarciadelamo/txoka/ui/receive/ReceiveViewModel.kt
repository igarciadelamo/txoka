package es.igarciadelamo.txoka.ui.receive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import es.igarciadelamo.txoka.domain.received.ReceivedContactRepository
import es.igarciadelamo.txoka.domain.received.SaveResult
import es.igarciadelamo.txoka.nfc.IncomingCards
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the user is told once a card is stored. */
enum class SaveOutcome { Saved, Updated }

@HiltViewModel
class ReceiveViewModel @Inject constructor(
    private val incomingCards: IncomingCards,
    private val repository: ReceivedContactRepository,
) : ViewModel() {
    val pending: StateFlow<VCardPayload?> = incomingCards.pending

    private val _alreadyStored = MutableStateFlow(false)
    val alreadyStored: StateFlow<Boolean> = _alreadyStored.asStateFlow()

    private val _outcome = MutableSharedFlow<SaveOutcome>(extraBufferCapacity = 1)
    val outcome: SharedFlow<SaveOutcome> = _outcome.asSharedFlow()

    init {
        viewModelScope.launch {
            incomingCards.pending.collect { payload ->
                _alreadyStored.value = payload?.let { repository.match(it) != null } ?: false
            }
        }
    }

    fun save() {
        val payload = pending.value ?: return
        viewModelScope.launch {
            _outcome.tryEmit(
                when (repository.save(payload)) {
                    is SaveResult.Saved -> SaveOutcome.Saved
                    is SaveResult.Updated -> SaveOutcome.Updated
                },
            )
            incomingCards.consume()
        }
    }

    fun discard() {
        incomingCards.consume()
    }
}
