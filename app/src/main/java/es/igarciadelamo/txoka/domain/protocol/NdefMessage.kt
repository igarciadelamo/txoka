package es.igarciadelamo.txoka.domain.protocol

import java.io.ByteArrayOutputStream

/**
 * Encodes a single MIME record NDEF message. This is the byte blob stored in the Type 4 Tag file,
 * so it stays free of Android dependencies and can be asserted in unit tests.
 */
object NdefMessage {
    const val MIME_TYPE = "text/vcard"
    const val MAX_MESSAGE_BYTES = 768

    private const val TNF_MIME = 0x02
    private const val TNF_MASK = 0x07
    private const val FLAG_MB = 0x80
    private const val FLAG_ME = 0x40
    private const val FLAG_SR = 0x10
    private const val FLAG_IL = 0x08
    private const val MAX_SHORT_PAYLOAD = 0xFF
    private const val LONG_LENGTH_BYTES = 4
    private const val BYTE_MASK = 0xFF
    private const val BITS_PER_BYTE = 8

    fun encode(vCard: String): ByteArray {
        val payload = vCard.toByteArray(Charsets.UTF_8)
        val type = MIME_TYPE.toByteArray(Charsets.US_ASCII)
        val isShort = payload.size <= MAX_SHORT_PAYLOAD

        val message = ByteArrayOutputStream()
        message.write(FLAG_MB or FLAG_ME or TNF_MIME or if (isShort) FLAG_SR else 0)
        message.write(type.size)
        if (isShort) {
            message.write(payload.size)
        } else {
            repeat(LONG_LENGTH_BYTES) { index ->
                message.write(payload.size ushr (BITS_PER_BYTE * (LONG_LENGTH_BYTES - 1 - index)))
            }
        }
        // write(ByteArray) works on every API level; writeBytes(ByteArray) needs API 33.
        message.write(type)
        message.write(payload)

        val bytes = message.toByteArray()
        require(bytes.size <= MAX_MESSAGE_BYTES) {
            "NDEF message of ${bytes.size} bytes exceeds the limit of $MAX_MESSAGE_BYTES"
        }
        return bytes
    }

    /** Returns the vCard text, or null when the message is not a single `text/vcard` record. */
    fun decode(message: ByteArray): String? {
        val header = message.firstOrNull()?.toInt()?.and(BYTE_MASK)
        if (header == null || header and TNF_MASK != TNF_MIME) return null
        val layout = layoutOf(message) ?: return null
        if (!layout.isVCardRecord(message)) return null
        return String(message, layout.payloadStart, layout.payloadLength, Charsets.UTF_8)
    }

    private fun layoutOf(message: ByteArray): Layout? {
        if (message.size <= MIN_MESSAGE_BYTES) return null

        val isShort = message[0].toInt() and FLAG_SR != 0
        var index = 1
        val typeLength = message[index++].toInt() and BYTE_MASK
        val payloadLength: Int = if (isShort) {
            (message[index++].toInt() and BYTE_MASK)
        } else {
            if (message.size < index + LONG_LENGTH_BYTES) return null
            var length = 0
            repeat(LONG_LENGTH_BYTES) {
                length = (length shl BITS_PER_BYTE) or (message[index++].toInt() and BYTE_MASK)
            }
            length
        }
        val idLength = if (message[0].toInt() and FLAG_IL != 0) message[index++].toInt() and BYTE_MASK else 0
        val typeStart = index
        val payloadStart = typeStart + typeLength + idLength
        if (payloadStart > message.size) return null

        return Layout(
            typeStart = typeStart,
            typeLength = typeLength,
            payloadStart = payloadStart,
            payloadLength = payloadLength,
        )
    }

    private data class Layout(val typeStart: Int, val typeLength: Int, val payloadStart: Int, val payloadLength: Int) {
        fun isVCardRecord(message: ByteArray): Boolean {
            val type = String(message, typeStart, typeLength, Charsets.US_ASCII)
            return type.equals(MIME_TYPE, ignoreCase = true) && payloadLength <= message.size - payloadStart
        }
    }

    private const val MIN_MESSAGE_BYTES = 4
}
