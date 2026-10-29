package es.igarciadelamo.txoka.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import es.igarciadelamo.txoka.data.profile.DataStoreProfileRepository
import es.igarciadelamo.txoka.data.profile.DataStoreShareSettingsRepository
import es.igarciadelamo.txoka.data.profile.TelephonyRegionSource
import es.igarciadelamo.txoka.domain.profile.ProfileRepository
import es.igarciadelamo.txoka.domain.profile.ShareSettingsRepository
import es.igarciadelamo.txoka.domain.profile.SimRegionSource
import javax.inject.Singleton

private val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(name = "profile")

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideProfileDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.profileDataStore

    @Provides
    @Singleton
    fun provideProfileRepository(dataStore: DataStore<Preferences>): ProfileRepository =
        DataStoreProfileRepository(dataStore)

    @Provides
    @Singleton
    fun provideShareSettingsRepository(dataStore: DataStore<Preferences>): ShareSettingsRepository =
        DataStoreShareSettingsRepository(dataStore)

    @Provides
    @Singleton
    fun provideSimRegionSource(source: TelephonyRegionSource): SimRegionSource = source
}
