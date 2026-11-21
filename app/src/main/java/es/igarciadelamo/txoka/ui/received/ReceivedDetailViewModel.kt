package es.igarciadelamo.txoka.ui.received

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.igarciadelamo.txoka.domain.library.LibraryOrganizationRepository
import es.igarciadelamo.txoka.domain.received.ReceivedContact
import es.igarciadelamo.txoka.domain.received.ReceivedContactRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
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
    private val organizationRepository: LibraryOrganizationRepository,
) : ViewModel() {
    private val contactId: Long = requireNotNull(savedStateHandle.get<Long>(ARG_ID)) { "Missing contact id" }
    private val noteDraft = MutableStateFlow<String?>(null)
    private val deleted = MutableStateFlow(false)
    private val _errors = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val errors = _errors.asSharedFlow()

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
        updateContact {
            repository.updateNote(contactId, draft)
            noteDraft.value = null
        }
    }

    fun delete() {
        updateContact {
            repository.delete(contactId)
            deleted.value = true
            organizationRepository.removeContacts(setOf(contactId))
        }
    }

    private fun updateContact(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
            } catch (_: IOException) {
                _errors.emit(Unit)
            } catch (_: SQLiteException) {
                _errors.emit(Unit)
            } catch (_: IllegalArgumentException) {
                _errors.emit(Unit)
            }
        }
    }

    private companion object {
        const val ARG_ID = "id"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
