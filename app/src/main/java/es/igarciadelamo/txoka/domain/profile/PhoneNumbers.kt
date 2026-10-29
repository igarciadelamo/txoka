package es.igarciadelamo.txoka.domain.profile

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber

/**
 * Normalises user input to E.164. Input without a country prefix is parsed against
 * [defaultRegion], which is an ISO 3166-1 alpha-2 code such as "ES".
 */
object PhoneNumbers {
    private val util: PhoneNumberUtil = PhoneNumberUtil.getInstance()

    fun toE164(raw: String, defaultRegion: String): String? {
        val input = raw.trim()
        if (input.isEmpty()) return null
        val number = parse(input, defaultRegion) ?: return null
        if (!util.isValidNumber(number)) return null
        return util.format(number, PhoneNumberUtil.PhoneNumberFormat.E164)
    }

    private fun parse(input: String, defaultRegion: String): Phonenumber.PhoneNumber? = try {
        util.parse(input, defaultRegion.uppercase())
    } catch (exception: NumberParseException) {
        null
    }
}
