package es.igarciadelamo.txoka.ui.received

import androidx.lifecycle.SavedStateHandle
import es.igarciadelamo.txoka.domain.library.LibraryCategory
import es.igarciadelamo.txoka.domain.library.LibraryCategoryIcon
import es.igarciadelamo.txoka.domain.library.LibraryOrganization
import es.igarciadelamo.txoka.domain.library.LibraryOrganizationRepository
import es.igarciadelamo.txoka.domain.library.LibraryOrganizationSnapshot
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.received.ReceivedContact
import es.igarciadelamo.txoka.domain.received.ReceivedContactRepository
import es.igarciadelamo.txoka.testing.FakeLibraryOrganizationRepository
import es.igarciadelamo.txoka.testing.FakeReceivedContactRepository
import es.igarciadelamo.txoka.testing.MainDispatcherRule
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class ReceivedDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val contact = ReceivedContact(id = 1, name = "Marta", note = "Old note")

    @Test
    fun `loads the contact`() = runTest {
        val viewModel = viewModel(FakeReceivedContactRepository(listOf(contact)))
        collect(viewModel)

        assertEquals("Marta", viewModel.state.value.contact?.name)
        assertEquals("Old note", viewModel.state.value.note)
    }

    @Test
    fun `saves the edited note`() = runTest {
        val repository = FakeReceivedContactRepository(listOf(contact))
        val viewModel = viewModel(repository)
        collect(viewModel)

        viewModel.onNoteChange("New note")
        viewModel.saveNote()
        advanceUntilIdle()

        assertEquals("New note", repository.contacts.first().first().note)
    }

    @Test
    fun `deletes the contact`() = runTest {
        val repository = FakeReceivedContactRepository(listOf(contact))
        val viewModel = viewModel(repository)
        collect(viewModel)

        viewModel.delete()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.deleted)
        assertNull(repository.contacts.first().firstOrNull())
    }

    @Test
    fun `deleting a card removes its private organization`() = runTest {
        val category = LibraryCategory("work", "Work", LibraryCategoryIcon.WORK, CardPalette.Default.accent)
        val library = FakeLibraryOrganizationRepository(
            LibraryOrganization(
                categories = listOf(category),
                assignments = mapOf(1L to category.id),
                cardOrder = mapOf(category.id to listOf(1)),
                favorites = setOf(1),
            ),
        )
        val viewModel = ReceivedDetailViewModel(
            SavedStateHandle(mapOf("id" to 1L)),
            FakeReceivedContactRepository(listOf(contact)),
            library,
        )
        collect(viewModel)
        viewModel.delete()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.deleted)
        val organization = library.organization.first()
        assertTrue(organization.assignments.isEmpty())
        assertTrue(organization.favorites.isEmpty())
        assertTrue(organization.cardOrder.isEmpty())
        assertEquals(listOf(category), organization.categories)
    }

    @Test
    fun `note and delete storage errors are reported without navigation`() = runTest {
        val repository = object : ReceivedContactRepository by FakeReceivedContactRepository(listOf(contact)) {
            override suspend fun updateNote(id: Long, note: String) = throw IOException("Disk unavailable")
            override suspend fun delete(id: Long) = throw IllegalArgumentException("Missing card")
        }
        val viewModel = ReceivedDetailViewModel(
            SavedStateHandle(mapOf("id" to 1L)),
            repository,
            FakeLibraryOrganizationRepository(),
        )
        var errors = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.errors.collect { errors++ } }
        collect(viewModel)
        viewModel.onNoteChange("New note")
        viewModel.saveNote()
        viewModel.delete()
        advanceUntilIdle()
        assertEquals(2, errors)
        assertEquals(false, viewModel.state.value.deleted)
        assertEquals("New note", viewModel.state.value.note)
    }

    @Test
    fun `successful Room deletion navigates away even if metadata cleanup fails`() = runTest {
        val library = object : LibraryOrganizationRepository by FakeLibraryOrganizationRepository() {
            override suspend fun removeContacts(ids: Set<Long>): LibraryOrganizationSnapshot =
                throw IOException("Disk unavailable")
        }
        val viewModel = ReceivedDetailViewModel(
            SavedStateHandle(mapOf("id" to 1L)),
            FakeReceivedContactRepository(listOf(contact)),
            library,
        )
        collect(viewModel)
        viewModel.delete()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.deleted)
        assertNull(viewModel.state.value.contact)
    }

    private fun viewModel(repository: FakeReceivedContactRepository) = ReceivedDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf("id" to 1L)),
        repository = repository,
        organizationRepository = FakeLibraryOrganizationRepository(),
    )

    private fun kotlinx.coroutines.test.TestScope.collect(viewModel: ReceivedDetailViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
        advanceUntilIdle()
    }
}
