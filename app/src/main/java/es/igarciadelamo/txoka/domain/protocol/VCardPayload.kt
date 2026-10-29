package es.igarciadelamo.txoka.domain.protocol

import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.Profile

/**
 * What travels between two devices: the shared fields plus the card style.
 * [protocolVersion] is ours, not the vCard version, so future payloads stay compatible.
 */
data class VCardPayload(
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val palette: CardPalette = CardPalette.Default,
    val protocolVersion: Int = CURRENT_PROTOCOL_VERSION,
) {
    companion object {
        const val CURRENT_PROTOCOL_VERSION = 1

        /** Version reported for cards that do not carry our extension properties. */
        const val UNKNOWN_PROTOCOL_VERSION = 0
    }
}

fun Profile.toVCardPayload(): VCardPayload = VCardPayload(
    name = name,
    phone = sharedPhone,
    email = sharedEmail,
    palette = card,
    protocolVersion = VCardPayload.CURRENT_PROTOCOL_VERSION,
)
