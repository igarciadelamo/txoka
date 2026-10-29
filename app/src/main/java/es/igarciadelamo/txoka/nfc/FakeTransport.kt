package es.igarciadelamo.txoka.nfc

import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.protocol.ContactTransport
import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stand in for real hardware, which emulators do not have. Demo cards enter the same confirmation
 * flow as a real tag.
 */
@Singleton
class FakeTransport @Inject constructor(private val incomingCards: IncomingCards) : ContactTransport {
    override suspend fun publish(payload: VCardPayload?) = Unit

    override val received: Flow<VCardPayload> = emptyFlow()

    fun emitDemoCard() {
        incomingCards.offer(
            VCardPayload(
                name = "Marta Ruiz",
                phone = "+34600111222",
                email = "marta@example.com",
                palette = CardPalette.Presets[2],
            ),
        )
    }
}
