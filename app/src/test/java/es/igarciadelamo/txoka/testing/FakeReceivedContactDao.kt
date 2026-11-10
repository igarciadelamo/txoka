package es.igarciadelamo.txoka.testing

import es.igarciadelamo.txoka.data.received.ReceivedContactDao
import es.igarciadelamo.txoka.data.received.ReceivedContactEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeReceivedContactDao : ReceivedContactDao {
    private val state = MutableStateFlow<List<ReceivedContactEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<ReceivedContactEntity>> = state

    override fun observe(id: Long): Flow<ReceivedContactEntity?> = state.map { contacts ->
        contacts.firstOrNull { contact -> contact.id == id }
    }

    override suspend fun findMatch(
        phoneKey: String,
        emailKey: String,
        linkedinKey: String,
        instagramKey: String,
    ): ReceivedContactEntity? = state.value
        .filter { contact ->
            (phoneKey.isNotEmpty() && contact.phoneKey == phoneKey) ||
                (emailKey.isNotEmpty() && contact.emailKey == emailKey) ||
                (linkedinKey.isNotEmpty() && contact.linkedin?.lowercase() == linkedinKey) ||
                (instagramKey.isNotEmpty() && contact.instagram?.lowercase() == instagramKey)
        }
        .maxByOrNull { contact -> contact.receivedAt }

    override suspend fun insert(contact: ReceivedContactEntity): Long {
        val id = nextId++
        state.update { contacts -> contacts + contact.copy(id = id) }
        return id
    }

    override suspend fun updateCard(card: ReceivedContactEntity) {
        state.update { contacts -> contacts.map { if (it.id == card.id) card else it } }
    }

    override suspend fun updateNote(id: Long, note: String) {
        state.update { contacts -> contacts.map { if (it.id == id) it.copy(note = note) else it } }
    }

    override suspend fun delete(id: Long) {
        state.update { contacts -> contacts.filterNot { it.id == id } }
    }
}
