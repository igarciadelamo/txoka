package es.igarciadelamo.txoka.domain.profile

/**
 * What the editor accepts per field. The encoded UTF-8 vCard is also checked against the QR budget
 * before saving because escaping, folding and multibyte text affect its size. NFC has a smaller,
 * independent budget, so some saved cards can only be shared by QR.
 */
object ProfileLimits {
    const val NAME = 64
    const val PHONE = 32
    const val EMAIL = 120
    const val HANDLE = 60

    /** Truncates [value] to [max] characters without splitting a surrogate pair. */
    fun limit(value: String, max: Int): String {
        if (value.codePointCount(0, value.length) <= max) return value
        return value.substring(0, value.offsetByCodePoints(0, max))
    }
}
