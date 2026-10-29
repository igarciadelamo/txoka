package es.igarciadelamo.txoka.domain.profile

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

/**
 * A dialling region used to turn national phone numbers into E.164.
 */
data class PhoneCountry(val iso: String, val dialCode: String) {
    val flag: String
        get() = PhoneCountries.flag(iso)

    val label: String
        get() = PhoneCountries.displayName(iso)
}

object PhoneCountries {
    const val FALLBACK_REGION = "ES"

    private val util: PhoneNumberUtil = PhoneNumberUtil.getInstance()
    private const val REGIONAL_INDICATOR_A = 0x1F1E6
    private const val REGION_LENGTH = 2

    /** Every region libphonenumber knows about, sorted by ISO code. */
    val all: List<PhoneCountry> = util.supportedRegions
        .map { iso -> PhoneCountry(iso = iso, dialCode = util.getCountryCodeForRegion(iso).toString()) }
        .filter { country -> country.dialCode != "0" }
        .sortedBy { country -> country.iso }

    fun find(iso: String): PhoneCountry? = all.firstOrNull { country -> country.iso.equals(iso, ignoreCase = true) }

    fun byIsoOrFallback(iso: String?): PhoneCountry = find(iso.orEmpty()) ?: find(FALLBACK_REGION) ?: all.first()

    /**
     * Resolves the region to start from: SIM first, then the network, then the device locale,
     * and finally [FALLBACK_REGION] so national numbers keep working without a SIM.
     */
    fun resolveRegion(simRegion: String?, networkRegion: String?, localeRegion: String?): String =
        listOf(simRegion, networkRegion, localeRegion)
            .firstOrNull { region -> region != null && find(region) != null }
            ?.uppercase()
            ?: FALLBACK_REGION

    /** Returns the region a stored E.164 number belongs to, falling back to [fallback]. */
    fun countryOf(e164: String, fallback: PhoneCountry): PhoneCountry {
        val number = try {
            util.parse(e164, "ZZ")
        } catch (exception: NumberParseException) {
            null
        } ?: return fallback
        return util.getRegionCodeForNumber(number)?.let(::find) ?: fallback
    }

    fun flag(iso: String): String {
        if (iso.length != REGION_LENGTH) return ""
        return iso.uppercase().map { letter ->
            String(Character.toChars(REGIONAL_INDICATOR_A + (letter - 'A')))
        }.joinToString(separator = "")
    }

    fun displayName(iso: String, locale: Locale = Locale.getDefault()): String =
        Locale.Builder().setRegion(iso.uppercase()).build().getDisplayCountry(locale).ifBlank { iso }
}
