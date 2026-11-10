package es.igarciadelamo.txoka.ui.receive

import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import es.igarciadelamo.txoka.domain.received.ReceivedContact
import es.igarciadelamo.txoka.nfc.IncomingCards
import es.igarciadelamo.txoka.testing.FakeReceivedContactRepository
import es.igarciadelamo.txoka.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReceiveViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val incomingCards = IncomingCards()
    private val repository = FakeReceivedContactRepository()
    private val payload = VCardPayload(name = "Marta Ruiz", phone = "+34600111222")

    private val stored = ReceivedContact(id = 1, name = "Marta Ruiz", phone = "+34600111222")

    @Test
    fun `exposes the pending card`() {
        val viewModel = viewModel()

        incomingCards.offer(payload)

        assertEquals(payload, viewModel.pending.value)
    }

    @Test
    fun `saves the card, clears it and reports it`() = runTest {
        val viewModel = viewModel()
        val outcomes = mutableListOf<SaveOutcome>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.outcome.collect { outcomes += it }
        }
        incomingCards.offer(payload)
        viewModel.save()
        advanceUntilIdle()

        assertEquals(payload, repository.saved)
        assertNull(incomingCards.pending.value)
        assertEquals(listOf(SaveOutcome.Saved), outcomes)
    }

    @Test
    fun `tells when the card is already stored`() = runTest {
        val viewModel = viewModel(FakeReceivedContactRepository(listOf(stored)))

        incomingCards.offer(payload)
        advanceUntilIdle()

        assertTrue(viewModel.alreadyStored.value)
    }

    @Test
    fun `tells when the card is new`() = runTest {
        val viewModel = viewModel()

        incomingCards.offer(payload)
        advanceUntilIdle()

        assertFalse(viewModel.alreadyStored.value)
    }

    @Test
    fun `reports an update when the card is stored again`() = runTest {
        val repository = FakeReceivedContactRepository(listOf(stored))
        val viewModel = viewModel(repository)
        val outcomes = mutableListOf<SaveOutcome>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.outcome.collect { outcomes += it }
        }
        incomingCards.offer(payload)
        viewModel.save()
        advanceUntilIdle()

        assertEquals(listOf(SaveOutcome.Updated), outcomes)
        assertEquals(1, repository.contacts.first().size)
    }

    @Test
    fun `discards without saving`() = runTest {
        val viewModel = viewModel()

        incomingCards.offer(payload)
        viewModel.discard()
        advanceUntilIdle()

        assertNull(repository.saved)
        assertNull(incomingCards.pending.value)
    }

    @Test
    fun `ignores save when nothing is pending`() = runTest {
        val viewModel = viewModel()

        viewModel.save()
        advanceUntilIdle()

        assertNull(repository.saved)
    }

    private fun viewModel(repository: FakeReceivedContactRepository = this.repository) =
        ReceiveViewModel(incomingCards, repository)
}
