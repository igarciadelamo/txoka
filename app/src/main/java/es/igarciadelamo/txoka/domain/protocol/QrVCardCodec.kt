package es.igarciadelamo.txoka.domain.protocol

import es.igarciadelamo.txoka.domain.profile.Profile
import java.nio.CharBuffer
import java.nio.charset.CharacterCodingException

/** Plain UTF-8 vCard, including STYLE, without the NFC record or Android application record. */
object QrVCardCodec {
    // Leaves room below QR version 40-M's byte capacity for the UTF-8 ECI header.
    const val MAX_QR_VCARD_BYTES = 2048

    private val LINE_PATTERN = Regex("\\r\\n|\\n|\\r")

    fun byteSize(profile: Profile): Int = utf8Size(VCardSerializer.encode(profile.toVCardPayload())) ?: Int.MAX_VALUE

    fun fits(profile: Profile): Boolean = byteSize(profile) <= MAX_QR_VCARD_BYTES

    fun encode(profile: Profile): String {
        val text = VCardSerializer.encode(profile.toVCardPayload())
        require(isBoundedUtf8(text)) { "QR vCard must be valid UTF-8 within $MAX_QR_VCARD_BYTES bytes" }
        return text
    }

    fun decode(text: String): VCardPayload? {
        if (!isBoundedUtf8(text) || !hasVCardEnvelope(text)) return null
        return (VCardSerializer.decode(text) as? VCardParseResult.Success)?.payload
    }

    private fun isBoundedUtf8(text: String): Boolean =
        text.length <= MAX_QR_VCARD_BYTES && (utf8Size(text) ?: Int.MAX_VALUE) <= MAX_QR_VCARD_BYTES

    private fun utf8Size(text: String): Int? = try {
        Charsets.UTF_8.newEncoder().encode(CharBuffer.wrap(text)).remaining()
    } catch (_: CharacterCodingException) {
        null
    }

    private fun hasVCardEnvelope(text: String): Boolean {
        if (text.any { it.isISOControl() && it !in "\r\n\t" }) return false
        val lines = text.trimEnd('\r', '\n').split(LINE_PATTERN)
        return lines.firstOrNull().equals("BEGIN:VCARD", ignoreCase = true) &&
            lines.lastOrNull().equals("END:VCARD", ignoreCase = true) &&
            hasSingleProperty(lines, "BEGIN", "VCARD") &&
            hasSingleProperty(lines, "END", "VCARD") &&
            hasSingleProperty(lines, "VERSION", "3.0")
    }

    private fun hasSingleProperty(lines: List<String>, name: String, value: String): Boolean {
        val properties = lines.filter { it.startsWith("$name:", ignoreCase = true) }
        return properties.singleOrNull().equals("$name:$value", ignoreCase = true)
    }
}
