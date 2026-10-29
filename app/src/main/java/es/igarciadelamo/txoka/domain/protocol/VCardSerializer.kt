package es.igarciadelamo.txoka.domain.protocol

import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.RgbColor

sealed interface VCardParseResult {
    data class Success(val payload: VCardPayload) : VCardParseResult

    data class Failure(val reason: String) : VCardParseResult
}

/**
 * Reads and writes vCard 3.0 text. The card is plain vCard plus `X-TXOKA-*` properties, so other
 * readers get a usable contact and unknown extensions are ignored by both sides.
 */
object VCardSerializer {
    private const val VERSION = "3.0"
    private const val CRLF = "\r\n"
    private const val FOLD = "\r\n "
    private const val MAX_LINE_OCTETS = 75
    private const val FOLD_LINE_PREFIX_OCTETS = 1
    private const val PROPERTY_PROTOCOL = "X-TXOKA-PROTOCOL"
    private const val PROPERTY_ACCENT = "X-TXOKA-CARD-ACCENT"
    private const val PROPERTY_ACCENT2 = "X-TXOKA-CARD-ACCENT2"

    private val FOLD_PATTERN = Regex("\\r?\\n[ \t]")
    private val LINE_PATTERN = Regex("\\r\\n|\\n|\\r")

    fun encode(payload: VCardPayload): String {
        val lines = buildList {
            add("BEGIN:VCARD")
            add("VERSION:$VERSION")
            add("FN:${escape(payload.name)}")
            payload.phone?.let { phone -> add("TEL;TYPE=CELL:${escape(phone)}") }
            payload.email?.let { email -> add("EMAIL;TYPE=INTERNET:${escape(email)}") }
            add("$PROPERTY_PROTOCOL:${payload.protocolVersion}")
            add("$PROPERTY_ACCENT:${payload.palette.accent.hex}")
            add("$PROPERTY_ACCENT2:${payload.palette.accent2.hex}")
            add("END:VCARD")
        }
        return lines.joinToString(separator = CRLF, postfix = CRLF) { line -> fold(line) }
    }

    fun decode(text: String): VCardParseResult {
        val lines = text.replace(FOLD_PATTERN, "").split(LINE_PATTERN)
        val properties = lines.mapNotNull(::parseLine)

        if (properties.none { property -> property.name == "BEGIN" }) {
            return VCardParseResult.Failure("Missing BEGIN:VCARD")
        }

        val name = properties.firstOrNull { property -> property.name == "FN" }?.value?.trim().orEmpty()
        if (name.isEmpty()) {
            return VCardParseResult.Failure("Missing FN")
        }

        val phones = properties.filter { property -> property.name == "TEL" }
        val emails = properties.filter { property -> property.name == "EMAIL" }

        return VCardParseResult.Success(
            VCardPayload(
                name = name,
                phone = phones.preferred("CELL"),
                email = emails.preferred("INTERNET"),
                palette = palette(properties),
                protocolVersion = properties.firstOrNull { it.name == PROPERTY_PROTOCOL }
                    ?.value
                    ?.trim()
                    ?.toIntOrNull()
                    ?: VCardPayload.UNKNOWN_PROTOCOL_VERSION,
            ),
        )
    }

    private fun palette(properties: List<Property>): CardPalette {
        val accent = properties.firstOrNull { it.name == PROPERTY_ACCENT }
            ?.value
            ?.let(RgbColor::parse)
            ?: return CardPalette.Default
        val accent2 = properties.firstOrNull { it.name == PROPERTY_ACCENT2 }
            ?.value
            ?.let(RgbColor::parse)
        return if (accent2 == null) CardPalette.fromAccent(accent) else CardPalette(accent, accent2)
    }

    private fun List<Property>.preferred(type: String): String? {
        val matching = firstOrNull { property ->
            property.params.any { param -> param.startsWith("TYPE=") && param.contains(type, ignoreCase = true) }
        }
        return (matching ?: firstOrNull())?.value?.takeIf { it.isNotBlank() }
    }

    private fun fold(line: String): String {
        if (line.toByteArray(Charsets.UTF_8).size <= MAX_LINE_OCTETS) return line

        val folded = StringBuilder()
        var octets = 0
        var index = 0
        while (index < line.length) {
            val codePoint = line.codePointAt(index)
            val encodedSize = String(Character.toChars(codePoint)).toByteArray(Charsets.UTF_8).size
            val limit = if (folded.isEmpty()) MAX_LINE_OCTETS else MAX_LINE_OCTETS - FOLD_LINE_PREFIX_OCTETS
            if (octets + encodedSize > limit) {
                folded.append(FOLD)
                octets = 0
            }
            folded.appendCodePoint(codePoint)
            octets += encodedSize
            index += Character.charCount(codePoint)
        }
        return folded.toString()
    }

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\r\n", "\\n")
        .replace("\n", "\\n")
        .replace("\r", "\\n")

    private fun unescape(value: String): String {
        val result = StringBuilder(value.length)
        var index = 0
        while (index < value.length) {
            val char = value[index]
            if (char == '\\' && index + 1 < value.length) {
                when (val next = value[index + 1]) {
                    'n', 'N' -> result.append('\n')
                    else -> result.append(next)
                }
                index += 2
            } else {
                result.append(char)
                index++
            }
        }
        return result.toString()
    }

    private fun parseLine(line: String): Property? {
        if (line.isBlank()) return null
        val colon = indexOfValueSeparator(line) ?: return null
        val parts = splitOutsideQuotes(line.substring(0, colon), ';')
        val name = parts.firstOrNull()?.trim()?.uppercase().orEmpty()
        if (name.isEmpty()) return null
        return Property(
            name = name,
            params = parts.drop(1).map { param -> param.trim().uppercase() },
            value = unescape(line.substring(colon + 1)),
        )
    }

    /** Finds the colon that separates the property head from its value, ignoring quoted params. */
    private fun indexOfValueSeparator(line: String): Int? {
        var quoted = false
        line.forEachIndexed { index, char ->
            when {
                char == '"' -> quoted = !quoted
                char == ':' && !quoted -> return index
            }
        }
        return null
    }

    private fun splitOutsideQuotes(value: String, separator: Char): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        value.forEach { char ->
            when {
                char == '"' -> {
                    quoted = !quoted
                    current.append(char)
                }
                char == separator && !quoted -> {
                    parts.add(current.toString())
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        parts.add(current.toString())
        return parts
    }

    private data class Property(val name: String, val params: List<String>, val value: String)
}
