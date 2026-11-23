package es.igarciadelamo.txoka.testing

import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import es.igarciadelamo.txoka.domain.received.ReceivedContact
import es.igarciadelamo.txoka.domain.received.SaveResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeReceivedContactRepositoryTest {
    @Test
    fun `undo restores exact ids notes palette and dates without reusing ids for new cards`() = runTest {
        val contact = ReceivedContact(
            id = 73,
            name = "Marta",
            note = "Private meeting",
            receivedAt = 1234,
            card = CardPalette(CardPalette.Default.accent2, CardPalette.Default.accent),
        )
        val repository = FakeReceivedContactRepository(listOf(contact))
        val removed = repository.deleteAll()
        val incoming = repository.save(VCardPayload(name = "Ana")) as SaveResult.Saved

        repository.restore(removed)
        val later = repository.save(VCardPayload(name = "Luis")) as SaveResult.Saved

        assertEquals(contact, repository.contact(contact.id).first())
        assertTrue(incoming.id > contact.id)
        assertTrue(later.id > incoming.id)
        assertEquals(3, repository.contacts.first().map { it.id }.toSet().size)
    }

    @Test
    fun `duplicate id aborts a restored batch without adding earlier cards`() = runTest {
        val existing = ReceivedContact(id = 1, name = "Marta")
        val repository = FakeReceivedContactRepository(listOf(existing))

        try {
            repository.restore(listOf(ReceivedContact(id = 73, name = "Ana"), existing))
            throw AssertionError("Expected duplicate id to reject the complete restore")
        } catch (_: IllegalStateException) {
            assertEquals(listOf(existing), repository.contacts.first())
        }
    }

    @Test
    fun `restoring an external snapshot advances the generated id sequence`() = runTest {
        val repository = FakeReceivedContactRepository()
        repository.restore(listOf(ReceivedContact(id = 73, name = "Marta")))

        val saved = repository.save(VCardPayload(name = "Ana")) as SaveResult.Saved

        assertEquals(74L, saved.id)
        assertEquals("Marta", repository.contact(73).first()?.name)
    }
}
