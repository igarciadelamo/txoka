package es.igarciadelamo.txoka.domain.received

/**
 * Comparable forms of the optional fields, used to detect contacts we already have.
 */
object ContactKeys {
    fun phone(e164: String?): String = e164.orEmpty().filter(Char::isDigit)

    fun email(address: String?): String = address.orEmpty().trim().lowercase()
}
