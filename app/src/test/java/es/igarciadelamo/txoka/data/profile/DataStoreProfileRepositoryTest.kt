package es.igarciadelamo.txoka.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import es.igarciadelamo.txoka.domain.profile.CardAppearance
import es.igarciadelamo.txoka.domain.profile.CardAppearanceCodec
import es.igarciadelamo.txoka.domain.profile.CardBackground
import es.igarciadelamo.txoka.domain.profile.CardEffect
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.CardTextStyle
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.domain.profile.RgbColor
import es.igarciadelamo.txoka.domain.profile.TextFinish
import es.igarciadelamo.txoka.domain.profile.TextSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataStoreProfileRepositoryTest {
    private val appearance = CardAppearance(
        background = CardBackground.HOLOGRAM,
        effect = CardEffect.WATER,
        topRight = RgbColor(40, 80, 120),
        bottomLeft = RgbColor(200, 100, 50),
        name = CardTextStyle(finish = TextFinish.METAL, size = TextSize.XL, color = RgbColor(80, 90, 100)),
        data = CardTextStyle(finish = TextFinish.EMBOSSED, size = TextSize.S, color = RgbColor(210, 220, 230)),
    )
    private val profile = Profile(
        name = "Marta Ruiz",
        phone = "+34600111222",
        email = "marta@example.com",
        linkedin = "marta-ruiz",
        instagram = "marta.ruiz",
        card = CardPalette.Default.copy(appearance = appearance),
    )

    @Test
    fun `saves contact fields palette and appearance`() = runTest {
        val store = MemoryPreferencesDataStore()
        val repository = DataStoreProfileRepository(store)

        repository.save(profile)

        assertEquals(profile, repository.profile.first())
        assertEquals(CardAppearanceCodec.encode(appearance), store.data.first()[appearanceKey])
    }

    @Test
    fun `saving a legacy palette removes the previous appearance`() = runTest {
        val store = MemoryPreferencesDataStore()
        val repository = DataStoreProfileRepository(store)
        repository.save(profile)
        val legacy = profile.copy(card = profile.card.copy(appearance = null))

        repository.save(legacy)

        assertEquals(legacy, repository.profile.first())
        assertNull(store.data.first()[appearanceKey])
    }

    @Test
    fun `reads old profiles with their existing palette and no appearance`() = runTest {
        val preferences = mutablePreferencesOf(
            stringPreferencesKey("profile.name") to "Marta",
            stringPreferencesKey("profile.card.accent") to "#123456",
            stringPreferencesKey("profile.card.accent2") to "#ABCDEF",
        )

        val restored = DataStoreProfileRepository(MemoryPreferencesDataStore(preferences)).profile.first()

        assertEquals("Marta", restored?.name)
        assertEquals(RgbColor(18, 52, 86), restored?.card?.accent)
        assertEquals(RgbColor(171, 205, 239), restored?.card?.accent2)
        assertNull(restored?.card?.appearance)
    }

    @Test
    fun `ignores malformed stored appearance while preserving the profile`() = runTest {
        val store = MemoryPreferencesDataStore()
        val repository = DataStoreProfileRepository(store)
        repository.save(profile)
        store.updateData { preferences ->
            preferences.toMutablePreferences().apply { this[appearanceKey] = "999:not-a-style" }
        }

        assertEquals(profile.copy(card = profile.card.copy(appearance = null)), repository.profile.first())
    }

    @Test
    fun `deleting a profile removes its appearance and keeps share settings`() = runTest {
        val store = MemoryPreferencesDataStore()
        val repository = DataStoreProfileRepository(store)
        repository.save(profile)
        val settings = DataStoreShareSettingsRepository(store)
        settings.setRequireDeviceUnlock(true)

        repository.delete()

        assertNull(repository.profile.first())
        assertFalse(store.data.first().asMap().keys.any { it.name.startsWith("profile.") })
        assertTrue(settings.settings.first().requireDeviceUnlock)
        assertEquals(true, store.data.first()[booleanPreferencesKey("share.require_unlock")])
    }

    @Test
    fun `missing name stays an empty profile even when appearance exists`() = runTest {
        val preferences = mutablePreferencesOf(appearanceKey to CardAppearanceCodec.encode(appearance))

        assertNull(DataStoreProfileRepository(MemoryPreferencesDataStore(preferences)).profile.first())
    }

    private companion object {
        val appearanceKey = stringPreferencesKey("profile.card.appearance")
    }
}

private class MemoryPreferencesDataStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
    private val state = MutableStateFlow(initial)
    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        transform(state.value).also { state.value = it }
}
