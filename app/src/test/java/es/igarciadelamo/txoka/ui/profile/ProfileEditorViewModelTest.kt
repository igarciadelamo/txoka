package es.igarciadelamo.txoka.ui.profile

import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.PhoneCountries
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.testing.FakeProfileRepository
import es.igarciadelamo.txoka.testing.FakeSimRegionSource
import es.igarciadelamo.txoka.testing.MainDispatcherRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileEditorViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loads an existing profile for editing`() = runTest {
        val repository = FakeProfileRepository(
            Profile(name = "Isidro", phone = "+34600123456", sharePhone = true),
        )

        val viewModel = ProfileEditorViewModel(repository, FakeSimRegionSource())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Isidro", state.name)
        assertEquals("+34600123456", state.phone)
        assertTrue(state.sharePhone)
        assertTrue(state.isEditing)
    }

    @Test
    fun `saves a normalised phone number and the sharing flags`() = runTest {
        val repository = FakeProfileRepository()
        val viewModel = ProfileEditorViewModel(repository, FakeSimRegionSource())

        viewModel.onNameChange("  Isidro García ")
        viewModel.onPhoneChange("600 12 34 56")
        viewModel.onSharePhoneChange(true)
        viewModel.onEmailChange("isidro@example.com")
        viewModel.save()
        advanceUntilIdle()

        val saved = repository.savedProfile
        assertEquals("Isidro García", saved?.name)
        assertEquals("+34600123456", saved?.phone)
        assertEquals("isidro@example.com", saved?.email)
        assertTrue(saved?.sharePhone == true)
        assertFalse(saved?.shareEmail == true)
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `requires a name before saving`() = runTest {
        val repository = FakeProfileRepository()
        val viewModel = ProfileEditorViewModel(repository, FakeSimRegionSource())

        viewModel.onPhoneChange("+34600123456")
        viewModel.save()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.nameError)
        assertNull(repository.savedProfile)
    }

    @Test
    fun `rejects an invalid phone number`() = runTest {
        val repository = FakeProfileRepository()
        val viewModel = ProfileEditorViewModel(repository, FakeSimRegionSource())

        viewModel.onNameChange("Isidro")
        viewModel.onPhoneChange("123")
        viewModel.onSharePhoneChange(true)
        viewModel.save()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.phoneError)
        assertNull(repository.savedProfile)
    }

    @Test
    fun `clearing a field disables its sharing toggle`() = runTest {
        val viewModel = ProfileEditorViewModel(FakeProfileRepository(), FakeSimRegionSource())

        viewModel.onPhoneChange("+34600123456")
        viewModel.onSharePhoneChange(true)
        assertTrue(viewModel.uiState.value.sharePhone)

        viewModel.onPhoneChange("")
        assertFalse(viewModel.uiState.value.sharePhone)
    }

    @Test
    fun `custom hex updates the palette and flags invalid input`() = runTest {
        val viewModel = ProfileEditorViewModel(FakeProfileRepository(), FakeSimRegionSource())
        viewModel.onNameChange("Isidro")

        viewModel.onCustomHexChange("#10B981")
        val custom = viewModel.uiState.value
        assertEquals("#10B981", custom.card.accent.hex)
        assertFalse(custom.customHexError)
        assertTrue(custom.canSave)

        viewModel.onCustomHexChange("#12")
        assertTrue(viewModel.uiState.value.customHexError)
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `starts from the sim region`() = runTest {
        val viewModel = ProfileEditorViewModel(FakeProfileRepository(), FakeSimRegionSource(sim = "US"))
        advanceUntilIdle()

        assertEquals("US", viewModel.uiState.value.country.iso)
    }

    @Test
    fun `resolves the country of the stored number when editing`() = runTest {
        val repository = FakeProfileRepository(Profile(name = "Isidro", phone = "+33612345678"))
        val viewModel = ProfileEditorViewModel(repository, FakeSimRegionSource(sim = "ES"))
        advanceUntilIdle()

        assertEquals("FR", viewModel.uiState.value.country.iso)
    }

    @Test
    fun `uses the selected country to normalise national numbers`() = runTest {
        val repository = FakeProfileRepository()
        val viewModel = ProfileEditorViewModel(repository, FakeSimRegionSource(sim = "ES"))
        viewModel.onNameChange("Isidro")
        viewModel.onCountrySelected(PhoneCountries.byIsoOrFallback("US"))
        viewModel.onPhoneChange("600123456")
        viewModel.save()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.phoneError)
        assertNull(repository.savedProfile)

        viewModel.onCountrySelected(PhoneCountries.byIsoOrFallback("ES"))
        viewModel.save()
        advanceUntilIdle()

        assertEquals("+34600123456", repository.savedProfile?.phone)
    }

    @Test
    fun `selecting a preset clears the custom colour`() = runTest {
        val viewModel = ProfileEditorViewModel(FakeProfileRepository(), FakeSimRegionSource())

        viewModel.onCustomHexChange("#E11D48")
        viewModel.onPresetSelected(CardPalette.Presets[1])

        val state = viewModel.uiState.value
        assertEquals(CardPalette.Presets[1], state.card)
        assertEquals("", state.customHex)
    }
}
