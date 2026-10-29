package es.igarciadelamo.txoka.ui.received

import es.igarciadelamo.txoka.domain.received.ReceivedContact
import es.igarciadelamo.txoka.testing.FakeReceivedContactRepository
import es.igarciadelamo.txoka.testing.MainDispatcherRule
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReceivedListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val contacts = listOf(
        ReceivedContact(id = 1, name = "Marta Ruiz", phone = "+34600111222", email = "marta@example.com"),
        ReceivedContact(id = 2, name = "Ana López", phone = "+34600999888"),
        ReceivedContact(id = 3, name = "Luis Pérez", email = "luis@example.com"),
    )

    @Test
    fun `lists every contact by default`() = runTest {
        val viewModel = ReceivedListViewModel(FakeReceivedContactRepository(contacts))
        collect(viewModel)

        assertEquals(3, viewModel.state.value.contacts.size)
        assertEquals(false, viewModel.state.value.isLoading)
    }

    @Test
    fun `filters by name, phone and email`() = runTest {
        val viewModel = ReceivedListViewModel(FakeReceivedContactRepository(contacts))
        collect(viewModel)

        viewModel.onQueryChange("ana")
        advanceUntilIdle()
        assertEquals(listOf("Ana López"), viewModel.state.value.contacts.map { it.name })

        viewModel.onQueryChange("600999")
        advanceUntilIdle()
        assertEquals(listOf("Ana López"), viewModel.state.value.contacts.map { it.name })

        viewModel.onQueryChange("LUIS@")
        advanceUntilIdle()
        assertEquals(listOf("Luis Pérez"), viewModel.state.value.contacts.map { it.name })
    }

    @Test
    fun `returns nothing when the query does not match`() = runTest {
        val viewModel = ReceivedListViewModel(FakeReceivedContactRepository(contacts))
        collect(viewModel)

        viewModel.onQueryChange("zzz")
        advanceUntilIdle()

        assertEquals(emptyList<String>(), viewModel.state.value.contacts.map { it.name })
    }

    private fun kotlinx.coroutines.test.TestScope.collect(viewModel: ReceivedListViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
        advanceUntilIdle()
    }
}
