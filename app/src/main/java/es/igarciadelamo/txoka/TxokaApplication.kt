package es.igarciadelamo.txoka

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import es.igarciadelamo.txoka.nfc.HceCardPublisher
import javax.inject.Inject

@HiltAndroidApp
class TxokaApplication : Application() {
    @Inject
    lateinit var cardPublisher: HceCardPublisher

    override fun onCreate() {
        super.onCreate()
        // The HCE service may be bound before any screen is opened, so the card is published here.
        cardPublisher.start()
    }
}
