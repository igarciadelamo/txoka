package es.igarciadelamo.txoka.testing

import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import es.igarciadelamo.txoka.domain.received.ContactKeys
import es.igarciadelamo.txoka.domain.received.ReceivedContact
import es.igarciadelamo.txoka.domain.received.ReceivedContactRepository
import es.igarciadelamo.txoka.domain.received.SaveResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeReceivedContactRepository(initial: List<ReceivedContact> = emptyList()) : ReceivedContactRepository {
    private val state = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    var saved: VCardPayload? = null
        private set

    override val contacts: Flow<List<ReceivedContact>> = state

    override fun contact(id: Long): Flow<ReceivedContact?> = state.map { contacts ->
        contacts.firstOrNull { it.id == id }
    }

    override suspend fun match(payload: VCardPayload): ReceivedContact? =
        state.value.firstOrNull { contact -> contact.matches(payload) }

    override suspend fun save(payload: VCardPayload): SaveResult {
        saved = payload
        val existing = match(payload)
        if (existing != null) {
            state.update { contacts ->
                contacts.map { contact ->
                    if (contact.id == existing.id) contact.from(payload) else contact
                }
            }
            return SaveResult.Updated(existing.id)
        }

        val id = nextId++
        state.update { contacts ->
            contacts + ReceivedContact(
                id = id,
                name = payload.name,
                phone = payload.phone,
                email = payload.email,
                linkedin = payload.linkedin,
                instagram = payload.instagram,
                card = payload.palette,
            )
        }
        return SaveResult.Saved(id)
    }

    override suspend fun updateNote(id: Long, note: String) {
        state.update { contacts -> contacts.map { if (it.id == id) it.copy(note = note) else it } }
    }

    override suspend fun delete(id: Long) {
        state.update { contacts -> contacts.filterNot { it.id == id } }
    }

    private fun ReceivedContact.matches(payload: VCardPayload): Boolean {
        val phoneKey = ContactKeys.phone(payload.phone)
        val emailKey = ContactKeys.email(payload.email)
        val linkedinKey = ContactKeys.social(payload.linkedin)
        val instagramKey = ContactKeys.social(payload.instagram)
        return (phoneKey.isNotEmpty() && ContactKeys.phone(phone) == phoneKey) ||
            (emailKey.isNotEmpty() && ContactKeys.email(email) == emailKey) ||
            (linkedinKey.isNotEmpty() && ContactKeys.social(linkedin) == linkedinKey) ||
            (instagramKey.isNotEmpty() && ContactKeys.social(instagram) == instagramKey)
    }

    private fun ReceivedContact.from(payload: VCardPayload): ReceivedContact = copy(
        name = payload.name,
        phone = payload.phone,
        email = payload.email,
        linkedin = payload.linkedin,
        instagram = payload.instagram,
        receivedAt = System.currentTimeMillis(),
        card = payload.palette,
    )
}
