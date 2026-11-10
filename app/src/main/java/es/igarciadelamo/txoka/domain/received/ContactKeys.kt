package es.igarciadelamo.txoka.domain.received

/**
 * Comparable forms of the optional fields, used to detect contacts we already have.
 * The name is deliberately left out: two people can share a name.
 */
object ContactKeys {
    fun phone(e164: String?): String = e164.orEmpty().filter(Char::isDigit)

    fun email(address: String?): String = address.orEmpty().trim().lowercase()

    fun social(handle: String?): String = handle.orEmpty().trim().removePrefix("@").lowercase()
}
