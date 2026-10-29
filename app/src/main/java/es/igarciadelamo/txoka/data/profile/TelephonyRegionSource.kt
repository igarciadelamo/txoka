package es.igarciadelamo.txoka.data.profile

import android.content.Context
import android.telephony.TelephonyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import es.igarciadelamo.txoka.domain.profile.SimRegionSource
import javax.inject.Inject

class TelephonyRegionSource @Inject constructor(@ApplicationContext context: Context) : SimRegionSource {
    private val telephony: TelephonyManager? = context.getSystemService(TelephonyManager::class.java)

    override fun simRegion(): String? = read { telephony?.simCountryIso }

    override fun networkRegion(): String? = read { telephony?.networkCountryIso }

    // Region is optional metadata: a device that refuses to report it just falls back to the locale.
    private inline fun read(value: () -> String?): String? =
        runCatching { value() }.getOrNull()?.takeIf { it.isNotBlank() }?.uppercase()
}
