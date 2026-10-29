package es.igarciadelamo.txoka.nfc

import es.igarciadelamo.txoka.domain.protocol.ContactTransport
import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NFC implementation of [ContactTransport]. Publishing goes through the HCE publisher, reading is
 * fed by the foreground reader.
 */
@Singleton
class NdefHceTransport @Inject constructor(
    private val publisher: HceCardPublisher,
    private val incomingCards: IncomingCards,
) : ContactTransport {
    override suspend fun publish(payload: VCardPayload?) {
        publisher.publish(payload)
    }

    override val received: Flow<VCardPayload> = kotlinx.coroutines.flow.flow {
        incomingCards.pending.collect { payload -> if (payload != null) emit(payload) }
    }
}
