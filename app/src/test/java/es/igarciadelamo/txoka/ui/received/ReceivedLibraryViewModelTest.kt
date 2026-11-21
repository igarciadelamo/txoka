package es.igarciadelamo.txoka.ui.received

import es.igarciadelamo.txoka.domain.library.LibraryCategory
import es.igarciadelamo.txoka.domain.library.LibraryCategoryIcon
import es.igarciadelamo.txoka.domain.library.LibraryOrganization
import es.igarciadelamo.txoka.domain.library.LibraryOrganizationRepository
import es.igarciadelamo.txoka.domain.library.LibraryViewMode
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import es.igarciadelamo.txoka.domain.received.ReceivedContact
import es.igarciadelamo.txoka.domain.received.ReceivedContactRepository
import es.igarciadelamo.txoka.testing.FakeLibraryOrganizationRepository
import es.igarciadelamo.txoka.testing.FakeReceivedContactRepository
import es.igarciadelamo.txoka.testing.MainDispatcherRule
import es.igarciadelamo.txoka.ui.library.LibraryFilter
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class ReceivedLibraryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val category = LibraryCategory("work", "Work", LibraryCategoryIcon.WORK, CardPalette.Default.accent)
    private val contacts = listOf(
        ReceivedContact(1, "Ana", email = "ana@example.com", receivedAt = 10),
        ReceivedContact(2, "Marta", email = "marta@example.com", receivedAt = 30),
        ReceivedContact(3, "Luis", phone = "600111222", receivedAt = 20),
    )
    private val organization = LibraryOrganization(
        categories = listOf(category),
        assignments = mapOf(1L to category.id, 2L to category.id),
        cardOrder = mapOf(category.id to listOf(2, 1)),
        favorites = setOf(1),
    )

    @Test
    fun `categories and manual order survive search and sender updates`() = runTest {
        val repository = FakeReceivedContactRepository(contacts)
        val viewModel = ReceivedListViewModel(repository, FakeLibraryOrganizationRepository(organization))
        collect(viewModel)
        assertEquals(listOf(2L, 1L, 3L), viewModel.state.value.contacts.map { it.id })
        assertEquals(listOf(2L, 1L), viewModel.state.value.sections.first().contacts.map { it.id })

        viewModel.onQueryChange("ANA@")
        advanceUntilIdle()
        assertEquals(listOf(1L), viewModel.state.value.contacts.map { it.id })
        assertEquals(3, viewModel.state.value.allContacts.size)
        repository.save(VCardPayload("New sender name", email = "ana@example.com"))
        viewModel.onQueryChange("")
        advanceUntilIdle()
        assertEquals(category.id, viewModel.state.value.organization.assignments[1L])
        assertEquals("New sender name", viewModel.state.value.contacts[1].name)
    }

    @Test
    fun `favorite and recent filters preserve complete underlying library`() = runTest {
        val viewModel = ReceivedListViewModel(
            FakeReceivedContactRepository(contacts),
            FakeLibraryOrganizationRepository(organization.copy(cardOrder = mapOf(category.id to listOf(1, 2)))),
        )
        collect(viewModel)
        viewModel.onFilterChange(LibraryFilter.FAVORITES)
        advanceUntilIdle()
        assertEquals(listOf(1L), viewModel.state.value.contacts.map { it.id })
        viewModel.onQueryChange("no match")
        advanceUntilIdle()
        assertTrue(viewModel.state.value.hasContacts)
        assertTrue(viewModel.state.value.contacts.isEmpty())
        viewModel.onQueryChange("")
        viewModel.onFilterChange(LibraryFilter.RECENT)
        advanceUntilIdle()
        assertEquals(listOf(2L, 3L, 1L), viewModel.state.value.contacts.map { it.id })
        assertEquals(listOf(2L, 1L), viewModel.state.value.sections.first().contacts.map { it.id })
        viewModel.onFilterChange(LibraryFilter.ALL)
        advanceUntilIdle()
        assertEquals(listOf(1L, 2L, 3L), viewModel.state.value.contacts.map { it.id })
    }

    @Test
    fun `organization actions update categories order favorites and mode`() = runTest {
        val library = FakeLibraryOrganizationRepository()
        val viewModel = ReceivedListViewModel(FakeReceivedContactRepository(contacts), library)
        collect(viewModel)
        viewModel.onSaveCategory(category)
        advanceUntilIdle()
        viewModel.onMoveCard(1, category.id)
        viewModel.onMoveCard(2, category.id)
        viewModel.onFavorite(2, true)
        viewModel.onViewModeChange(LibraryViewMode.SHELVES)
        advanceUntilIdle()
        viewModel.onReorderCards(category.id, listOf(2, 1))
        advanceUntilIdle()
        assertEquals(listOf(2L, 1L), viewModel.state.value.sections.first().contacts.map { it.id })
        assertEquals(setOf(2L), viewModel.state.value.organization.favorites)
        assertEquals(LibraryViewMode.SHELVES, viewModel.state.value.organization.viewMode)
        viewModel.onDeleteCategory(category.id)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.organization.categories.isEmpty())
        assertTrue(viewModel.state.value.organization.assignments.isEmpty())
        assertEquals(3, viewModel.state.value.contacts.size)
    }

    @Test
    fun `custom category order persists and uncategorized remains last`() = runTest {
        val other = category.copy(id = "study", name = "Study", icon = LibraryCategoryIcon.STUDY)
        val library = FakeLibraryOrganizationRepository(organization.copy(categories = listOf(category, other)))
        val repository = FakeReceivedContactRepository(contacts)
        val viewModel = ReceivedListViewModel(repository, library)
        collect(viewModel)
        viewModel.onReorderCategories(listOf(other.id, category.id))
        advanceUntilIdle()
        assertEquals(listOf(other, category), viewModel.state.value.organization.categories)
        val reopened = ReceivedListViewModel(repository, library)
        collect(reopened)
        assertEquals(listOf(other, category, null), reopened.state.value.sections.map { it.category })
        assertEquals(listOf(2L, 1L), reopened.state.value.sections[1].contacts.map { it.id })
    }

    @Test
    fun `clear and undo restore ids categories order favorites and notes`() = runTest {
        val repository = FakeReceivedContactRepository(contacts.map { it.copy(note = "Private note") })
        val library = FakeLibraryOrganizationRepository(organization)
        val viewModel = ReceivedListViewModel(repository, library)
        collect(viewModel)
        viewModel.clearLibrary()
        advanceUntilIdle()
        assertTrue(library.organization.first().assignments.isEmpty())
        assertTrue(library.organization.first().favorites.isEmpty())
        assertEquals(listOf(category), library.organization.first().categories)
        viewModel.undoClear()
        advanceUntilIdle()
        assertEquals(organization, library.organization.first())
        assertEquals(listOf(2L, 1L, 3L), viewModel.state.value.contacts.map { it.id })
        assertTrue(viewModel.state.value.contacts.all { it.note == "Private note" })
        viewModel.undoClear()
        advanceUntilIdle()
        assertEquals(3, repository.contacts.first().size)
    }

    @Test
    fun `opening library prunes only unavailable ids from local metadata`() = runTest {
        val library = FakeLibraryOrganizationRepository(
            organization.copy(assignments = organization.assignments + (99L to category.id), favorites = setOf(1, 99)),
        )
        val viewModel = ReceivedListViewModel(FakeReceivedContactRepository(contacts), library)
        collect(viewModel)
        assertFalse(99L in viewModel.state.value.organization.assignments)
        assertEquals(setOf(1L), viewModel.state.value.organization.favorites)
    }

    @Test
    fun `failed clear restores metadata and reports an error`() = runTest {
        val repository = object : ReceivedContactRepository by FakeReceivedContactRepository(contacts) {
            override suspend fun deleteAll(): List<ReceivedContact> = throw IOException("Disk unavailable")
        }
        val library = FakeLibraryOrganizationRepository(organization)
        val viewModel = ReceivedListViewModel(repository, library)
        val events = errors(viewModel)
        collect(viewModel)
        viewModel.clearLibrary()
        advanceUntilIdle()
        assertEquals(organization, library.organization.first())
        assertEquals(3, viewModel.state.value.contacts.size)
        assertEquals(listOf(ReceivedListEvent.OperationFailed), events)
    }

    @Test
    fun `invalid organization and unavailable storage report errors without changing cards`() = runTest {
        val library = object : LibraryOrganizationRepository by FakeLibraryOrganizationRepository() {
            override suspend fun setViewMode(mode: LibraryViewMode) = throw IOException("Disk unavailable")
            override suspend fun assign(contactId: Long, categoryId: String?) =
                throw IllegalArgumentException("Missing")
        }
        val viewModel = ReceivedListViewModel(FakeReceivedContactRepository(contacts), library)
        val events = errors(viewModel)
        collect(viewModel)
        viewModel.onViewModeChange(LibraryViewMode.LIST)
        viewModel.onMoveCard(1, "missing")
        advanceUntilIdle()
        assertEquals(List(2) { ReceivedListEvent.OperationFailed }, events)
        assertEquals(3, viewModel.state.value.contacts.size)
    }

    private fun TestScope.collect(viewModel: ReceivedListViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
        advanceUntilIdle()
    }

    private fun TestScope.errors(viewModel: ReceivedListViewModel): List<ReceivedListEvent> {
        val events = mutableListOf<ReceivedListEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect { events += it } }
        return events
    }
}
