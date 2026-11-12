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

    /** Queues the card and returns false when an identical one is already waiting. */
    fun offer(payload: VCardPayload): Boolean {
        while (true) {
            val current = _queue.value
            if (payload in current) return false
            if (_queue.compareAndSet(current, current + payload)) return true
        }
    }

    /** Removes the card the user just handled, so the next one can be shown. */
    fun consume(payload: VCardPayload) {
        _queue.update { queue -> queue - payload }
    }
}
