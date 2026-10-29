package es.igarciadelamo.txoka.testing

import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import es.igarciadelamo.txoka.domain.received.ReceivedContactRepository

class FakeReceivedContactRepository : ReceivedContactRepository {
    var saved: VCardPayload? = null
        private set

    override suspend fun save(payload: VCardPayload) {
        saved = payload
    }
}
