package es.igarciadelamo.txoka.testing

import es.igarciadelamo.txoka.domain.profile.SimRegionSource

class FakeSimRegionSource(private val sim: String? = "ES", private val network: String? = null) : SimRegionSource {
    override fun simRegion(): String? = sim

    override fun networkRegion(): String? = network
}
