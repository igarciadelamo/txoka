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

    override suspend fun match(payload: VCardPayload): ReceivedContact? = findMatch(payload)?.toDomain()

    override suspend fun save(payload: VCardPayload): SaveResult {
        val card = payload.toEntity()
        val existing = findMatch(payload)
            ?: return SaveResult.Saved(dao.insert(card))

        // Sharing again refreshes the card, not the local note.
        dao.updateCard(card.copy(id = existing.id, note = existing.note))
        return SaveResult.Updated(existing.id)
    }

    override suspend fun updateNote(id: Long, note: String) {
        dao.updateNote(id = id, note = note)
    }

    override suspend fun delete(id: Long) {
        dao.delete(id)
    }

    private suspend fun findMatch(payload: VCardPayload): ReceivedContactEntity? = dao.findMatch(
        phoneKey = ContactKeys.phone(payload.phone),
        emailKey = ContactKeys.email(payload.email),
        linkedinKey = ContactKeys.social(payload.linkedin),
        instagramKey = ContactKeys.social(payload.instagram),
    )

    private fun VCardPayload.toEntity(): ReceivedContactEntity = ReceivedContactEntity(
        name = name,
        phone = phone,
        email = email,
        linkedin = linkedin,
        instagram = instagram,
        phoneKey = ContactKeys.phone(phone),
        emailKey = ContactKeys.email(email),
        note = "",
        receivedAt = System.currentTimeMillis(),
        accentHex = palette.accent.hex,
        accent2Hex = palette.accent2.hex,
    )

    private fun ReceivedContactEntity.toDomain(): ReceivedContact = ReceivedContact(
        id = id,
        name = name,
        phone = phone,
        email = email,
        linkedin = linkedin,
        instagram = instagram,
        note = note,
        receivedAt = receivedAt,
        card = CardPalette(
            accent = RgbColor.parse(accentHex) ?: CardPalette.Default.accent,
            accent2 = RgbColor.parse(accent2Hex) ?: CardPalette.Default.accent2,
        ),
    )
}
