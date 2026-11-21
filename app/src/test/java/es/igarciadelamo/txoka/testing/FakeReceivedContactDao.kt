package es.igarciadelamo.txoka.testing

import es.igarciadelamo.txoka.data.received.ReceivedContactDao
import es.igarciadelamo.txoka.data.received.ReceivedContactEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FakeReceivedContactDao : ReceivedContactDao {
    private val state = MutableStateFlow<List<ReceivedContactEntity>>(emptyList())
    private var nextId = 1L
    private val writeMutex = Mutex()

    override fun observeAll(): Flow<List<ReceivedContactEntity>> = state

    override suspend fun getAll(): List<ReceivedContactEntity> = state.value.sortedByDescending { it.receivedAt }

    override fun observe(id: Long): Flow<ReceivedContactEntity?> = state.map { contacts ->
        contacts.firstOrNull { contact -> contact.id == id }
    }

    override suspend fun findMatches(
        phoneKey: String,
        emailKey: String,
        linkedinKey: String,
        instagramKey: String,
    ): List<ReceivedContactEntity> = state.value
        .filter { contact ->
            (phoneKey.isNotEmpty() && contact.phoneKey == phoneKey) ||
                (emailKey.isNotEmpty() && contact.emailKey == emailKey) ||
                (linkedinKey.isNotEmpty() && contact.linkedin?.lowercase() == linkedinKey) ||
                (instagramKey.isNotEmpty() && contact.instagram?.lowercase() == instagramKey)
        }
        .sortedByDescending { contact -> contact.receivedAt }

    override suspend fun insert(contact: ReceivedContactEntity): Long = writeMutex.withLock {
        val id = if (contact.id == 0L) nextId else contact.id
        check(state.value.none { it.id == id })
        nextId = maxOf(nextId, id + 1)
        state.value += contact.copy(id = id)
        id
    }

    override suspend fun insertAll(contacts: List<ReceivedContactEntity>) {
        writeMutex.withLock {
            var allocatedId = nextId
            val rows = contacts.map { contact ->
                val id = if (contact.id == 0L) allocatedId else contact.id
                allocatedId = maxOf(allocatedId, id + 1)
                contact.copy(id = id)
            }
            check(rows.map { it.id }.distinct().size == rows.size)
            check(rows.none { row -> state.value.any { it.id == row.id } })
            state.value += rows
            nextId = allocatedId
        }
    }

    override suspend fun updateCard(card: ReceivedContactEntity) {
        writeMutex.withLock {
            state.update { contacts -> contacts.map { if (it.id == card.id) card else it } }
        }
    }

    override suspend fun updateNote(id: Long, note: String) {
        writeMutex.withLock {
            state.update { contacts -> contacts.map { if (it.id == id) it.copy(note = note) else it } }
        }
    }

    override suspend fun delete(id: Long) {
        writeMutex.withLock { state.update { contacts -> contacts.filterNot { it.id == id } } }
    }

    override suspend fun deleteAll() {
        writeMutex.withLock { state.value = emptyList() }
    }

    override suspend fun deleteAllReturning(): List<ReceivedContactEntity> = writeMutex.withLock {
        state.value.sortedByDescending { it.receivedAt }.also { state.value = emptyList() }
    }
}
