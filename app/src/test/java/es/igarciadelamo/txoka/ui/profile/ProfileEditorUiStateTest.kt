package es.igarciadelamo.txoka.ui.profile

import es.igarciadelamo.txoka.domain.profile.CardPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileEditorUiStateTest {
    @Test
    fun `requires a name`() {
        assertFalse(ProfileEditorUiState(name = "   ").canSave)
        assertFalse(ProfileEditorUiState(name = "Ana", customHexError = true).canSave)
        assertTrue(ProfileEditorUiState(name = "Ana").canSave)
    }

    @Test
    fun `trims the preview and drops blank optional fields`() {
        val preview = ProfileEditorUiState(
            name = "  Ana  ",
            phone = " ",
            email = " ana@example.com ",
            shareEmail = true,
            card = CardPalette.Presets[3],
        ).toPreview()

        assertEquals("Ana", preview.name)
        assertNull(preview.phone)
        assertEquals("ana@example.com", preview.email)
        assertTrue(preview.shareEmail)
        assertFalse(preview.sharePhone)
        assertEquals(CardPalette.Presets[3], preview.card)
    }
}
