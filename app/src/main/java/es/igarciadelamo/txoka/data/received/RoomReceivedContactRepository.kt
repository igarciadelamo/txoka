package es.igarciadelamo.txoka.data.received

import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.RgbColor
import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import es.igarciadelamo.txoka.domain.received.ContactKeys
import es.igarciadelamo.txoka.domain.received.ReceivedContact
import es.igarciadelamo.txoka.domain.received.ReceivedContactRepository
import es.igarciadelamo.txoka.domain.received.SaveResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomReceivedContactRepository @Inject constructor(private val dao: ReceivedContactDao) :
    ReceivedContactRepository {
    override val contacts: Flow<List<ReceivedContact>> = dao.observeAll().map { contacts ->
        contacts.map { contact -> contact.toDomain() }
    }

    override fun contact(id: Long): Flow<ReceivedContact?> = dao.observe(id).map { contact ->
        contact?.toDomain()
    }

    override suspend fun save(payload: VCardPayload): SaveResult {
        val phoneKey = ContactKeys.phone(payload.phone)
        val emailKey = ContactKeys.email(payload.email)

        val duplicate = dao.findDuplicate(phoneKey, emailKey)
        if (duplicate != null) return SaveResult.Duplicate(duplicate.id)

        val id = dao.insert(
            ReceivedContactEntity(
                name = payload.name,
                phone = payload.phone,
                email = payload.email,
                phoneKey = phoneKey,
                emailKey = emailKey,
                note = "",
                receivedAt = System.currentTimeMillis(),
                accentHex = payload.palette.accent.hex,
                accent2Hex = payload.palette.accent2.hex,
            ),
        )
        return SaveResult.Saved(id)
    }

    override suspend fun updateNote(id: Long, note: String) {
        dao.updateNote(id = id, note = note)
    }

    override suspend fun delete(id: Long) {
        dao.delete(id)
    }

    private fun ReceivedContactEntity.toDomain(): ReceivedContact = ReceivedContact(
        id = id,
        name = name,
        phone = phone,
        email = email,
        note = note,
        receivedAt = receivedAt,
        card = CardPalette(
            accent = RgbColor.parse(accentHex) ?: CardPalette.Default.accent,
            accent2 = RgbColor.parse(accent2Hex) ?: CardPalette.Default.accent2,
        ),
    )
}
