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
import es.igarciadelamo.txoka.domain.profile.SimRegionSource
import es.igarciadelamo.txoka.domain.profile.SocialHandle
import es.igarciadelamo.txoka.domain.profile.SocialNetwork
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
    val linkedin: String = "",
    val instagram: String = "",
    val country: PhoneCountry = PhoneCountries.byIsoOrFallback(PhoneCountries.FALLBACK_REGION),
    val card: CardPalette = CardPalette.Default,
    val nameError: Boolean = false,
    val phoneError: Boolean = false,
    val emailError: Boolean = false,
    val linkedinError: Boolean = false,
    val instagramError: Boolean = false,
    val contactError: Boolean = false,
    val isEditing: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false,
) {
    /** True as soon as any of the four detail fields carries text. */
    val hasContact: Boolean
        get() = phone.isNotBlank() || email.isNotBlank() || linkedin.isNotBlank() || instagram.isNotBlank()

    val canSave: Boolean
        get() = name.isNotBlank() && hasContact

    fun toPreview(): Profile = Profile(
        name = name.trim(),
        phone = phone.trim().ifEmpty { null },
        email = email.trim().ifEmpty { null },
        linkedin = SocialHandle.normalize(linkedin, SocialNetwork.LinkedIn),
        instagram = SocialHandle.normalize(instagram, SocialNetwork.Instagram),
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
        _uiState.update { it.copy(phone = value, phoneError = false, contactError = false) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = false, contactError = false) }
    }

    fun onLinkedInChange(value: String) {
        _uiState.update { it.copy(linkedin = value, linkedinError = false, contactError = false) }
    }

    fun onInstagramChange(value: String) {
        _uiState.update { it.copy(instagram = value, instagramError = false, contactError = false) }
    }

    fun onCountrySelected(country: PhoneCountry) {
        _uiState.update { it.copy(country = country, phoneError = false) }
    }

    fun onPresetSelected(palette: CardPalette) {
        _uiState.update { it.copy(card = palette) }
    }

    fun onCustomColourSelected(palette: CardPalette) {
        _uiState.update { it.copy(card = palette) }
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
        val linkedin = SocialHandle.normalize(state.linkedin, SocialNetwork.LinkedIn)
        val instagram = SocialHandle.normalize(state.instagram, SocialNetwork.Instagram)
        val linkedinError = state.linkedin.isNotBlank() && linkedin == null
        val instagramError = state.instagram.isNotBlank() && instagram == null
        // The button already blocks a blank card; this keeps the rule true outside the UI.
        val contactError = !state.hasContact

        val errors = listOf(nameError, phoneError, emailError, linkedinError, instagramError, contactError)
        if (errors.any { it }) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    phoneError = phoneError,
                    emailError = emailError,
                    linkedinError = linkedinError,
                    instagramError = instagramError,
                    contactError = contactError,
                )
            }
            return
        }

        val profile = Profile(
            name = name,
            phone = phone,
            email = email.ifEmpty { null },
            linkedin = linkedin,
            instagram = instagram,
            card = state.card,
        )

        viewModelScope.launch {
            repository.save(profile)
            _uiState.update {
                it.copy(
                    phone = phone.orEmpty(),
                    linkedin = linkedin.orEmpty(),
                    instagram = instagram.orEmpty(),
                    isEditing = true,
                    saved = true,
                )
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            repository.delete()
            _uiState.update { it.copy(deleted = true) }
        }
    }

    private fun ProfileEditorUiState.withProfile(profile: Profile): ProfileEditorUiState = copy(
        name = profile.name,
        phone = profile.phone.orEmpty(),
        email = profile.email.orEmpty(),
        linkedin = profile.linkedin.orEmpty(),
        instagram = profile.instagram.orEmpty(),
        country = profile.phone?.let { PhoneCountries.countryOf(it, country) } ?: country,
        card = profile.card,
        isEditing = true,
    )

    private companion object {
        val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
