package es.igarciadelamo.txoka.domain.profile

import kotlinx.coroutines.flow.Flow

/**
 * Controls whether the device answers NFC taps and under which conditions.
 */
data class ShareSettings(val sharingEnabled: Boolean = true, val requireDeviceUnlock: Boolean = false)

interface ShareSettingsRepository {
    val settings: Flow<ShareSettings>

    suspend fun setSharingEnabled(enabled: Boolean)

    suspend fun setRequireDeviceUnlock(enabled: Boolean)
}
