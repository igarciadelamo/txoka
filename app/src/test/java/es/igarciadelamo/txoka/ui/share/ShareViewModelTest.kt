package es.igarciadelamo.txoka.ui.share

import es.igarciadelamo.txoka.domain.profile.ShareSettings
import es.igarciadelamo.txoka.nfc.NfcStatus
import es.igarciadelamo.txoka.testing.FakeNfcAvailability
import es.igarciadelamo.txoka.testing.FakeProfileRepository
import es.igarciadelamo.txoka.testing.FakeShareSettingsRepository
import es.igarciadelamo.txoka.testing.MainDispatcherRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ShareViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `reflects the stored settings`() = runTest {
        val repository = FakeShareSettingsRepository(ShareSettings(sharingEnabled = false, requireDeviceUnlock = true))
        val viewModel = ShareViewModel(FakeProfileRepository(), repository, FakeNfcAvailability())
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.sharingEnabled)
        assertTrue(state.requireUnlock)
    }

    @Test
    fun `persists the sharing switches`() = runTest {
        val repository = FakeShareSettingsRepository(ShareSettings(sharingEnabled = true))
        val viewModel = ShareViewModel(FakeProfileRepository(), repository, FakeNfcAvailability())
        advanceUntilIdle()

        viewModel.onSharingChanged(false)
        viewModel.onRequireUnlockChanged(true)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.sharingEnabled)
        assertTrue(viewModel.state.value.requireUnlock)
    }

    @Test
    fun `refreshes the nfc status on demand`() = runTest {
        val availability = FakeNfcAvailability(NfcStatus.Enabled)
        val viewModel = ShareViewModel(FakeProfileRepository(), FakeShareSettingsRepository(), availability)
        advanceUntilIdle()
        assertEquals(NfcStatus.Enabled, viewModel.state.value.nfc)

        availability.status = NfcStatus.Disabled
        viewModel.refreshNfcStatus()

        assertEquals(NfcStatus.Disabled, viewModel.state.value.nfc)
    }
}
