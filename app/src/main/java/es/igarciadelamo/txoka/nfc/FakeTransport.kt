package es.igarciadelamo.txoka.nfc

import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.protocol.ContactTransport
import es.igarciadelamo.txoka.domain.protocol.NdefMessage
import es.igarciadelamo.txoka.domain.protocol.VCardParseResult
import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import es.igarciadelamo.txoka.domain.protocol.VCardSerializer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stand in for real hardware, which emulators do not have. The demo card travels through the same
 * vCard and NDEF round trip as a real tag, so the emulator exercises the real decoding path.
 */
@Singleton
class FakeTransport @Inject constructor(private val incomingCards: IncomingCards) : ContactTransport {
    override suspend fun publish(payload: VCardPayload?) = Unit

    override val received: Flow<VCardPayload> = emptyFlow()

    fun emitDemoCard() {
        val ndef = NdefMessage.encode(VCardSerializer.encode(DEMO_CARD))
        val decoded = NdefMessage.decode(ndef) ?: return
        val parsed = VCardSerializer.decode(decoded)
        if (parsed is VCardParseResult.Success) {
            incomingCards.offer(parsed.payload)
        }
    }

    private companion object {
        val DEMO_CARD = VCardPayload(
            name = "Marta Ruiz",
            phone = "+34600111222",
            email = "marta@example.com",
            palette = CardPalette.Presets[2],
        )
    }
}
