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
            linkedin = " https://www.linkedin.com/in/ana-ruiz ",
            instagram = "@ana.ruiz",
            card = CardPalette.Presets[3],
        ).toPreview()

        assertEquals("Ana", preview.name)
        assertNull(preview.phone)
        assertEquals("ana@example.com", preview.email)
        assertEquals("ana-ruiz", preview.linkedin)
        assertEquals("ana.ruiz", preview.instagram)
        assertEquals(CardPalette.Presets[3], preview.card)
    }

    @Test
    fun `drops a social handle the card cannot use`() {
        val preview = ProfileEditorUiState(
            name = "Ana",
            linkedin = "https://instagram.com/ana.ruiz",
        ).toPreview()

        assertNull(preview.linkedin)
    }
}
