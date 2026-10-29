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

        val payload = incomingCards.pending.value
        assertEquals("Marta Ruiz", payload?.name)
        assertEquals("+34600111222", payload?.phone)
        assertEquals("marta@example.com", payload?.email)
    }

    @Test
    fun `does nothing before the demo card is emitted`() {
        val incomingCards = IncomingCards()

        assertNull(incomingCards.pending.value)
    }
}
