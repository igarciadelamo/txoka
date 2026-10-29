package es.igarciadelamo.txoka.testing

import es.igarciadelamo.txoka.domain.profile.ShareSettings
import es.igarciadelamo.txoka.domain.profile.ShareSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeShareSettingsRepository(initial: ShareSettings = ShareSettings()) : ShareSettingsRepository {
    private val state = MutableStateFlow(initial)

    override val settings: Flow<ShareSettings> = state

    override suspend fun setSharingEnabled(enabled: Boolean) {
        state.update { it.copy(sharingEnabled = enabled) }
    }

    override suspend fun setRequireDeviceUnlock(enabled: Boolean) {
        state.update { it.copy(requireDeviceUnlock = enabled) }
    }
}
