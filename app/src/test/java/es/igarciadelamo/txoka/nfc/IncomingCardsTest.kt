package es.igarciadelamo.txoka.nfc

import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IncomingCardsTest {
    private val first = VCardPayload(name = "Marta Ruiz", phone = "+34600111222")
    private val second = VCardPayload(name = "Ana Ruiz", phone = "+34600333444")

    @Test
    fun `starts empty`() {
        val cards = IncomingCards()

        assertEquals(emptyList<VCardPayload>(), cards.queue.value)
        assertNull(cards.pending)
    }

    @Test
    fun `keeps every card in arrival order`() {
        val cards = IncomingCards()

        cards.offer(first)
        cards.offer(second)

        assertEquals(listOf(first, second), cards.queue.value)
        assertEquals(first, cards.pending)
    }

    @Test
    fun `consuming the first card leaves the next one pending`() {
        val cards = IncomingCards()
        cards.offer(first)
        cards.offer(second)

        cards.consume(first)

        assertEquals(listOf(second), cards.queue.value)
        assertEquals(second, cards.pending)
    }

    @Test
    fun `ignores an identical card already waiting`() {
        val cards = IncomingCards()

        assertEquals(true, cards.offer(first))
        assertEquals(false, cards.offer(first.copy()))

        assertEquals(listOf(first), cards.queue.value)
    }

    @Test
    fun `caps the pending queue`() {
        val cards = IncomingCards()

        repeat(IncomingCards.MAX_PENDING) { index ->
            assertEquals(true, cards.offer(VCardPayload(name = "Person $index")))
        }
        assertEquals(false, cards.offer(VCardPayload(name = "One more")))

        assertEquals(IncomingCards.MAX_PENDING, cards.queue.value.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `reports an invalid card`() = runTest {
        val cards = IncomingCards()
        val reported = async(UnconfinedTestDispatcher(testScheduler)) { cards.invalid.first() }

        cards.reportInvalid()

        assertEquals(Unit, reported.await())
    }

    @Test
    fun `consuming a card that is not queued keeps the rest`() {
        val cards = IncomingCards()
        cards.offer(second)

        cards.consume(first)

        assertEquals(listOf(second), cards.queue.value)
    }

    @Test
    fun `consuming an empty queue is safe`() {
        val cards = IncomingCards()

        cards.consume(first)

        assertEquals(emptyList<VCardPayload>(), cards.queue.value)
        assertNull(cards.pending)
    }
}
