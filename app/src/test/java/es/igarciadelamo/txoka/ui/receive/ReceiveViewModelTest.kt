package es.igarciadelamo.txoka.ui.receive

import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import es.igarciadelamo.txoka.nfc.IncomingCards
import es.igarciadelamo.txoka.testing.FakeReceivedContactRepository
import es.igarciadelamo.txoka.testing.MainDispatcherRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ReceiveViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val incomingCards = IncomingCards()
    private val repository = FakeReceivedContactRepository()
    private val viewModel = ReceiveViewModel(incomingCards, repository)
    private val payload = VCardPayload(name = "Marta Ruiz", phone = "+34600111222")

    @Test
    fun `exposes the pending card`() {
        incomingCards.offer(payload)

        assertEquals(payload, viewModel.pending.value)
    }

    @Test
    fun `saves the card and clears it`() = runTest {
        incomingCards.offer(payload)

        viewModel.save()
        advanceUntilIdle()

        assertEquals(payload, repository.saved)
        assertNull(incomingCards.pending.value)
    }

    @Test
    fun `discards without saving`() = runTest {
        incomingCards.offer(payload)

        viewModel.discard()
        advanceUntilIdle()

        assertNull(repository.saved)
        assertNull(incomingCards.pending.value)
    }

    @Test
    fun `ignores save when nothing is pending`() = runTest {
        viewModel.save()
        advanceUntilIdle()

        assertNull(repository.saved)
    }
}
