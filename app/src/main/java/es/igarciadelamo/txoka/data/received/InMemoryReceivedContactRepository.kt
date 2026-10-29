package es.igarciadelamo.txoka.data.received

import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import es.igarciadelamo.txoka.domain.received.ReceivedContactRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Temporary storage until Room lands. Kept in memory so the receive flow can be exercised end to
 * end without persisting anything yet.
 */
@Singleton
class InMemoryReceivedContactRepository @Inject constructor() : ReceivedContactRepository {
    private val _contacts = MutableStateFlow<List<VCardPayload>>(emptyList())
    val contacts: StateFlow<List<VCardPayload>> = _contacts.asStateFlow()

    override suspend fun save(payload: VCardPayload) {
        _contacts.update { current -> current + payload }
    }
}
