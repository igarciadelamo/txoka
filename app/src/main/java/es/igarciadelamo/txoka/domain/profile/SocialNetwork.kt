package es.igarciadelamo.txoka.domain.profile

/**
 * Social profiles the card can carry. Handles are stored bare (`ana.ruiz`, no `@` and no URL) so
 * the payload stays small; the profile link is rebuilt wherever it is needed.
 */
enum class SocialNetwork(val vCardType: String, val host: String, val profileUrl: String, val displayPrefix: String) {
    LinkedIn(
        vCardType = "LINKEDIN",
        host = "linkedin.com",
        profileUrl = "https://www.linkedin.com/in/",
        displayPrefix = "",
    ),
    Instagram(
        vCardType = "INSTAGRAM",
        host = "instagram.com",
        profileUrl = "https://www.instagram.com/",
        displayPrefix = "@",
    ),
}

object SocialHandle {
    private val PATTERN = Regex("^[A-Za-z0-9._-]{1,60}$")

    /**
     * Accepts a bare handle or a pasted profile link and returns the bare handle, or null when the
     * value is not a valid handle for [network].
     */
    fun normalize(raw: String, network: SocialNetwork): String? {
        val value = raw.trim().removePrefix("@").trimEnd('/')
        if (value.isEmpty()) return null
        val handle = if ('/' in value) handleFromUrl(value, network) else value
        return handle?.takeIf(PATTERN::matches)
    }

    fun url(network: SocialNetwork, handle: String): String = network.profileUrl + handle

    fun display(network: SocialNetwork, handle: String): String = network.displayPrefix + handle

    private fun handleFromUrl(value: String, network: SocialNetwork): String? {
        val withoutQuery = value.substringAfter("://", value).substringBefore('?').substringBefore('#')
        val host = withoutQuery.substringBefore('/').lowercase()
        if (host != network.host && !host.endsWith(".${network.host}")) return null
        return withoutQuery.substringAfter('/', "").split('/').lastOrNull { it.isNotBlank() }
    }
}
