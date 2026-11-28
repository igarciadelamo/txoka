package es.igarciadelamo.txoka.ui.received

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.igarciadelamo.txoka.domain.received.ReceivedContact
import es.igarciadelamo.txoka.domain.received.ReceivedContactRepository
import es.igarciadelamo.txoka.domain.received.matchesSearch
import es.igarciadelamo.txoka.nfc.NfcAvailability
import es.igarciadelamo.txoka.nfc.NfcStatus
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
    /** Nothing to receive without a working adapter, so the action stays hidden. */
    val canReceive: Boolean = false,
)

sealed interface ReceivedListEvent {
    data object LibraryCleared : ReceivedListEvent
}

@HiltViewModel
class ReceivedListViewModel @Inject constructor(
    private val repository: ReceivedContactRepository,
    private val deletedContacts: DeletedContactStore,
    private val nfcAvailability: NfcAvailability,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val cleared = MutableStateFlow<List<ReceivedContact>>(emptyList())
    private val _events = MutableSharedFlow<ReceivedListEvent>(extraBufferCapacity = 1)

    val events: SharedFlow<ReceivedListEvent> = _events.asSharedFlow()

    /** The last card removed from its detail screen, waiting for the undo snackbar. */
    val pendingDelete: StateFlow<ReceivedContact?> = deletedContacts.contact

    val state: StateFlow<ReceivedListUiState> = combine(
        repository.contacts,
        query,
        nfcAvailability.stateChanges(),
    ) { contacts, search, nfc ->
        ReceivedListUiState(
            query = search,
            contacts = contacts.filter { contact -> contact.matchesSearch(search) },
            isLoading = false,
            hasContacts = contacts.isNotEmpty(),
            canReceive = nfc == NfcStatus.Enabled,
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
        deletedContacts.clear()
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

    fun undoDelete() {
        val contact = deletedContacts.contact.value ?: return
        viewModelScope.launch {
            repository.restore(listOf(contact))
            deletedContacts.clear()
        }
    }

    fun discardDelete() = deletedContacts.clear()

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
