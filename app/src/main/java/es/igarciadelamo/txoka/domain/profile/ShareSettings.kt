package es.igarciadelamo.txoka.domain.profile

import kotlinx.coroutines.flow.Flow

/**
 * Conditions that apply whenever the device answers NFC taps.
 */
data class ShareSettings(val requireDeviceUnlock: Boolean = false)

interface ShareSettingsRepository {
    val settings: Flow<ShareSettings>

    suspend fun setRequireDeviceUnlock(enabled: Boolean)
}
