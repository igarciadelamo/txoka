package es.igarciadelamo.txoka.domain.profile

/**
 * Provides the regions reported by the device so national phone numbers can be parsed
 * without asking the user first.
 */
interface SimRegionSource {
    fun simRegion(): String?

    fun networkRegion(): String?
}
