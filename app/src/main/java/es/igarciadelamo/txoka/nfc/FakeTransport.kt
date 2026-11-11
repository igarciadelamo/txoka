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

    fun emitDemoCard() = offer(DEMO_CARD)

    /** Two senders at once, so the confirmation queue can be exercised without real hardware. */
    fun emitDemoCards() = QUEUE_DEMO_CARDS.forEach(::offer)

    private fun offer(card: VCardPayload) {
        val ndef = NdefMessage.encode(VCardSerializer.encode(card))
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
            linkedin = "marta-ruiz",
            instagram = "marta.ruiz",
            palette = CardPalette.Presets[2],
        )

        val QUEUE_DEMO_CARDS = listOf(
            VCardPayload(
                name = "Ana Ruiz",
                phone = "+34600333444",
                email = "ana@example.com",
                linkedin = "ana-ruiz",
                instagram = "ana.ruiz",
                palette = CardPalette.Presets[4],
            ),
            VCardPayload(
                name = "Luis Ortega",
                phone = "+34600555666",
                email = "luis@example.com",
                instagram = "luis.ortega",
                palette = CardPalette.Presets[1],
            ),
        )
    }
}
