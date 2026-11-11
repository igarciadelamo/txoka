package es.igarciadelamo.txoka.nfc

import es.igarciadelamo.txoka.domain.profile.ShareRole
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeTransportTest {
    @Test
    fun `demo card survives the ndef and vcard round trip`() = runTest {
        val incomingCards = IncomingCards()
        val transport = FakeTransport(incomingCards, ShareRoleController(backgroundScope))

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
    fun `queues two cards from different senders`() = runTest {
        val incomingCards = IncomingCards()
        val transport = FakeTransport(incomingCards, ShareRoleController(backgroundScope))

        transport.emitDemoCards()

        val queued = incomingCards.queue.value
        assertEquals(2, queued.size)
        assertEquals(queued.map { it.name }.distinct(), queued.map { it.name })
        assertEquals(listOf("+34600333444", "+34600555666"), queued.map { it.phone })
    }

    @Test
    fun `a demo card arms the return offer while receiving`() = runTest {
        val roles = ShareRoleController(backgroundScope)
        roles.select(ShareRole.Receive)
        val transport = FakeTransport(IncomingCards(), roles)

        transport.emitDemoCard()

        assertTrue(roles.state.value.returning)
    }

    @Test
    fun `a demo card leaves the send role alone`() = runTest {
        val roles = ShareRoleController(backgroundScope)
        val transport = FakeTransport(IncomingCards(), roles)

        transport.emitDemoCard()

        assertEquals(ShareRole.Send, roles.state.value.role)
        assertFalse(roles.state.value.returning)
    }
}
