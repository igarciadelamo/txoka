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
    )

    @Test
    fun `saves a card and reads it back`() = runTest {
        val result = repository.save(marta)

        assertTrue(result is SaveResult.Saved)
        val saved = repository.contacts.first().single()
        assertEquals("Marta Ruiz", saved.name)
        assertEquals("+34600111222", saved.phone)
        assertEquals("marta@example.com", saved.email)
    }

    @Test
    fun `rejects a card with the same phone`() = runTest {
        val first = repository.save(marta) as SaveResult.Saved

        val second = repository.save(marta.copy(name = "Marta", email = null))

        assertEquals(SaveResult.Duplicate(first.id), second)
        assertEquals(1, repository.contacts.first().size)
    }

    @Test
    fun `rejects a card with the same email`() = runTest {
        repository.save(marta)

        val second = repository.save(VCardPayload(name = "Otra Marta", email = "MARTA@example.com"))

        assertTrue(second is SaveResult.Duplicate)
        assertEquals(1, repository.contacts.first().size)
    }

    @Test
    fun `stores contacts without phone or email`() = runTest {
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
}
