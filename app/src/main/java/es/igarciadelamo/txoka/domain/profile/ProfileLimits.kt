package es.igarciadelamo.txoka.domain.profile

/**
 * What the editor accepts per field. A card has to fit in a 768-byte Type 4 Tag file and the vCard
 * escapes and folds long values, so these caps keep a normal card far below the budget; the encoded
 * size is still checked before saving because the caps alone cannot bound multibyte text.
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
