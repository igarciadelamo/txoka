package es.igarciadelamo.txoka.ui

import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.testing.FakeProfileRepository
import es.igarciadelamo.txoka.testing.MainDispatcherRule
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `starts loading and ends with the stored profile`() = runTest {
        val viewModel = AppViewModel(FakeProfileRepository(Profile(name = "Isidro")))

        assertTrue(viewModel.state.value.isLoading)

        collect(viewModel)
        advanceUntilIdle()

        assertEquals("Isidro", viewModel.state.value.profile?.name)
        assertEquals(false, viewModel.state.value.isLoading)
    }

    @Test
    fun `reports no profile on a fresh install`() = runTest {
        val viewModel = AppViewModel(FakeProfileRepository(null))

        collect(viewModel)
        advanceUntilIdle()

        assertNull(viewModel.state.value.profile)
        assertEquals(false, viewModel.state.value.isLoading)
    }

    private fun TestScope.collect(viewModel: AppViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
    }
}
