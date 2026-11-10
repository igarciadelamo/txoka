package es.igarciadelamo.txoka.ui.received

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.igarciadelamo.txoka.domain.received.ReceivedContact
import es.igarciadelamo.txoka.domain.received.ReceivedContactRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReceivedListUiState(
    val query: String = "",
    val contacts: List<ReceivedContact> = emptyList(),
    val isLoading: Boolean = true,
    /** True while the library holds cards, even if the search hides all of them. */
    val hasContacts: Boolean = false,
)

sealed interface ReceivedListEvent {
    data object LibraryCleared : ReceivedListEvent
}

@HiltViewModel
class ReceivedListViewModel @Inject constructor(private val repository: ReceivedContactRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val cleared = MutableStateFlow<List<ReceivedContact>>(emptyList())
    private val _events = MutableSharedFlow<ReceivedListEvent>(extraBufferCapacity = 1)

    val events: SharedFlow<ReceivedListEvent> = _events.asSharedFlow()

    val state: StateFlow<ReceivedListUiState> = combine(repository.contacts, query) { contacts, search ->
        ReceivedListUiState(
            query = search,
            contacts = contacts.filter { contact -> contact.matches(search) },
            isLoading = false,
            hasContacts = contacts.isNotEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ReceivedListUiState(),
    )

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun clearLibrary() {
        viewModelScope.launch {
            cleared.value = repository.deleteAll()
            _events.tryEmit(ReceivedListEvent.LibraryCleared)
        }
    }

    fun undoClear() {
        viewModelScope.launch {
            repository.restore(cleared.value)
            cleared.value = emptyList()
        }
    }

    private fun ReceivedContact.matches(search: String): Boolean {
        val text = search.trim()
        if (text.isEmpty()) return true
        return name.contains(text, ignoreCase = true) ||
            phone.orEmpty().contains(text) ||
            email.orEmpty().contains(text, ignoreCase = true) ||
            linkedin.orEmpty().contains(text, ignoreCase = true) ||
            instagram.orEmpty().contains(text, ignoreCase = true)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
