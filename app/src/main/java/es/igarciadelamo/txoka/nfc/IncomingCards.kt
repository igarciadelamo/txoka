package es.igarciadelamo.txoka.nfc

import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The cards waiting for the user to accept or discard them, shared by the reader and the launcher
 * intent so both paths end on the same confirmation screen.
 *
 * Cards are kept in arrival order: a second card offered while the confirmation screen is open
 * waits behind the first one instead of replacing it. An identical card already waiting is ignored,
 * so a repeated tap on the tag does not stack the same sender twice.
 */
@Singleton
class IncomingCards @Inject constructor() {
    private val _queue = MutableStateFlow<List<VCardPayload>>(emptyList())

    /** Cards waiting for the user, oldest first. */
    val queue: StateFlow<List<VCardPayload>> = _queue.asStateFlow()

    val pending: VCardPayload? get() = queue.value.firstOrNull()

    fun offer(payload: VCardPayload) {
        _queue.update { queue -> if (payload in queue) queue else queue + payload }
    }

    /** Removes the card the user just handled, so the next one can be shown. */
    fun consume(payload: VCardPayload) {
        _queue.update { queue -> queue - payload }
    }
}
