package es.igarciadelamo.txoka.ui.received

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.igarciadelamo.txoka.domain.library.LibraryCategory
import es.igarciadelamo.txoka.domain.library.LibraryOrganization
import es.igarciadelamo.txoka.domain.library.LibraryOrganizationRepository
import es.igarciadelamo.txoka.domain.library.LibraryOrganizationSnapshot
import es.igarciadelamo.txoka.domain.library.LibraryViewMode
import es.igarciadelamo.txoka.domain.received.ReceivedContact
import es.igarciadelamo.txoka.domain.received.ReceivedContactRepository
import es.igarciadelamo.txoka.ui.library.LibraryFilter
import es.igarciadelamo.txoka.ui.library.LibrarySection
import es.igarciadelamo.txoka.ui.library.librarySections
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import javax.inject.Inject

data class ReceivedListUiState(
    val query: String = "",
    val contacts: List<ReceivedContact> = emptyList(),
    val isLoading: Boolean = true,
    val hasContacts: Boolean = false,
    val allContacts: List<ReceivedContact> = emptyList(),
    val organization: LibraryOrganization = LibraryOrganization(),
    val sections: List<LibrarySection> = emptyList(),
    val filter: LibraryFilter = LibraryFilter.ALL,
)

sealed interface ReceivedListEvent {
    data object LibraryCleared : ReceivedListEvent
    data object OperationFailed : ReceivedListEvent
    data object RestoreFailed : ReceivedListEvent
}

private data class LibraryRestore(
    val contacts: List<ReceivedContact>,
    val organization: LibraryOrganizationSnapshot? = null,
    val contactsRestored: Boolean = false,
)

@HiltViewModel
class ReceivedListViewModel @Inject constructor(
    private val repository: ReceivedContactRepository,
    private val organizationRepository: LibraryOrganizationRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(LibraryFilter.ALL)
    private var pendingRestores = emptyList<LibraryRestore>()
    private val operationMutex = Mutex()
    private val _events = MutableSharedFlow<ReceivedListEvent>(extraBufferCapacity = 1)

    val events: SharedFlow<ReceivedListEvent> = _events.asSharedFlow()

    val state: StateFlow<ReceivedListUiState> = combine(
        repository.contacts,
        query,
        organizationRepository.organization,
        filter,
    ) { contacts, search, organization, selection ->
        val matches = contacts.filter { contact ->
            contact.matches(search) && (selection != LibraryFilter.FAVORITES || contact.id in organization.favorites)
        }
        val sections = librarySections(matches, organization).map { section ->
            if (selection == LibraryFilter.RECENT) {
                section.copy(contacts = section.contacts.sortedByDescending(ReceivedContact::receivedAt))
            } else {
                section
            }
        }
        val ordered = sections.flatMap { it.contacts }.let {
            if (selection == LibraryFilter.RECENT) it.sortedByDescending(ReceivedContact::receivedAt) else it
        }
        ReceivedListUiState(
            query = search,
            contacts = ordered,
            isLoading = false,
            hasContacts = contacts.isNotEmpty(),
            allContacts = contacts,
            organization = organization,
            sections = sections,
            filter = selection,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ReceivedListUiState())

    init {
        updateLibrary { organizationRepository.prune(repository.contacts.first().map { it.id }.toSet()) }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onFilterChange(value: LibraryFilter) {
        filter.value = value
    }

    fun onViewModeChange(mode: LibraryViewMode) {
        updateLibrary { organizationRepository.setViewMode(mode) }
    }

    fun onSaveCategory(category: LibraryCategory) {
        updateLibrary { organizationRepository.saveCategory(category) }
    }

    fun onDeleteCategory(id: String) {
        updateLibrary { organizationRepository.deleteCategory(id) }
    }

    fun onReorderCategories(ids: List<String>) {
        updateLibrary { organizationRepository.reorderCategories(ids) }
    }

    fun onMoveCard(id: Long, categoryId: String?) {
        updateLibrary { organizationRepository.assign(id, categoryId) }
    }

    fun onReorderCards(categoryId: String?, ids: List<Long>) {
        updateLibrary { organizationRepository.reorderCards(categoryId, ids) }
    }

    fun onFavorite(id: Long, favorite: Boolean) {
        updateLibrary { organizationRepository.setFavorite(id, favorite) }
    }

    fun clearLibrary() {
        updateLibrary {
            val removed = repository.deleteAll()
            if (removed.isEmpty()) return@updateLibrary
            var snapshot = LibraryOrganizationSnapshot()
            val metadataRemoved = attemptLibraryUpdate {
                snapshot = organizationRepository.removeContacts(removed.map { it.id }.toSet())
            }
            if (metadataRemoved) {
                pendingRestores = listOf(LibraryRestore(removed, snapshot))
                _events.tryEmit(ReceivedListEvent.LibraryCleared)
            } else if (attemptLibraryUpdate { repository.restore(removed) }) {
                _events.tryEmit(ReceivedListEvent.OperationFailed)
            } else {
                pendingRestores = listOf(LibraryRestore(removed)) + pendingRestores
                _events.tryEmit(ReceivedListEvent.RestoreFailed)
            }
        }
    }

    fun undoClear() {
        updateLibrary {
            while (pendingRestores.isNotEmpty()) {
                var restore = pendingRestores.first()
                if (!restore.contactsRestored) {
                    if (!attemptLibraryUpdate { repository.restore(restore.contacts) }) {
                        _events.tryEmit(ReceivedListEvent.RestoreFailed)
                        return@updateLibrary
                    }
                    restore = restore.copy(contactsRestored = true)
                    pendingRestores = listOf(restore) + pendingRestores.drop(1)
                }
                val snapshot = restore.organization
                if (snapshot != null && !attemptLibraryUpdate { organizationRepository.restoreContacts(snapshot) }) {
                    _events.tryEmit(ReceivedListEvent.RestoreFailed)
                    return@updateLibrary
                }
                pendingRestores = pendingRestores.drop(1)
            }
        }
    }

    private fun updateLibrary(action: suspend () -> Unit) {
        viewModelScope.launch {
            operationMutex.withLock {
                if (!attemptLibraryUpdate(action)) _events.tryEmit(ReceivedListEvent.OperationFailed)
            }
        }
    }

    private suspend fun attemptLibraryUpdate(action: suspend () -> Unit): Boolean = try {
        action()
        true
    } catch (_: IOException) {
        false
    } catch (_: SQLiteException) {
        false
    } catch (_: IllegalArgumentException) {
        false
    }

    private fun ReceivedContact.matches(search: String): Boolean {
        val text = search.trim()
        return text.isEmpty() ||
            name.contains(text, ignoreCase = true) ||
            phone.orEmpty().contains(text) ||
            email.orEmpty().contains(text, ignoreCase = true) ||
            linkedin.orEmpty().contains(text, ignoreCase = true) ||
            instagram.orEmpty().contains(text, ignoreCase = true)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
