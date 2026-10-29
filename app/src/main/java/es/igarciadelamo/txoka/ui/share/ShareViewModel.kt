package es.igarciadelamo.txoka.ui.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.igarciadelamo.txoka.domain.profile.Profile
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

data class ShareUiState(
    val profile: Profile? = null,
    val sharingEnabled: Boolean = true,
    val requireUnlock: Boolean = false,
    val nfc: NfcStatus = NfcStatus.Enabled,
)

@HiltViewModel
class ShareViewModel @Inject constructor(
    profileRepository: ProfileRepository,
    private val settingsRepository: ShareSettingsRepository,
    private val nfcAvailability: NfcAvailability,
) : ViewModel() {
    private val _state = MutableStateFlow(ShareUiState(nfc = nfcAvailability.status()))
    val state: StateFlow<ShareUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(profileRepository.profile, settingsRepository.settings) { profile, settings ->
                _state.value.copy(
                    profile = profile,
                    sharingEnabled = settings.sharingEnabled,
                    requireUnlock = settings.requireDeviceUnlock,
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
