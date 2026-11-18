package es.igarciadelamo.txoka.ui.profile

import es.igarciadelamo.txoka.domain.profile.CardAppearance
import es.igarciadelamo.txoka.domain.profile.CardBackground
import es.igarciadelamo.txoka.domain.profile.CardEffect
import es.igarciadelamo.txoka.domain.profile.CardMosaic
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.CardTextStyle
import es.igarciadelamo.txoka.domain.profile.PhoneCountries
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.domain.profile.ProfileLimits
import es.igarciadelamo.txoka.domain.profile.RgbColor
import es.igarciadelamo.txoka.domain.profile.TextFinish
import es.igarciadelamo.txoka.domain.profile.TextSize
import es.igarciadelamo.txoka.domain.protocol.fitsInTag
import es.igarciadelamo.txoka.domain.protocol.toVCardPayload
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
            Profile(name = "Isidro", phone = "+34600123456"),
        )

        val viewModel = ProfileEditorViewModel(repository, FakeSimRegionSource())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Isidro", state.name)
        assertEquals("+34600123456", state.phone)
        assertTrue(state.isEditing)
    }

    @Test
    fun `saves a normalised phone number and the email`() = runTest {
        val repository = FakeProfileRepository()
        val viewModel = ProfileEditorViewModel(repository, FakeSimRegionSource())

        viewModel.onNameChange("  Isidro García ")
        viewModel.onPhoneChange("600 12 34 56")
        viewModel.onEmailChange("isidro@example.com")
        viewModel.save()
        advanceUntilIdle()

        val saved = repository.savedProfile
        assertEquals("Isidro García", saved?.name)
        assertEquals("+34600123456", saved?.phone)
        assertEquals("isidro@example.com", saved?.email)
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `stores social handles as bare usernames`() = runTest {
        val repository = FakeProfileRepository()
        val viewModel = ProfileEditorViewModel(repository, FakeSimRegionSource())

        viewModel.onNameChange("Isidro")
        viewModel.onEmailChange("isidro@example.com")
        viewModel.onLinkedInChange("https://www.linkedin.com/in/isidro-garcia/")
        viewModel.onInstagramChange("@isidro.png")
        viewModel.save()
        advanceUntilIdle()

        assertEquals("isidro-garcia", repository.savedProfile?.linkedin)
        assertEquals("isidro.png", repository.savedProfile?.instagram)
        assertEquals("isidro-garcia", viewModel.uiState.value.linkedin)
    }

    @Test
    fun `rejects a social handle that is not a username`() = runTest {
        val repository = FakeProfileRepository()
        val viewModel = ProfileEditorViewModel(repository, FakeSimRegionSource())

        viewModel.onNameChange("Isidro")
        viewModel.onEmailChange("isidro@example.com")
        viewModel.onInstagramChange("ana ruiz")
        viewModel.save()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.instagramError)
        assertNull(repository.savedProfile)
    }

    @Test
    fun `loads the stored social handles`() = runTest {
        val repository = FakeProfileRepository(
            Profile(name = "Isidro", linkedin = "isidro-garcia", instagram = "isidro.png"),
        )

        val viewModel = ProfileEditorViewModel(repository, FakeSimRegionSource())
        advanceUntilIdle()

        assertEquals("isidro-garcia", viewModel.uiState.value.linkedin)
        assertEquals("isidro.png", viewModel.uiState.value.instagram)
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
        viewModel.save()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.phoneError)
        assertNull(repository.savedProfile)
    }

    @Test
    fun `enables save once any of the four details is present`() = runTest {
        val viewModel = ProfileEditorViewModel(FakeProfileRepository(), FakeSimRegionSource())

        viewModel.onNameChange("Isidro")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onInstagramChange("isidro.png")
        assertTrue(viewModel.uiState.value.canSave)

        viewModel.onInstagramChange(" ")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onLinkedInChange("isidro-garcia")
        assertTrue(viewModel.uiState.value.canSave)

        viewModel.onLinkedInChange(" ")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onEmailChange("isidro@example.com")
        assertTrue(viewModel.uiState.value.canSave)

        viewModel.onEmailChange(" ")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onPhoneChange("+34600123456")
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `saves a card that only carries a social handle`() = runTest {
        val repository = FakeProfileRepository()
        val viewModel = ProfileEditorViewModel(repository, FakeSimRegionSource())

        viewModel.onNameChange("Marta")
        viewModel.onInstagramChange("@marta.ruiz")
        viewModel.save()
        advanceUntilIdle()

        val saved = repository.savedProfile
        assertEquals("Marta", saved?.name)
        assertEquals("marta.ruiz", saved?.instagram)
        assertNull(saved?.phone)
        assertNull(saved?.email)
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `refuses to save a card without any detail`() = runTest {
        val repository = FakeProfileRepository()
        val viewModel = ProfileEditorViewModel(repository, FakeSimRegionSource())

        viewModel.onNameChange("Marta")
        viewModel.save()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.contactError)
        assertNull(repository.savedProfile)
    }

    @Test
    fun `clears the missing detail error when a detail is typed`() = runTest {
        val viewModel = ProfileEditorViewModel(FakeProfileRepository(), FakeSimRegionSource())

        viewModel.onNameChange("Marta")
        viewModel.save()
        assertTrue(viewModel.uiState.value.contactError)

        viewModel.onInstagramChange("marta.ruiz")

        assertFalse(viewModel.uiState.value.contactError)
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `deletes the stored profile`() = runTest {
        val repository = FakeProfileRepository(Profile(name = "Isidro"))
        val viewModel = ProfileEditorViewModel(repository, FakeSimRegionSource())
        advanceUntilIdle()

        viewModel.delete()
        advanceUntilIdle()

        assertTrue(repository.deleted)
        assertTrue(viewModel.uiState.value.deleted)
    }

    @Test
    fun `stores the selected custom palette`() = runTest {
        val viewModel = ProfileEditorViewModel(FakeProfileRepository(), FakeSimRegionSource())
        val palette = CardPalette.fromAccent(RgbColor(0x10, 0xB9, 0x81))

        viewModel.onCustomColourSelected(palette)

        assertEquals(palette, viewModel.uiState.value.card)
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
    fun `selecting a preset replaces the custom colour`() = runTest {
        val viewModel = ProfileEditorViewModel(FakeProfileRepository(), FakeSimRegionSource())

        viewModel.onCustomColourSelected(CardPalette.fromAccent(RgbColor(0xE1, 0x1D, 0x48)))
        viewModel.onPresetSelected(CardPalette.Presets[1])

        assertEquals(CardPalette.Presets[1], viewModel.uiState.value.card)
    }

    @Test
    fun `truncates the fields at their limits`() = runTest {
        val viewModel = ProfileEditorViewModel(FakeProfileRepository(), FakeSimRegionSource())

        viewModel.onNameChange("N".repeat(ProfileLimits.NAME + 20))
        viewModel.onPhoneChange("6".repeat(ProfileLimits.PHONE + 20))
        viewModel.onEmailChange("e".repeat(ProfileLimits.EMAIL + 20))
        viewModel.onLinkedInChange("l".repeat(ProfileLimits.HANDLE + 20))
        viewModel.onInstagramChange("i".repeat(ProfileLimits.HANDLE + 20))

        val state = viewModel.uiState.value
        assertEquals(ProfileLimits.NAME, state.name.length)
        assertEquals(ProfileLimits.PHONE, state.phone.length)
        assertEquals(ProfileLimits.EMAIL, state.email.length)
        assertEquals(ProfileLimits.HANDLE, state.linkedin.length)
        assertEquals(ProfileLimits.HANDLE, state.instagram.length)
    }

    @Test
    fun `refuses to save a card above the tag limit`() = runTest {
        val repository = FakeProfileRepository()
        val viewModel = ProfileEditorViewModel(repository, FakeSimRegionSource())

        viewModel.onNameChange("😀".repeat(ProfileLimits.NAME))
        viewModel.onPhoneChange("600123456")
        viewModel.onEmailChange("a".repeat(ProfileLimits.EMAIL - 5) + "@b.cd")
        viewModel.onLinkedInChange("a".repeat(ProfileLimits.HANDLE))
        viewModel.onInstagramChange("b".repeat(ProfileLimits.HANDLE))
        viewModel.save()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.cardTooLarge)
        assertFalse(viewModel.uiState.value.canSave)
        assertNull(repository.savedProfile)
        assertFalse(viewModel.uiState.value.saved)
    }

    @Test
    fun `changing curated or custom colours preserves the configured appearance`() = runTest {
        val style = CardAppearance(background = CardBackground.HOLOGRAM, effect = CardEffect.FIRE)
        val viewModel = ProfileEditorViewModel(FakeProfileRepository(), FakeSimRegionSource())
        viewModel.onCardChange(CardPalette.Default.copy(appearance = style))
        viewModel.onPresetSelected(CardPalette.Presets[1])
        assertEquals(style, viewModel.uiState.value.card.appearance)
        assertEquals(CardPalette.Presets[1].accent, viewModel.uiState.value.card.accent)

        val custom = CardPalette.fromAccent(RgbColor(210, 130, 75))
        viewModel.onCustomColourSelected(custom)
        assertEquals(style, viewModel.uiState.value.card.appearance)
        assertEquals(custom.accent, viewModel.uiState.value.card.accent)
    }

    @Test
    fun `saves the full independent appearance unchanged`() = runTest {
        val repository = FakeProfileRepository()
        val viewModel = ProfileEditorViewModel(repository, FakeSimRegionSource())
        val card = richCard()
        viewModel.onNameChange("Ana")
        viewModel.onEmailChange("ana@example.com")
        viewModel.onCardChange(card)
        viewModel.save()
        advanceUntilIdle()

        assertEquals(card, repository.savedProfile?.card)
    }

    @Test
    fun `budgets the canonical phone before saving a near-cap styled card`() = runTest {
        val card = richCard()
        val candidate = (40..64).asSequence().flatMap { length ->
            (1..115).asSequence().map { local ->
                Profile(
                    name = "😀".repeat(length),
                    phone = "600123456",
                    email = "a".repeat(local) + "@b.cd",
                    linkedin = "l".repeat(60),
                    instagram = "i".repeat(60),
                    card = card,
                )
            }
        }.first { it.toVCardPayload().fitsInTag() && !it.copy(phone = "+34600123456").toVCardPayload().fitsInTag() }
        val repository = FakeProfileRepository()
        val viewModel = ProfileEditorViewModel(repository, FakeSimRegionSource(sim = "ES"))
        viewModel.onNameChange(candidate.name)
        viewModel.onPhoneChange(requireNotNull(candidate.phone))
        viewModel.onEmailChange(requireNotNull(candidate.email))
        viewModel.onLinkedInChange(requireNotNull(candidate.linkedin))
        viewModel.onInstagramChange(requireNotNull(candidate.instagram))
        viewModel.onCardChange(card)
        viewModel.save()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.cardTooLarge)
        assertFalse(viewModel.uiState.value.canSave)
        assertNull(repository.savedProfile)
    }

    private fun richCard(): CardPalette = CardPalette.Default.copy(
        appearance = CardAppearance(
            background = CardBackground.HOLOGRAM,
            mosaic = CardMosaic.NATURE_FISH,
            effect = CardEffect.WATER,
            topRight = RgbColor(85, 210, 180),
            bottomLeft = RgbColor(220, 100, 180),
            name = CardTextStyle(TextFinish.METAL, TextSize.XL, RgbColor(210, 180, 90)),
            data = CardTextStyle(TextFinish.RAISED, TextSize.S, RgbColor(245, 245, 250)),
        ),
    )
}
