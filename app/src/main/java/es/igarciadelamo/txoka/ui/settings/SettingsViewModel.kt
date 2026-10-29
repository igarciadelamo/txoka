package es.igarciadelamo.txoka.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.igarciadelamo.txoka.domain.profile.ProfileRepository
import es.igarciadelamo.txoka.domain.profile.ShareSettingsRepository
import es.igarciadelamo.txoka.nfc.NfcAvailability
import es.igarciadelamo.txoka.nfc.NfcStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val hasProfile: Boolean = false,
    val sharingEnabled: Boolean = true,
    val requireUnlock: Boolean = false,
    val nfc: NfcStatus = NfcStatus.Enabled,
    val isLoading: Boolean = true,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    profileRepository: ProfileRepository,
    private val settingsRepository: ShareSettingsRepository,
    private val nfcAvailability: NfcAvailability,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState(nfc = nfcAvailability.status()))
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(profileRepository.profile, settingsRepository.settings) { profile, settings ->
                SettingsUiState(
                    hasProfile = profile != null,
                    sharingEnabled = settings.sharingEnabled,
                    requireUnlock = settings.requireDeviceUnlock,
                    nfc = nfcAvailability.status(),
                    isLoading = false,
                )
            }.collect { state -> _state.value = state }
        }
    }

    fun refreshNfcStatus() {
        _state.update { it.copy(nfc = nfcAvailability.status()) }
    }

    fun onSharingChanged(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSharingEnabled(enabled) }
    }

    fun onRequireUnlockChanged(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setRequireDeviceUnlock(enabled) }
    }
}
