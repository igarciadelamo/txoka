package es.igarciadelamo.txoka.ui.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.igarciadelamo.txoka.domain.profile.ShareSettingsRepository
import es.igarciadelamo.txoka.nfc.NfcAvailability
import es.igarciadelamo.txoka.nfc.NfcStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShareUiState(
    val sharingEnabled: Boolean = true,
    val requireUnlock: Boolean = false,
    val nfc: NfcStatus = NfcStatus.Enabled,
)

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val settingsRepository: ShareSettingsRepository,
    private val nfcAvailability: NfcAvailability,
) : ViewModel() {
    private val _state = MutableStateFlow(ShareUiState(nfc = nfcAvailability.status()))
    val state: StateFlow<ShareUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update {
                    it.copy(
                        sharingEnabled = settings.sharingEnabled,
                        requireUnlock = settings.requireDeviceUnlock,
                    )
                }
            }
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
