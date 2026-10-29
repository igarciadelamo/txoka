package es.igarciadelamo.txoka.ui.received

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReceivedDetailUiState(
    val contact: ReceivedContact? = null,
    val note: String = "",
    val deleted: Boolean = false,
)

@HiltViewModel
class ReceivedDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ReceivedContactRepository,
) : ViewModel() {
    private val contactId: Long = requireNotNull(savedStateHandle.get<Long>(ARG_ID)) { "Missing contact id" }
    private val noteDraft = MutableStateFlow<String?>(null)
    private val deleted = MutableStateFlow(false)

    val state: StateFlow<ReceivedDetailUiState> = combine(
        repository.contact(contactId),
        noteDraft,
        deleted,
    ) { contact, draft, isDeleted ->
        ReceivedDetailUiState(
            contact = contact,
            note = draft ?: contact?.note.orEmpty(),
            deleted = isDeleted,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ReceivedDetailUiState(),
    )

    fun onNoteChange(value: String) {
        noteDraft.value = value
    }

    fun saveNote() {
        val draft = noteDraft.value ?: state.value.note
        viewModelScope.launch {
            repository.updateNote(contactId, draft)
            noteDraft.value = null
        }
    }

    fun delete() {
        viewModelScope.launch {
            repository.delete(contactId)
            deleted.value = true
        }
    }

    private companion object {
        const val ARG_ID = "id"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
