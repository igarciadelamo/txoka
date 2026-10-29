package es.igarciadelamo.txoka.ui.received

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.igarciadelamo.txoka.domain.received.ReceivedContact
import es.igarciadelamo.txoka.domain.received.ReceivedContactRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ReceivedListUiState(
    val query: String = "",
    val contacts: List<ReceivedContact> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class ReceivedListViewModel @Inject constructor(repository: ReceivedContactRepository) : ViewModel() {
    private val query = MutableStateFlow("")

    val state: StateFlow<ReceivedListUiState> = combine(repository.contacts, query) { contacts, search ->
        ReceivedListUiState(
            query = search,
            contacts = contacts.filter { contact -> contact.matches(search) },
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ReceivedListUiState(),
    )

    fun onQueryChange(value: String) {
        query.value = value
    }

    private fun ReceivedContact.matches(search: String): Boolean {
        val text = search.trim()
        if (text.isEmpty()) return true
        return name.contains(text, ignoreCase = true) ||
            phone.orEmpty().contains(text) ||
            email.orEmpty().contains(text, ignoreCase = true)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
