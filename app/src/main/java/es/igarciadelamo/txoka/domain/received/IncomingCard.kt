package es.igarciadelamo.txoka.domain.received

import es.igarciadelamo.txoka.domain.profile.ProfileLimits
import es.igarciadelamo.txoka.domain.profile.SocialHandle
import es.igarciadelamo.txoka.domain.profile.SocialNetwork
import es.igarciadelamo.txoka.domain.protocol.NdefMessage
import es.igarciadelamo.txoka.domain.protocol.VCardParseResult
import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import es.igarciadelamo.txoka.domain.protocol.VCardSerializer

/**
 * Decodes and cleans a card before it reaches the library. Tags and future imported files are not
 * trusted: a message over [MAX_MESSAGE_BYTES] is ignored, every field is clamped and stripped of
 * control characters, and a card without a usable name is rejected.
 */
object IncomingCard {
    const val MAX_MESSAGE_BYTES = 4 * 1024

    fun fromNdef(message: ByteArray): VCardPayload? {
        if (message.size > MAX_MESSAGE_BYTES) return null
        val vCard = NdefMessage.decode(message) ?: return null
        val parsed = VCardSerializer.decode(vCard) as? VCardParseResult.Success ?: return null
        return sanitize(parsed.payload)
    }

    fun sanitize(payload: VCardPayload): VCardPayload? {
        val name = clean(payload.name, ProfileLimits.NAME, " ").replace(WHITESPACE, " ")
        if (name.isBlank()) return null

        return payload.copy(
            name = name,
            phone = cleanOrNull(payload.phone, ProfileLimits.PHONE),
            email = cleanOrNull(payload.email, ProfileLimits.EMAIL),
            linkedin = handle(payload.linkedin, SocialNetwork.LinkedIn),
            instagram = handle(payload.instagram, SocialNetwork.Instagram),
        )
    }

    private fun handle(raw: String?, network: SocialNetwork): String? =
        cleanOrNull(raw, ProfileLimits.HANDLE)?.let { SocialHandle.normalize(it, network) }

    private fun cleanOrNull(value: String?, max: Int): String? =
        value?.let { clean(it, max, "") }?.takeIf { it.isNotBlank() }

    private fun clean(value: String, max: Int, controlReplacement: String): String =
        ProfileLimits.limit(value.replace(CONTROL, controlReplacement).trim(), max)

    private val CONTROL = Regex("[\\p{Cc}\\p{Cf}\\u2028\\u2029]+")
    private val WHITESPACE = Regex("\\s+")
}
