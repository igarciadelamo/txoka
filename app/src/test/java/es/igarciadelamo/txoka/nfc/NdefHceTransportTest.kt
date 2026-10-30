package es.igarciadelamo.txoka.nfc

import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.domain.protocol.Type4Tag
import es.igarciadelamo.txoka.domain.protocol.toVCardPayload
import es.igarciadelamo.txoka.testing.FakeProfileRepository
import es.igarciadelamo.txoka.testing.FakeShareSettingsRepository
import es.igarciadelamo.txoka.testing.NfcTestPackets.SelectAid
import es.igarciadelamo.txoka.testing.NfcTestPackets.status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class NdefHceTransportTest {
    private val incoming = IncomingCards()
    private val responder = HceResponder()
    private val publisher = HceCardPublisher(
        profileRepository = FakeProfileRepository(Profile(name = "Isidro")),
        settingsRepository = FakeShareSettingsRepository(),
        responder = responder,
        scope = CoroutineScope(Dispatchers.Unconfined),
    )

    @Test
    fun `publishes through the HCE responder`() = runTest {
        val transport = NdefHceTransport(publisher, incoming)

        transport.publish(Profile(name = "Marta").toVCardPayload())

        assertEquals(Type4Tag.SW_OK, status(responder.respond(SelectAid, deviceLocked = false)))
    }

    @Test
    fun `emits incoming cards and skips the empty state`() = runTest {
        val transport = NdefHceTransport(publisher, incoming)
        val received = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            transport.received.collect { payload -> received += payload.name }
        }

        incoming.consume()
        val payload = Profile(name = "Marta").toVCardPayload()
        incoming.offer(payload)

        assertEquals(listOf("Marta"), received)
        assertEquals("Marta", transport.received.first().name)
    }
}
