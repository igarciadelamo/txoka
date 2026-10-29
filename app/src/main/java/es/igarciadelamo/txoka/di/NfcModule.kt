package es.igarciadelamo.txoka.di

import android.app.KeyguardManager
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import es.igarciadelamo.txoka.nfc.AndroidNfcAvailability
import es.igarciadelamo.txoka.nfc.HceResponder
import es.igarciadelamo.txoka.nfc.NfcAvailability
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NfcModule {
    @Provides
    @Singleton
    fun provideHceResponder(): HceResponder = HceResponder()

    @Provides
    @Singleton
    fun provideNfcAvailability(availability: AndroidNfcAvailability): NfcAvailability = availability

    @Provides
    @Singleton
    fun provideKeyguardManager(@ApplicationContext context: Context): KeyguardManager =
        requireNotNull(context.getSystemService(KeyguardManager::class.java)) {
            "KeyguardManager is always available"
        }
}
