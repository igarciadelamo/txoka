package es.igarciadelamo.txoka.domain.library

import es.igarciadelamo.txoka.domain.profile.CardPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LibraryCategoryTest {
    @Test
    fun `accepts a UUID category id and forty character name for a flat background`() {
        val category = LibraryCategory(
            id = "0d064d9c-1362-4bf4-b8cc-54e3df9573b0",
            name = "x".repeat(MAX_CATEGORY_NAME_LENGTH),
            icon = LibraryCategoryIcon.WORK,
            accent = CardPalette.Default.accent,
        )

        assertEquals(MAX_CATEGORY_NAME_LENGTH, category.name.length)
        assertEquals(null, category.accent2)
    }

    @Test
    fun `rejects empty reserved oversized and malformed category ids`() {
        listOf("", " ", "x".repeat(65), "bad/id").forEach { id ->
            assertThrows(IllegalArgumentException::class.java) { category(id = id) }
        }
    }

    @Test
    fun `rejects empty oversized untrimmed and control character names`() {
        listOf("", " ", "x".repeat(MAX_CATEGORY_NAME_LENGTH + 1), " Work", "Work ", "Work\nHome").forEach { name ->
            assertThrows(IllegalArgumentException::class.java) { category(name = name) }
        }
    }

    @Test
    fun `snapshot and organization defaults leave existing contacts uncategorized`() {
        assertEquals(LibraryViewMode.COVER_FLOW, LibraryOrganization().viewMode)
        assertEquals(emptySet<Long>(), LibraryOrganizationSnapshot().contactIds)
        assertEquals("", UNCATEGORIZED_CATEGORY_ID)
    }

    private fun category(id: String = "work", name: String = "Work"): LibraryCategory = LibraryCategory(
        id = id,
        name = name,
        icon = LibraryCategoryIcon.WORK,
        accent = CardPalette.Default.accent,
    )
}
