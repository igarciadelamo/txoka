package es.igarciadelamo.txoka.nfc

import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The card waiting for the user to accept or discard it, shared by the reader and the launcher
 * intent so both paths end on the same confirmation screen.
 */
@Singleton
class IncomingCards @Inject constructor() {
    private val _pending = MutableStateFlow<VCardPayload?>(null)
    val pending: StateFlow<VCardPayload?> = _pending.asStateFlow()

    fun offer(payload: VCardPayload) {
        _pending.value = payload
    }

    fun consume() {
        _pending.value = null
    }
}
