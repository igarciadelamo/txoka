package es.igarciadelamo.txoka.nfc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FakeTransportTest {
    @Test
    fun `demo card survives the ndef and vcard round trip`() {
        val incomingCards = IncomingCards()
        val transport = FakeTransport(incomingCards)

        transport.emitDemoCard()

        val payload = incomingCards.pending
        assertEquals("Marta Ruiz", payload?.name)
        assertEquals("+34600111222", payload?.phone)
        assertEquals("marta@example.com", payload?.email)
    }

    @Test
    fun `does nothing before the demo card is emitted`() {
        val incomingCards = IncomingCards()

        assertNull(incomingCards.pending)
    }

    @Test
    fun `queues two cards from different senders`() {
        val incomingCards = IncomingCards()
        val transport = FakeTransport(incomingCards)

        transport.emitDemoCards()

        val queued = incomingCards.queue.value
        assertEquals(2, queued.size)
        assertEquals(queued.map { it.name }.distinct(), queued.map { it.name })
        assertEquals(listOf("+34600333444", "+34600555666"), queued.map { it.phone })
    }
}
