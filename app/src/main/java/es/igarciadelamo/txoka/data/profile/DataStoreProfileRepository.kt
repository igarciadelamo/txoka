package es.igarciadelamo.txoka.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.domain.profile.ProfileRepository
import es.igarciadelamo.txoka.domain.profile.RgbColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreProfileRepository(private val dataStore: DataStore<Preferences>) : ProfileRepository {
    override val profile: Flow<Profile?> = dataStore.data.map { preferences -> preferences.toProfile() }

    override suspend fun save(profile: Profile) {
        dataStore.edit { preferences ->
            preferences[Keys.Name] = profile.name
            preferences[Keys.Phone] = profile.phone.orEmpty()
            preferences[Keys.Email] = profile.email.orEmpty()
            preferences[Keys.SharePhone] = profile.sharePhone
            preferences[Keys.ShareEmail] = profile.shareEmail
            preferences[Keys.Accent] = profile.card.accent.hex
            preferences[Keys.Accent2] = profile.card.accent2.hex
        }
    }

    private fun Preferences.toProfile(): Profile? {
        val name = this[Keys.Name]?.takeIf { it.isNotBlank() } ?: return null
        return Profile(
            name = name,
            phone = this[Keys.Phone]?.takeIf { it.isNotBlank() },
            email = this[Keys.Email]?.takeIf { it.isNotBlank() },
            sharePhone = this[Keys.SharePhone] ?: false,
            shareEmail = this[Keys.ShareEmail] ?: false,
            card = CardPalette(
                accent = RgbColor.parse(this[Keys.Accent].orEmpty()) ?: CardPalette.Default.accent,
                accent2 = RgbColor.parse(this[Keys.Accent2].orEmpty()) ?: CardPalette.Default.accent2,
            ),
        )
    }

    private object Keys {
        val Name = stringPreferencesKey("profile.name")
        val Phone = stringPreferencesKey("profile.phone")
        val Email = stringPreferencesKey("profile.email")
        val SharePhone = booleanPreferencesKey("profile.share_phone")
        val ShareEmail = booleanPreferencesKey("profile.share_email")
        val Accent = stringPreferencesKey("profile.card.accent")
        val Accent2 = stringPreferencesKey("profile.card.accent2")
    }
}
