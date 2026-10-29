package es.igarciadelamo.txoka.domain.received

import es.igarciadelamo.txoka.domain.protocol.VCardPayload

/**
 * Storage for the cards the user accepted. Room replaces the in-memory implementation in the
 * next milestone; the interface keeps the UI independent from that change.
 */
interface ReceivedContactRepository {
    suspend fun save(payload: VCardPayload)
}
