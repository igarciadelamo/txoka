package es.igarciadelamo.txoka.domain.profile

/**
 * The card the user shares. The name is mandatory; phone and email are shared whenever present.
 */
data class Profile(
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val card: CardPalette = CardPalette.Default,
) {
    val initials: String
        get() = name.trim()
            .split(WHITESPACE)
            .filter { it.isNotEmpty() }
            .take(MAX_INITIALS)
            .map { it.first().uppercaseChar() }
            .joinToString(separator = "")

    private companion object {
        const val MAX_INITIALS = 2
        val WHITESPACE = Regex("\\s+")
    }
}
