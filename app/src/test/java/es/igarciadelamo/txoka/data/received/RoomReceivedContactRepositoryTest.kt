package es.igarciadelamo.txoka.data.received

import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import es.igarciadelamo.txoka.domain.received.SaveResult
import es.igarciadelamo.txoka.testing.FakeReceivedContactDao
import kotlinx.coroutines.flow.first
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
        assertEquals(setOf("Marta Ruiz", "Ana"), restored.map { it.name }.toSet())
        assertEquals("Meetup", restored.first { it.name == "Marta Ruiz" }.note)
    }
}
