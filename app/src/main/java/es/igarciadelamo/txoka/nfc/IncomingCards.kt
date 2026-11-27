package es.igarciadelamo.txoka.nfc

import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
 * so a repeated tap on the tag does not stack the same sender twice. Once [MAX_PENDING] cards are
 * waiting, new ones are ignored; the sender can tap again after the queue drains.
 */
@Singleton
class IncomingCards @Inject constructor() {
    private val _queue = MutableStateFlow<List<VCardPayload>>(emptyList())
    private val _invalid = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Cards waiting for the user, oldest first. */
    val queue: StateFlow<List<VCardPayload>> = _queue.asStateFlow()

    /** Emits when a tag or message could not be read as a card. */
    val invalid: SharedFlow<Unit> = _invalid.asSharedFlow()

    val pending: VCardPayload? get() = queue.value.firstOrNull()

    /** Queues the card and returns false when it is a duplicate or the queue is full. */
    fun offer(payload: VCardPayload): Boolean {
        while (true) {
            val current = _queue.value
            if (current.size >= MAX_PENDING || payload in current) return false
            if (_queue.compareAndSet(current, current + payload)) return true
        }
    }

    fun reportInvalid() {
        _invalid.tryEmit(Unit)
    }

    /** Removes the card the user just handled, so the next one can be shown. */
    fun consume(payload: VCardPayload) {
        _queue.update { queue -> queue - payload }
    }

    internal companion object {
        const val MAX_PENDING = 5
    }
}
