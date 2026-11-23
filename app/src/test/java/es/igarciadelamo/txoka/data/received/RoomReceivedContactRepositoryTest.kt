package es.igarciadelamo.txoka.data.received

import es.igarciadelamo.txoka.domain.profile.CardAppearance
import es.igarciadelamo.txoka.domain.profile.CardAppearanceCodec
import es.igarciadelamo.txoka.domain.profile.CardBackground
import es.igarciadelamo.txoka.domain.profile.CardEffect
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.CardTextStyle
import es.igarciadelamo.txoka.domain.profile.RgbColor
import es.igarciadelamo.txoka.domain.profile.TextFinish
import es.igarciadelamo.txoka.domain.profile.TextSize
import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import es.igarciadelamo.txoka.domain.received.ReceivedContact
import es.igarciadelamo.txoka.domain.received.SaveResult
import es.igarciadelamo.txoka.testing.FakeReceivedContactDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomReceivedContactRepositoryTest {
    private val dao = FakeReceivedContactDao()
    private val repository = RoomReceivedContactRepository(dao)

    private val marta = VCardPayload(
        name = "Marta Ruiz",
        phone = "+34600111222",
        email = "marta@example.com",
        linkedin = "marta-ruiz",
        instagram = "marta.ruiz",
    )

    private val appearance = CardAppearance(
        background = CardBackground.HOLOGRAM,
        effect = CardEffect.FIRE,
        topRight = RgbColor(45, 67, 89),
        bottomLeft = RgbColor(180, 140, 100),
        name = CardTextStyle(finish = TextFinish.METAL, size = TextSize.L, color = RgbColor(80, 90, 100)),
        data = CardTextStyle(finish = TextFinish.RAISED, size = TextSize.S),
    )

    @Test
    fun `persists independent text styles effects and four corner colours`() = runTest {
        val card = marta.copy(palette = CardPalette.Default.copy(appearance = appearance))

        val saved = repository.save(card) as SaveResult.Saved

        assertEquals(card.palette, repository.contact(saved.id).first()?.card)
        assertEquals(CardAppearanceCodec.encode(appearance), dao.observe(saved.id).first()?.appearance)
    }

    @Test
    fun `refreshing appearance keeps local note and replaces the previous style`() = runTest {
        val first = repository.save(marta) as SaveResult.Saved
        repository.updateNote(first.id, "Meetup")
        val styled = marta.copy(palette = marta.palette.copy(appearance = appearance))

        assertEquals(SaveResult.Updated(first.id), repository.save(styled))
        assertEquals(appearance, repository.contact(first.id).first()?.card?.appearance)
        assertEquals("Meetup", repository.contact(first.id).first()?.note)

        repository.save(marta)

        assertNull(repository.contact(first.id).first()?.card?.appearance)
        assertNull(dao.observe(first.id).first()?.appearance)
        assertEquals("Meetup", repository.contact(first.id).first()?.note)
    }

    @Test
    fun `restoring a deleted library retains its appearance palette note and timestamp`() = runTest {
        val styled = marta.copy(palette = marta.palette.copy(appearance = appearance))
        val first = repository.save(styled) as SaveResult.Saved
        repository.updateNote(first.id, "Meetup")

        val removed = repository.deleteAll()
        repository.restore(removed)

        val restored = repository.contacts.first().single()
        assertEquals(first.id, restored.id)
        assertEquals(removed.single().card, restored.card)
        assertEquals(removed.single().receivedAt, restored.receivedAt)
        assertEquals("Meetup", restored.note)
        assertEquals("marta.ruiz", restored.instagram)
    }

    @Test
    fun `restore preserves original ids alongside cards received while undo was pending`() = runTest {
        val first = repository.save(marta) as SaveResult.Saved
        repository.updateNote(first.id, "Meetup")
        val removed = repository.deleteAll()
        val incoming = repository.save(VCardPayload(name = "Ana", email = "ana@example.com")) as SaveResult.Saved

        repository.restore(removed)
        val later = repository.save(VCardPayload(name = "Luis", email = "luis@example.com")) as SaveResult.Saved

        assertEquals(removed.single(), repository.contact(first.id).first())
        assertEquals("Ana", repository.contact(incoming.id).first()?.name)
        assertTrue(later.id > incoming.id)
        assertEquals(3, repository.contacts.first().map { it.id }.toSet().size)
    }

    @Test
    fun `failed bulk restore leaves all rows unchanged instead of restoring only a prefix`() = runTest {
        val saved = repository.save(marta) as SaveResult.Saved
        val existing = requireNotNull(repository.contact(saved.id).first())
        val newContact = ReceivedContact(id = 73, name = "Ana", note = "Private meeting")

        try {
            repository.restore(listOf(newContact, existing))
            throw AssertionError("Expected duplicate id to abort the complete restore")
        } catch (_: IllegalStateException) {
            assertEquals(listOf(existing), repository.contacts.first())
            assertNull(repository.contact(newContact.id).first())
        }
    }

    @Test
    fun `clearing delegates to the transactional snapshot rather than a separately observed list`() = runTest {
        val saved = repository.save(marta) as SaveResult.Saved
        val expected = requireNotNull(repository.contact(saved.id).first())
        val transactionalOnlyDao = object : ReceivedContactDao by dao {
            override fun observeAll(): Flow<List<ReceivedContactEntity>> = flow {
                error("Must use atomic read and delete")
            }
        }
        val transactional = RoomReceivedContactRepository(transactionalOnlyDao)

        assertEquals(listOf(expected), transactional.deleteAll())
        assertTrue(repository.contacts.first().isEmpty())
    }

    @Test
    fun `malformed stored appearance falls back to the stored legacy palette`() = runTest {
        val styled = marta.copy(palette = marta.palette.copy(appearance = appearance))
        val saved = repository.save(styled) as SaveResult.Saved
        val entity = requireNotNull(dao.observe(saved.id).first())
        dao.updateCard(entity.copy(appearance = "999:not-a-style"))

        assertEquals(marta.palette, repository.contact(saved.id).first()?.card)
        assertEquals("Marta Ruiz", repository.contact(saved.id).first()?.name)
        assertEquals("+34600111222", repository.contact(saved.id).first()?.phone)
    }

    @Test
    fun `saves a card and reads it back`() = runTest {
        val result = repository.save(marta)

        assertTrue(result is SaveResult.Saved)
        val saved = repository.contacts.first().single()
        assertEquals("Marta Ruiz", saved.name)
        assertEquals("+34600111222", saved.phone)
        assertEquals("marta@example.com", saved.email)
        assertEquals("marta-ruiz", saved.linkedin)
        assertEquals("marta.ruiz", saved.instagram)
    }

    @Test
    fun `refreshes the card when the phone matches`() = runTest {
        val first = repository.save(marta) as SaveResult.Saved

        val result = repository.save(VCardPayload(name = "Marta Ruiz Gómez", phone = "+34600111222"))

        assertEquals(SaveResult.Updated(first.id), result)
        val saved = repository.contacts.first().single()
        assertEquals("Marta Ruiz Gómez", saved.name)
        assertNull(saved.email)
        assertNull(saved.instagram)
    }

    @Test
    fun `refreshes the card when the email matches, ignoring case`() = runTest {
        repository.save(marta)

        val result = repository.save(VCardPayload(name = "Otra Marta", email = "MARTA@example.com"))

        assertTrue(result is SaveResult.Updated)
        assertEquals(1, repository.contacts.first().size)
        assertEquals("Otra Marta", repository.contacts.first().single().name)
    }

    @Test
    fun `refreshes the card when a social handle matches`() = runTest {
        repository.save(VCardPayload(name = "Ana", instagram = "ana.ruiz"))

        val result = repository.save(VCardPayload(name = "Ana Ruiz", instagram = "@Ana.Ruiz"))

        assertTrue(result is SaveResult.Updated)
        assertEquals(1, repository.contacts.first().size)
    }

    @Test
    fun `keeps the note and bumps the date when a card is refreshed`() = runTest {
        val first = repository.save(marta) as SaveResult.Saved
        repository.updateNote(first.id, "Meetup")
        val receivedAt = repository.contact(first.id).first()?.receivedAt

        repository.save(marta.copy(name = "Marta"))

        val saved = repository.contacts.first().single()
        assertEquals("Meetup", saved.note)
        assertTrue(saved.receivedAt >= (receivedAt ?: 0L))
    }

    @Test
    fun `match reports the stored contact`() = runTest {
        val first = repository.save(marta) as SaveResult.Saved

        assertEquals(first.id, repository.match(marta.copy(name = "Marta"))?.id)
        assertNull(repository.match(VCardPayload(name = "Marta", phone = "+34600999888")))
    }

    @Test
    fun `stores contacts without phone, email or handles`() = runTest {
        repository.save(VCardPayload(name = "Ana"))
        repository.save(VCardPayload(name = "Luis"))

        assertEquals(2, repository.contacts.first().size)
    }

    @Test
    fun `updates notes and deletes`() = runTest {
        val saved = repository.save(marta) as SaveResult.Saved

        repository.updateNote(saved.id, "Meetup")
        assertEquals("Meetup", repository.contact(saved.id).first()?.note)

        repository.delete(saved.id)
        assertNull(repository.contact(saved.id).first())
    }

    @Test
    fun `keeps the card apart when the keys point at different contacts`() = runTest {
        repository.save(VCardPayload(name = "Marta por teléfono", phone = "+34600111222"))
        repository.save(VCardPayload(name = "Marta por email", email = "marta@example.com"))

        val result = repository.save(marta)

        assertTrue(result is SaveResult.Saved)
        assertEquals(3, repository.contacts.first().size)
        assertNull(repository.match(marta))
    }

    @Test
    fun `empties the library and restores it on undo`() = runTest {
        val first = repository.save(marta) as SaveResult.Saved
        repository.updateNote(first.id, "Meetup")
        repository.save(VCardPayload(name = "Ana", instagram = "ana.ruiz"))

        val removed = repository.deleteAll()

        assertTrue(repository.contacts.first().isEmpty())
        assertEquals(2, removed.size)

        repository.restore(removed)

        val restored = repository.contacts.first()
        assertEquals(removed.map { it.id }.toSet(), restored.map { it.id }.toSet())
        assertEquals(setOf("Marta Ruiz", "Ana"), restored.map { it.name }.toSet())
        assertEquals("Meetup", restored.first { it.name == "Marta Ruiz" }.note)
    }
}
