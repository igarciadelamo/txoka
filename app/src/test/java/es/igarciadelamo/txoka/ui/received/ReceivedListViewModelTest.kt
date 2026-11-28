package es.igarciadelamo.txoka.ui.received

import es.igarciadelamo.txoka.domain.received.ReceivedContact
import es.igarciadelamo.txoka.nfc.NfcStatus
import es.igarciadelamo.txoka.testing.FakeNfcAvailability
import es.igarciadelamo.txoka.testing.FakeReceivedContactRepository
import es.igarciadelamo.txoka.testing.MainDispatcherRule
import kotlinx.coroutines.flow.collect
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

class ReceivedListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val contacts = listOf(
        ReceivedContact(
            id = 1,
            name = "Marta Ruiz",
            phone = "+34600111222",
            email = "marta@example.com",
            linkedin = "marta-ruiz",
            instagram = "marta.png",
            note = "Met at the meetup",
        ),
        ReceivedContact(id = 2, name = "Ana López", phone = "+34600999888"),
        ReceivedContact(id = 3, name = "Luis Pérez", email = "luis@example.com"),
    )

    @Test
    fun `lists every contact by default`() = runTest {
        val viewModel = viewModel(FakeReceivedContactRepository(contacts))
        collect(viewModel)

        assertEquals(3, viewModel.state.value.names.size)
        assertEquals(ReceivedListItem.Header(ReceivedGroup.Earlier), viewModel.state.value.items.first())
        assertEquals(false, viewModel.state.value.isLoading)
    }

    @Test
    fun `keeps the stored count while the search filters the list`() = runTest {
        val viewModel = viewModel(FakeReceivedContactRepository(contacts))
        collect(viewModel)

        viewModel.onQueryChange("ana")
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.names.size)
        assertEquals(3, viewModel.state.value.libraryCount)
    }

    @Test
    fun `filters by name, phone and email`() = runTest {
        val viewModel = viewModel(FakeReceivedContactRepository(contacts))
        collect(viewModel)

        viewModel.onQueryChange("ana")
        advanceUntilIdle()
        assertEquals(listOf("Ana López"), viewModel.state.value.names)

        viewModel.onQueryChange("600999")
        advanceUntilIdle()
        assertEquals(listOf("Ana López"), viewModel.state.value.names)

        viewModel.onQueryChange("LUIS@")
        advanceUntilIdle()
        assertEquals(listOf("Luis Pérez"), viewModel.state.value.names)

        viewModel.onQueryChange("PNG")
        advanceUntilIdle()
        assertEquals(listOf("Marta Ruiz"), viewModel.state.value.names)

        viewModel.onQueryChange("-ruiz")
        advanceUntilIdle()
        assertEquals(listOf("Marta Ruiz"), viewModel.state.value.names)
    }

    @Test
    fun `ignores accents, phone separators and searches notes`() = runTest {
        val viewModel = viewModel(FakeReceivedContactRepository(contacts))
        collect(viewModel)

        viewModel.onQueryChange("lopez")
        advanceUntilIdle()
        assertEquals(listOf("Ana López"), viewModel.state.value.names)

        viewModel.onQueryChange("600 999 888")
        advanceUntilIdle()
        assertEquals(listOf("Ana López"), viewModel.state.value.names)

        viewModel.onQueryChange("meetup")
        advanceUntilIdle()
        assertEquals(listOf("Marta Ruiz"), viewModel.state.value.names)
    }

    @Test
    fun `returns nothing when the query does not match`() = runTest {
        val viewModel = viewModel(FakeReceivedContactRepository(contacts))
        collect(viewModel)

        viewModel.onQueryChange("zzz")
        advanceUntilIdle()

        assertEquals(emptyList<String>(), viewModel.state.value.names)
        assertEquals(true, viewModel.state.value.hasContacts)
    }

    @Test
    fun `empties the library and puts it back on undo`() = runTest {
        val viewModel = viewModel(FakeReceivedContactRepository(contacts))
        val events = mutableListOf<ReceivedListEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { event -> events += event }
        }
        collect(viewModel)

        viewModel.clearLibrary()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.names.isEmpty())
        assertEquals(false, viewModel.state.value.hasContacts)
        assertEquals(listOf(ReceivedListEvent.LibraryCleared), events)

        viewModel.undoClear()
        advanceUntilIdle()

        assertEquals(3, viewModel.state.value.names.size)
        assertEquals(true, viewModel.state.value.hasContacts)
    }

    @Test
    fun `undoes a single delete`() = runTest {
        val repository = FakeReceivedContactRepository(contacts)
        val deleted = DeletedContactStore()
        val viewModel = viewModel(repository, deleted)
        collect(viewModel)

        deleted.remember(contacts.first())
        advanceUntilIdle()
        assertEquals(contacts.first(), viewModel.pendingDelete.value)

        viewModel.undoDelete()
        advanceUntilIdle()

        assertNull(viewModel.pendingDelete.value)
        assertEquals(4, viewModel.state.value.names.size)
        assertTrue(viewModel.state.value.names.contains("Marta Ruiz"))
    }

    @Test
    fun `discarding the delete clears the undo`() = runTest {
        val repository = FakeReceivedContactRepository(contacts)
        val deleted = DeletedContactStore()
        val viewModel = viewModel(repository, deleted)
        collect(viewModel)

        deleted.remember(contacts.first())
        viewModel.discardDelete()
        advanceUntilIdle()

        assertNull(viewModel.pendingDelete.value)
        assertEquals(3, viewModel.state.value.names.size)
    }

    @Test
    fun `emptying the library drops a pending single delete`() = runTest {
        val repository = FakeReceivedContactRepository(contacts)
        val deleted = DeletedContactStore()
        val viewModel = viewModel(repository, deleted)
        collect(viewModel)

        deleted.remember(contacts.first())
        viewModel.clearLibrary()
        advanceUntilIdle()

        assertNull(viewModel.pendingDelete.value)
    }

    @Test
    fun `offers the receive action only while nfc is on`() = runTest {
        val nfc = FakeNfcAvailability(NfcStatus.Enabled)
        val viewModel = viewModel(FakeReceivedContactRepository(emptyList()), nfc = nfc)
        collect(viewModel)
        assertTrue(viewModel.state.value.canReceive)

        nfc.status = NfcStatus.Disabled
        advanceUntilIdle()
        assertFalse(viewModel.state.value.canReceive)

        nfc.status = NfcStatus.Unavailable
        advanceUntilIdle()
        assertFalse(viewModel.state.value.canReceive)
    }

    private fun kotlinx.coroutines.test.TestScope.viewModel(
        repository: FakeReceivedContactRepository,
        deleted: DeletedContactStore = DeletedContactStore(),
        nfc: FakeNfcAvailability = FakeNfcAvailability(),
    ) = ReceivedListViewModel(repository, deleted, nfc)

    private val ReceivedListUiState.names: List<String>
        get() = items.filterIsInstance<ReceivedListItem.Card>().map { it.contact.name }

    private fun kotlinx.coroutines.test.TestScope.collect(viewModel: ReceivedListViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
        advanceUntilIdle()
    }
}
