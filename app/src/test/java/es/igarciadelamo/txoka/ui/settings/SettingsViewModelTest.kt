package es.igarciadelamo.txoka.ui.settings

import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.domain.profile.ShareSettings
import es.igarciadelamo.txoka.nfc.FakeTransport
import es.igarciadelamo.txoka.nfc.IncomingCards
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

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `reports whether the user already has a card`() = runTest {
        val viewModel = SettingsViewModel(
            profileRepository = FakeProfileRepository(Profile(name = "Isidro")),
            settingsRepository = FakeShareSettingsRepository(),
            nfcAvailability = FakeNfcAvailability(),
            fakeTransport = FakeTransport(IncomingCards()),
        )
        advanceUntilIdle()

        assertTrue(viewModel.state.value.hasProfile)
    }

    @Test
    fun `reflects the stored sharing settings`() = runTest {
        val viewModel = SettingsViewModel(
            profileRepository = FakeProfileRepository(null),
            settingsRepository = FakeShareSettingsRepository(
                ShareSettings(sharingEnabled = false, requireDeviceUnlock = true),
            ),
            nfcAvailability = FakeNfcAvailability(),
            fakeTransport = FakeTransport(IncomingCards()),
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.hasProfile)
        assertFalse(state.sharingEnabled)
        assertTrue(state.requireUnlock)
    }

    @Test
    fun `persists the toggles`() = runTest {
        val viewModel = SettingsViewModel(
            profileRepository = FakeProfileRepository(null),
            settingsRepository = FakeShareSettingsRepository(),
            nfcAvailability = FakeNfcAvailability(),
            fakeTransport = FakeTransport(IncomingCards()),
        )
        advanceUntilIdle()

        viewModel.onSharingChanged(false)
        viewModel.onRequireUnlockChanged(true)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.sharingEnabled)
        assertTrue(viewModel.state.value.requireUnlock)
    }

    @Test
    fun `refreshes the nfc status`() = runTest {
        val availability = FakeNfcAvailability(NfcStatus.Enabled)
        val viewModel = SettingsViewModel(
            profileRepository = FakeProfileRepository(null),
            settingsRepository = FakeShareSettingsRepository(),
            nfcAvailability = availability,
            fakeTransport = FakeTransport(IncomingCards()),
        )
        advanceUntilIdle()

        availability.status = NfcStatus.Unavailable
        viewModel.refreshNfcStatus()

        assertEquals(NfcStatus.Unavailable, viewModel.state.value.nfc)
    }
}
