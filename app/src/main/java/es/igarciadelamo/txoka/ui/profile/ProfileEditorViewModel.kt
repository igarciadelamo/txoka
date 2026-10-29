package es.igarciadelamo.txoka.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.PhoneCountries
import es.igarciadelamo.txoka.domain.profile.PhoneCountry
import es.igarciadelamo.txoka.domain.profile.PhoneNumbers
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.domain.profile.ProfileRepository
import es.igarciadelamo.txoka.domain.profile.RgbColor
import es.igarciadelamo.txoka.domain.profile.SimRegionSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class ProfileEditorUiState(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val country: PhoneCountry = PhoneCountries.byIsoOrFallback(PhoneCountries.FALLBACK_REGION),
    val sharePhone: Boolean = false,
    val shareEmail: Boolean = false,
    val card: CardPalette = CardPalette.Default,
    val customHex: String = "",
    val nameError: Boolean = false,
    val phoneError: Boolean = false,
    val emailError: Boolean = false,
    val customHexError: Boolean = false,
    val isEditing: Boolean = false,
    val saved: Boolean = false,
) {
    val canSave: Boolean
        get() = name.isNotBlank() && !customHexError

    fun toPreview(): Profile = Profile(
        name = name.trim(),
        phone = phone.trim().ifEmpty { null },
        email = email.trim().ifEmpty { null },
        sharePhone = sharePhone,
        shareEmail = shareEmail,
        card = card,
    )
}

@HiltViewModel
class ProfileEditorViewModel @Inject constructor(
    private val repository: ProfileRepository,
    regionSource: SimRegionSource,
) : ViewModel() {
    private val deviceCountry: PhoneCountry = PhoneCountries.byIsoOrFallback(
        PhoneCountries.resolveRegion(
            simRegion = regionSource.simRegion(),
            networkRegion = regionSource.networkRegion(),
            localeRegion = Locale.getDefault().country,
        ),
    )

    private val _uiState = MutableStateFlow(ProfileEditorUiState(country = deviceCountry))
    val uiState: StateFlow<ProfileEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.profile.first()?.let { profile -> _uiState.update { it.withProfile(profile) } }
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, nameError = false) }
    }

    fun onPhoneChange(value: String) {
        _uiState.update {
            it.copy(
                phone = value,
                sharePhone = it.sharePhone && value.isNotBlank(),
                phoneError = false,
            )
        }
    }

    fun onEmailChange(value: String) {
        _uiState.update {
            it.copy(
                email = value,
                shareEmail = it.shareEmail && value.isNotBlank(),
                emailError = false,
            )
        }
    }

    fun onCountrySelected(country: PhoneCountry) {
        _uiState.update { it.copy(country = country, phoneError = false) }
    }

    fun onSharePhoneChange(enabled: Boolean) {
        _uiState.update { it.copy(sharePhone = enabled && it.phone.isNotBlank()) }
    }

    fun onShareEmailChange(enabled: Boolean) {
        _uiState.update { it.copy(shareEmail = enabled && it.email.isNotBlank()) }
    }

    fun onPresetSelected(palette: CardPalette) {
        _uiState.update {
            it.copy(card = palette, customHex = "", customHexError = false)
        }
    }

    fun onCustomHexChange(value: String) {
        val parsed = RgbColor.parse(value)
        _uiState.update {
            it.copy(
                customHex = value,
                customHexError = value.isNotBlank() && parsed == null,
                card = parsed?.let(CardPalette::fromAccent) ?: it.card,
            )
        }
    }

    fun save() {
        val state = _uiState.value
        val name = state.name.trim()
        val email = state.email.trim()
        val rawPhone = state.phone.trim()
        val phone = if (rawPhone.isEmpty()) null else PhoneNumbers.toE164(rawPhone, state.country.iso)

        val nameError = name.isEmpty()
        val phoneError = rawPhone.isNotEmpty() && phone == null
        val emailError = email.isNotEmpty() && !EMAIL_PATTERN.matches(email)

        if (nameError || phoneError || emailError) {
            _uiState.update {
                it.copy(nameError = nameError, phoneError = phoneError, emailError = emailError)
            }
            return
        }

        val profile = Profile(
            name = name,
            phone = phone,
            email = email.ifEmpty { null },
            sharePhone = state.sharePhone && phone != null,
            shareEmail = state.shareEmail && email.isNotEmpty(),
            card = state.card,
        )

        viewModelScope.launch {
            repository.save(profile)
            _uiState.update {
                it.copy(
                    phone = phone.orEmpty(),
                    sharePhone = profile.sharePhone,
                    shareEmail = profile.shareEmail,
                    isEditing = true,
                    saved = true,
                )
            }
        }
    }

    private fun ProfileEditorUiState.withProfile(profile: Profile): ProfileEditorUiState = copy(
        name = profile.name,
        phone = profile.phone.orEmpty(),
        email = profile.email.orEmpty(),
        country = profile.phone?.let { PhoneCountries.countryOf(it, country) } ?: country,
        sharePhone = profile.sharePhone,
        shareEmail = profile.shareEmail,
        card = profile.card,
        isEditing = true,
    )

    private companion object {
        val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
