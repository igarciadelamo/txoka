package es.igarciadelamo.txoka.ui.profile

import es.igarciadelamo.txoka.domain.profile.CardPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileEditorUiStateTest {
    @Test
    fun `requires a name and one contact channel`() {
        assertFalse(ProfileEditorUiState(name = "   ", phone = "+34600123456").canSave)
        assertFalse(ProfileEditorUiState(name = "Ana").canSave)
        assertFalse(ProfileEditorUiState(name = "Ana", phone = " ").canSave)
        assertTrue(ProfileEditorUiState(name = "Ana", phone = "+34600123456").canSave)
        assertTrue(ProfileEditorUiState(name = "Ana", email = "ana@example.com").canSave)
    }

    @Test
    fun `trims the preview and drops blank optional fields`() {
        val preview = ProfileEditorUiState(
            name = "  Ana  ",
            phone = " ",
            email = " ana@example.com ",
            card = CardPalette.Presets[3],
        ).toPreview()

        assertEquals("Ana", preview.name)
        assertNull(preview.phone)
        assertEquals("ana@example.com", preview.email)
        assertEquals(CardPalette.Presets[3], preview.card)
    }
}
