package es.igarciadelamo.txoka.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import es.igarciadelamo.txoka.domain.profile.ShareSettings
import es.igarciadelamo.txoka.domain.profile.ShareSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreShareSettingsRepository(private val dataStore: DataStore<Preferences>) : ShareSettingsRepository {
    override val settings: Flow<ShareSettings> = dataStore.data.map { preferences ->
        ShareSettings(
            sharingEnabled = preferences[Keys.Sharing] ?: true,
            requireDeviceUnlock = preferences[Keys.RequireUnlock] ?: false,
        )
    }

    override suspend fun setSharingEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[Keys.Sharing] = enabled }
    }

    override suspend fun setRequireDeviceUnlock(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[Keys.RequireUnlock] = enabled }
    }

    private object Keys {
        val Sharing = booleanPreferencesKey("share.enabled")
        val RequireUnlock = booleanPreferencesKey("share.require_unlock")
    }
}
