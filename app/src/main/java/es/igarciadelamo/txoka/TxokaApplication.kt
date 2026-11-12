package es.igarciadelamo.txoka

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import dagger.hilt.android.HiltAndroidApp
import es.igarciadelamo.txoka.nfc.HceCardPublisher
import es.igarciadelamo.txoka.nfc.NdefHceService
import javax.inject.Inject

@HiltAndroidApp
class TxokaApplication : Application() {
    @Inject
    lateinit var cardPublisher: HceCardPublisher

    override fun onCreate() {
        super.onCreate()
        restoreHceService()
        // The HCE service may be bound before any screen is opened, so the card is published here.
        cardPublisher.start()
    }

    /**
     * An earlier build disabled the HCE service while receiving and the disabled state survives
     * reinstalls, which leaves the phone unable to send. Enabling it again is idempotent.
     */
    private fun restoreHceService() {
        runCatching {
            packageManager.setComponentEnabledSetting(
                ComponentName(this, NdefHceService::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}
