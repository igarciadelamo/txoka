package es.igarciadelamo.txoka.domain.protocol

import kotlinx.coroutines.flow.Flow

/**
 * How a card leaves and enters the device. Implementations live outside domain, so an APDU or
 * Bluetooth transport can be added without touching the UI or the protocol.
 */
interface ContactTransport {
    /** Publishes [payload] for other devices, or stops sharing when it is null. */
    suspend fun publish(payload: VCardPayload?)

    /** Cards read from nearby devices while listening. */
    val received: Flow<VCardPayload>
}
