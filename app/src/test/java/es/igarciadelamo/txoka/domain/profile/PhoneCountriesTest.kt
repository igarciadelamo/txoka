package es.igarciadelamo.txoka.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneCountriesTest {
    @Test
    fun `sim region wins over network and locale`() {
        assertEquals("ES", PhoneCountries.resolveRegion("ES", "FR", "US"))
    }

    @Test
    fun `falls back to network then locale`() {
        assertEquals("FR", PhoneCountries.resolveRegion(null, "FR", "US"))
        assertEquals("US", PhoneCountries.resolveRegion(null, null, "US"))
    }

    @Test
    fun `falls back to Spain when nothing is a known region`() {
        assertEquals(PhoneCountries.FALLBACK_REGION, PhoneCountries.resolveRegion(null, null, null))
        assertEquals(PhoneCountries.FALLBACK_REGION, PhoneCountries.resolveRegion("ZZ", "", "??"))
    }

    @Test
    fun `knows the dial code of a region`() {
        assertEquals("34", PhoneCountries.find("es")?.dialCode)
        assertEquals("1", PhoneCountries.find("US")?.dialCode)
    }

    @Test
    fun `falls back when the region is unknown`() {
        assertEquals("ES", PhoneCountries.byIsoOrFallback("ZZ").iso)
        assertEquals("ES", PhoneCountries.byIsoOrFallback(null).iso)
        assertEquals("FR", PhoneCountries.byIsoOrFallback("FR").iso)
    }

    @Test
    fun `builds a flag from the region code`() {
        assertEquals("\uD83C\uDDEA\uD83C\uDDF8", PhoneCountries.flag("ES"))
        assertEquals("", PhoneCountries.flag("ESP"))
    }

    @Test
    fun `detects the region of a stored number`() {
        val fallback = PhoneCountries.byIsoOrFallback("US")

        assertEquals("ES", PhoneCountries.countryOf("+34600123456", fallback).iso)
        assertEquals("FR", PhoneCountries.countryOf("+33612345678", fallback).iso)
        assertEquals("US", PhoneCountries.countryOf("not a number", fallback).iso)
    }

    @Test
    fun `lists the supported regions`() {
        assertTrue(PhoneCountries.all.size > 200)
        assertTrue(PhoneCountries.all.any { country -> country.iso == PhoneCountries.FALLBACK_REGION })
    }
}
