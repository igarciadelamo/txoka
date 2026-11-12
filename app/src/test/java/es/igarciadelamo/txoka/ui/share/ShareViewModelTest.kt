package es.igarciadelamo.txoka.ui.share

import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.domain.profile.ShareRole
import es.igarciadelamo.txoka.domain.profile.ShareSettings
import es.igarciadelamo.txoka.nfc.NfcDiagnostics
import es.igarciadelamo.txoka.nfc.NfcStatus
import es.igarciadelamo.txoka.nfc.ShareRoleController
import es.igarciadelamo.txoka.testing.FakeNfcAvailability
import es.igarciadelamo.txoka.testing.FakeProfileRepository
import es.igarciadelamo.txoka.testing.FakeShareSettingsRepository
import es.igarciadelamo.txoka.testing.MainDispatcherRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
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
        val repository = FakeShareSettingsRepository(ShareSettings(requireDeviceUnlock = true))
        val viewModel = ShareViewModel(
            FakeProfileRepository(),
            repository,
            ShareRoleController(backgroundScope),
            FakeNfcAvailability(),
            NfcDiagnostics(),
        )
        advanceUntilIdle()

        assertTrue(viewModel.state.value.requireUnlock)
    }

    @Test
    fun `starts on the send role`() = runTest {
        val viewModel = ShareViewModel(
            FakeProfileRepository(),
            FakeShareSettingsRepository(),
            ShareRoleController(backgroundScope),
            FakeNfcAvailability(),
            NfcDiagnostics(),
        )
        advanceUntilIdle()

        assertEquals(ShareRole.Send, viewModel.state.value.role.role)
        assertFalse(viewModel.state.value.role.returning)
    }

    @Test
    fun `selects the role`() = runTest {
        val roles = ShareRoleController(backgroundScope)
        val viewModel = ShareViewModel(
            FakeProfileRepository(),
            FakeShareSettingsRepository(),
            roles,
            FakeNfcAvailability(),
            NfcDiagnostics(),
        )
        advanceUntilIdle()

        viewModel.onRoleSelected(ShareRole.Receive)
        advanceUntilIdle()

        assertEquals(ShareRole.Receive, viewModel.state.value.role.role)
    }

    @Test
    fun `cancels the return offer`() = runTest {
        val roles = ShareRoleController(backgroundScope)
        val viewModel = ShareViewModel(
            FakeProfileRepository(),
            FakeShareSettingsRepository(),
            roles,
            FakeNfcAvailability(),
            NfcDiagnostics(),
        )
        advanceUntilIdle()

        viewModel.onRoleSelected(ShareRole.Receive)
        roles.onCardReceived()
        runCurrent()
        assertTrue(viewModel.state.value.role.returning)

        viewModel.onCancelReturn()
        runCurrent()

        assertEquals(ShareRole.Receive, viewModel.state.value.role.role)
        assertFalse(viewModel.state.value.role.returning)
    }

    @Test
    fun `refreshes the nfc status on demand`() = runTest {
        val availability = FakeNfcAvailability(NfcStatus.Enabled)
        val viewModel = ShareViewModel(
            FakeProfileRepository(),
            FakeShareSettingsRepository(),
            ShareRoleController(backgroundScope),
            availability,
            NfcDiagnostics(),
        )
        advanceUntilIdle()
        assertEquals(NfcStatus.Enabled, viewModel.state.value.nfc)

        availability.status = NfcStatus.Disabled
        viewModel.refreshNfcStatus()

        assertEquals(NfcStatus.Disabled, viewModel.state.value.nfc)
    }

    @Test
    fun `flags a stored card that no longer fits in the tag`() = runTest {
        val repository = FakeProfileRepository(
            Profile(
                name = "😀".repeat(64),
                phone = "+34600123456",
                email = "a".repeat(115) + "@b.cd",
                linkedin = "a".repeat(60),
                instagram = "b".repeat(60),
            ),
        )
        val viewModel = ShareViewModel(
            repository,
            FakeShareSettingsRepository(),
            ShareRoleController(backgroundScope),
            FakeNfcAvailability(),
            NfcDiagnostics(),
        )
        advanceUntilIdle()

        assertTrue(viewModel.state.value.cardTooLarge)
    }

    @Test
    fun `does not flag a card that fits`() = runTest {
        val repository = FakeProfileRepository(
            Profile(name = "Isidro", phone = "+34600123456", email = "isidro@example.com"),
        )
        val viewModel = ShareViewModel(
            repository,
            FakeShareSettingsRepository(),
            ShareRoleController(backgroundScope),
            FakeNfcAvailability(),
            NfcDiagnostics(),
        )
        advanceUntilIdle()

        assertFalse(viewModel.state.value.cardTooLarge)
    }
}
