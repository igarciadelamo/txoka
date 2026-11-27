package es.igarciadelamo.txoka.domain.received

import java.text.Normalizer

/**
 * Library search: case and accent insensitive over the text fields, digits only over the phone,
 * and the local note counts as well.
 */
fun ReceivedContact.matchesSearch(query: String): Boolean {
    val text = query.trim()
    if (text.isEmpty()) return true

    val needle = text.searchable()
    val phoneNeedle = text.filter(Char::isDigit)
    val phoneQuery = phoneNeedle.isNotEmpty() && text.all { it.isDigit() || it in PHONE_SEPARATORS }

    return name.searchable().contains(needle) ||
        email.orEmpty().searchable().contains(needle) ||
        linkedin.orEmpty().searchable().contains(needle) ||
        instagram.orEmpty().searchable().contains(needle) ||
        note.searchable().contains(needle) ||
        (phoneQuery && ContactKeys.phone(phone).contains(phoneNeedle))
}

private fun String.searchable(): String = Normalizer.normalize(this, Normalizer.Form.NFD)
    .replace(DIACRITICS, "")
    .lowercase()

private val DIACRITICS = Regex("\\p{M}+")
private const val PHONE_SEPARATORS = " +-()./"
