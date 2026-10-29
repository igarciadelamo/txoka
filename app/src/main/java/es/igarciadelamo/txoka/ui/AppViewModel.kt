package es.igarciadelamo.txoka.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.domain.profile.ProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AppState(val isLoading: Boolean = false, val profile: Profile? = null)

@HiltViewModel
class AppViewModel @Inject constructor(repository: ProfileRepository) : ViewModel() {
    val state: StateFlow<AppState> = repository.profile
        .map { profile -> AppState(isLoading = false, profile = profile) }
        .onStart { emit(AppState(isLoading = true)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = AppState(isLoading = true),
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
