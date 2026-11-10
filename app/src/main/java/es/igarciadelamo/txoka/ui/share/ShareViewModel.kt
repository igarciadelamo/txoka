package es.igarciadelamo.txoka.ui.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.domain.profile.ProfileRepository
import es.igarciadelamo.txoka.domain.profile.ShareRole
import es.igarciadelamo.txoka.domain.profile.ShareSettingsRepository
import es.igarciadelamo.txoka.domain.protocol.fitsInTag
import es.igarciadelamo.txoka.domain.protocol.toVCardPayload
import es.igarciadelamo.txoka.nfc.NfcAvailability
import es.igarciadelamo.txoka.nfc.NfcStatus
import es.igarciadelamo.txoka.nfc.ShareRoleController
import es.igarciadelamo.txoka.nfc.ShareRoleState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShareUiState(
    val profile: Profile? = null,
    val role: ShareRoleState = ShareRoleState(),
    val requireUnlock: Boolean = false,
    val nfc: NfcStatus = NfcStatus.Enabled,
) {
    /**
     * A stored card above the tag limit is published as nothing, so the module explains it instead
     * of waiting for a tap that will never send.
     */
    val cardTooLarge: Boolean
        get() = profile?.toVCardPayload()?.fitsInTag() == false
}

@HiltViewModel
class ShareViewModel @Inject constructor(
    profileRepository: ProfileRepository,
    private val settingsRepository: ShareSettingsRepository,
    private val roles: ShareRoleController,
    private val nfcAvailability: NfcAvailability,
) : ViewModel() {
    private val _state = MutableStateFlow(ShareUiState(nfc = nfcAvailability.status()))
    val state: StateFlow<ShareUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                profileRepository.profile,
                settingsRepository.settings,
                roles.state,
            ) { profile, settings, role ->
                _state.value.copy(
                    profile = profile,
                    role = role,
                    requireUnlock = settings.requireDeviceUnlock,
                )
            }.collect { state -> _state.value = state }
        }
    }

    fun refreshNfcStatus() {
        _state.update { it.copy(nfc = nfcAvailability.status()) }
    }

    fun onRoleSelected(role: ShareRole) {
        roles.select(role)
    }

    fun onCancelReturn() {
        roles.cancelReturn()
    }
}
