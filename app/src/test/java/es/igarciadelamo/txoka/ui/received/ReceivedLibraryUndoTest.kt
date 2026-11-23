package es.igarciadelamo.txoka.ui.received

import es.igarciadelamo.txoka.domain.library.LibraryCategory
import es.igarciadelamo.txoka.domain.library.LibraryCategoryIcon
import es.igarciadelamo.txoka.domain.library.LibraryOrganization
import es.igarciadelamo.txoka.domain.library.LibraryOrganizationRepository
import es.igarciadelamo.txoka.domain.library.LibraryOrganizationSnapshot
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import es.igarciadelamo.txoka.domain.received.ReceivedContact
import es.igarciadelamo.txoka.domain.received.ReceivedContactRepository
import es.igarciadelamo.txoka.testing.FakeLibraryOrganizationRepository
import es.igarciadelamo.txoka.testing.FakeReceivedContactRepository
import es.igarciadelamo.txoka.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class ReceivedLibraryUndoTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val category = LibraryCategory("work", "Work", LibraryCategoryIcon.WORK, CardPalette.Default.accent)
    private val contacts = listOf(
        ReceivedContact(1, "Ana", email = "ana@example.com", note = "Private note", receivedAt = 10),
        ReceivedContact(2, "Marta", email = "marta@example.com", receivedAt = 20),
    )
    private val organization = LibraryOrganization(
        categories = listOf(category),
        assignments = mapOf(1L to category.id, 2L to category.id),
        cardOrder = mapOf(category.id to listOf(2, 1)),
        favorites = setOf(1),
    )

    @Test
    fun `undo retries metadata without inserting the same card ids twice`() = runTest {
        val repository = TrackedContacts(FakeReceivedContactRepository(contacts))
        val library = FailingOrganization(FakeLibraryOrganizationRepository(organization))
        val viewModel = ReceivedListViewModel(repository, library)
        val events = observe(viewModel)
        viewModel.clearLibrary()
        advanceUntilIdle()
        library.restoreFailures = 1

        viewModel.undoClear()
        advanceUntilIdle()
        assertEquals(setOf(1L, 2L), repository.contacts.first().map { it.id }.toSet())
        assertEquals(1, repository.restoreAttempts)
        assertTrue(library.organization.first().assignments.isEmpty())
        assertEquals(ReceivedListEvent.RestoreFailed, events.last())

        viewModel.undoClear()
        advanceUntilIdle()
        viewModel.undoClear()
        advanceUntilIdle()
        assertEquals(1, repository.restoreAttempts)
        assertEquals(2, library.restoreAttempts)
        assertEquals(organization, library.organization.first())
        assertEquals(2, repository.contacts.first().size)
        assertEquals("Private note", repository.contacts.first().single { it.id == 1L }.note)
    }

    @Test
    fun `two queued undo requests restore cards only once`() = runTest {
        val repository = TrackedContacts(FakeReceivedContactRepository(contacts))
        val library = FailingOrganization(FakeLibraryOrganizationRepository(organization))
        val viewModel = ReceivedListViewModel(repository, library)
        observe(viewModel)
        viewModel.clearLibrary()
        advanceUntilIdle()
        val gate = CompletableDeferred<Unit>()
        repository.restoreGate = gate

        viewModel.undoClear()
        viewModel.undoClear()
        runCurrent()
        assertEquals(1, repository.restoreAttempts)
        assertTrue(repository.contacts.first().isEmpty())
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.restoreAttempts)
        assertEquals(1, library.restoreAttempts)
        assertEquals(organization, library.organization.first())
        assertEquals(contacts.toSet(), repository.contacts.first().toSet())
    }

    @Test
    fun `a failed later clear keeps the earlier undo and rolls back newly removed cards`() = runTest {
        val repository = TrackedContacts(FakeReceivedContactRepository(contacts))
        val library = FailingOrganization(FakeLibraryOrganizationRepository(organization))
        val viewModel = ReceivedListViewModel(repository, library)
        val events = observe(viewModel)
        viewModel.clearLibrary()
        advanceUntilIdle()
        repository.save(VCardPayload("New arrival", email = "new@example.com"))
        viewModel.onMoveCard(3, category.id)
        viewModel.onFavorite(3, true)
        advanceUntilIdle()
        library.removeFailures = 1

        viewModel.clearLibrary()
        advanceUntilIdle()
        assertEquals(listOf(3L), repository.contacts.first().map { it.id })
        assertEquals(category.id, library.organization.first().assignments[3L])
        assertEquals(ReceivedListEvent.OperationFailed, events.last())
        viewModel.undoClear()
        advanceUntilIdle()

        assertEquals(setOf(1L, 2L, 3L), repository.contacts.first().map { it.id }.toSet())
        val restored = library.organization.first()
        assertEquals(mapOf(1L to category.id, 2L to category.id, 3L to category.id), restored.assignments)
        assertEquals(setOf(1L, 3L), restored.favorites)
        assertEquals(listOf(3L, 2L, 1L), restored.cardOrder[category.id])
        assertEquals(3, repository.contacts.first().size)
    }

    @Test
    fun `failed metadata removal and failed rollback leave a recoverable undo`() = runTest {
        val repository = TrackedContacts(FakeReceivedContactRepository(contacts))
        val library = FailingOrganization(FakeLibraryOrganizationRepository(organization))
        val viewModel = ReceivedListViewModel(repository, library)
        val events = observe(viewModel)
        library.removeFailures = 1
        repository.restoreFailures = 1

        viewModel.clearLibrary()
        advanceUntilIdle()
        assertTrue(repository.contacts.first().isEmpty())
        assertEquals(organization, library.organization.first())
        assertEquals(listOf(ReceivedListEvent.RestoreFailed), events)
        viewModel.undoClear()
        advanceUntilIdle()
        viewModel.undoClear()
        advanceUntilIdle()

        assertEquals(contacts.toSet(), repository.contacts.first().toSet())
        assertEquals(organization, library.organization.first())
        assertEquals(2, repository.restoreAttempts)
        assertEquals(0, library.restoreAttempts)
    }

    private fun TestScope.observe(viewModel: ReceivedListViewModel): List<ReceivedListEvent> {
        val events = mutableListOf<ReceivedListEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect { events += it } }
        advanceUntilIdle()
        return events
    }
}

private class TrackedContacts(private val repository: FakeReceivedContactRepository) :
    ReceivedContactRepository by repository {
    var restoreAttempts = 0
    var restoreFailures = 0
    var restoreGate: CompletableDeferred<Unit>? = null

    override suspend fun restore(contacts: List<ReceivedContact>) {
        restoreAttempts++
        restoreGate?.await()
        if (restoreFailures > 0) {
            restoreFailures--
            throw IOException("Room unavailable")
        }
        repository.restore(contacts)
    }
}

private class FailingOrganization(private val repository: FakeLibraryOrganizationRepository) :
    LibraryOrganizationRepository by repository {
    var removeFailures = 0
    var restoreFailures = 0
    var restoreAttempts = 0

    override suspend fun removeContacts(ids: Set<Long>): LibraryOrganizationSnapshot {
        if (removeFailures > 0) {
            removeFailures--
            throw IOException("DataStore unavailable")
        }
        return repository.removeContacts(ids)
    }

    override suspend fun restoreContacts(snapshot: LibraryOrganizationSnapshot) {
        restoreAttempts++
        if (restoreFailures > 0) {
            restoreFailures--
            throw IOException("DataStore unavailable")
        }
        repository.restoreContacts(snapshot)
    }
}
