package es.igarciadelamo.txoka.ui.received

import androidx.lifecycle.SavedStateHandle
import es.igarciadelamo.txoka.domain.received.ReceivedContact
import es.igarciadelamo.txoka.testing.FakeReceivedContactRepository
import es.igarciadelamo.txoka.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

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
    fun `saves a pending draft when the screen is left`() = runTest {
        val repository = FakeReceivedContactRepository(listOf(contact))
        val viewModel = viewModel(repository)
        collect(viewModel)

        viewModel.onNoteChange("New note")
        advanceUntilIdle()
        assertTrue(viewModel.state.value.noteDirty)
        viewModel.flushNote()
        advanceUntilIdle()

        assertEquals("New note", repository.contacts.first().first().note)
    }

    @Test
    fun `an empty draft clears the stored note on exit`() = runTest {
        val repository = FakeReceivedContactRepository(listOf(contact))
        val viewModel = viewModel(repository)
        collect(viewModel)

        viewModel.onNoteChange("")
        viewModel.flushNote()
        advanceUntilIdle()

        assertEquals("", repository.contacts.first().first().note)
        assertEquals(1, repository.noteUpdates)
    }

    @Test
    fun `leaving without changes writes nothing`() = runTest {
        val repository = FakeReceivedContactRepository(listOf(contact))
        val viewModel = viewModel(repository)
        collect(viewModel)

        viewModel.flushNote()
        viewModel.saveNote()
        advanceUntilIdle()

        assertEquals(0, repository.noteUpdates)
    }

    @Test
    fun `the note stops being dirty once it is saved`() = runTest {
        val repository = FakeReceivedContactRepository(listOf(contact))
        val viewModel = viewModel(repository)
        collect(viewModel)

        viewModel.onNoteChange("New note")
        viewModel.saveNote()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.noteDirty)
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

    private fun kotlinx.coroutines.test.TestScope.viewModel(repository: FakeReceivedContactRepository) =
        ReceivedDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("id" to 1L)),
            repository = repository,
            appScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
        )

    private fun kotlinx.coroutines.test.TestScope.collect(viewModel: ReceivedDetailViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
        advanceUntilIdle()
    }
}
