package es.igarciadelamo.txoka.data.library

import es.igarciadelamo.txoka.domain.library.LibraryCategory
import es.igarciadelamo.txoka.domain.library.LibraryCategoryIcon
import es.igarciadelamo.txoka.domain.library.LibraryOrganization
import es.igarciadelamo.txoka.domain.library.LibraryViewMode
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.RgbColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryOrganizationCodecTest {
    @Test
    fun `round trip retains category order both backgrounds assignments favorites and modes`() {
        LibraryViewMode.entries.forEach { mode ->
            val categories = LibraryCategoryIcon.entries.map { icon ->
                LibraryCategory(
                    id = icon.name,
                    name = "Café ${icon.name}",
                    icon = icon,
                    accent = RgbColor(12, 34, 56),
                    accent2 = if (icon == LibraryCategoryIcon.WORK) RgbColor(210, 220, 230) else null,
                )
            }
            val organization = LibraryOrganization(
                categories = categories,
                assignments = mapOf(1L to "WORK", 2L to "WORK"),
                cardOrder = mapOf("WORK" to listOf(2L, 1L), "" to listOf(3L)),
                favorites = setOf(1L, 3L),
                viewMode = mode,
            )

            val encoded = LibraryOrganizationCodec.encode(organization)

            assertEquals(organization, LibraryOrganizationCodec.decode(encoded))
            assertTrue(encoded.contains("\"version\":1"))
            assertFalse(encoded.contains("X-TXOKA"))
        }
    }

    @Test
    fun `missing malformed or unsupported state starts with an empty private library`() {
        listOf(null, "", "garbage", "[]", "{", "{}", "{\"version\":9}", "{\"version\":1,\"favorites\":false}")
            .forEach { encoded -> assertEquals(LibraryOrganization(), LibraryOrganizationCodec.decode(encoded)) }
    }

    @Test
    fun `invalid local category fields have safe defaults without discarding valid categories`() {
        val encoded = """
            {"version":1,"categories":[
              {"id":"work","name":"  Work  ","icon":"future","accent":"broken","accent2":"broken"},
              {"id":"","name":"Uncategorized"},
              {"id":"bad","name":"\n"},
              {"id":"work","name":"Duplicate"}
            ],"viewMode":"future"}
        """.trimIndent()

        val decoded = LibraryOrganizationCodec.decode(encoded)

        assertEquals(1, decoded.categories.size)
        assertEquals("Work", decoded.categories.single().name)
        assertEquals(LibraryCategoryIcon.OTHER, decoded.categories.single().icon)
        assertEquals(CardPalette.Default.accent, decoded.categories.single().accent)
        assertNull(decoded.categories.single().accent2)
        assertEquals(LibraryViewMode.COVER_FLOW, decoded.viewMode)
    }

    @Test
    fun `drops invalid contact ids dangling categories duplicate order and incorrect shelf entries`() {
        val encoded = """
            {"version":1,"categories":[{"id":"work","name":"Work"}],
             "assignments":{"1":"work","2":"gone","-1":"work","overflow":"work"},
             "cardOrder":{"work":[1,1,2,-1],"gone":[2],"":[2,1,3,3,0]},
             "favorites":[1,1,3,0,-1]}
        """.trimIndent()

        val decoded = LibraryOrganizationCodec.decode(encoded)

        assertEquals(mapOf(1L to "work"), decoded.assignments)
        assertEquals(mapOf("work" to listOf(1L), "" to listOf(2L, 3L)), decoded.cardOrder)
        assertEquals(setOf(1L, 3L), decoded.favorites)
    }

    @Test
    fun `ignores unknown fields and keeps arbitrary category punctuation inside its name`() {
        val encoded = """
            {"version":1,"future":{"ignored":[1,2]},
             "categories":[{"id":"work","name":"Friends, \"old\" & new","future":true}]}
        """.trimIndent()

        assertEquals("Friends, \"old\" & new", LibraryOrganizationCodec.decode(encoded).categories.single().name)
    }

    @Test
    fun `oversized corrupt preferences are rejected before parsing`() {
        val oversized = " ".repeat(LibraryOrganizationCodec.MAX_ENCODED_LENGTH + 1)

        assertEquals(LibraryOrganization(), LibraryOrganizationCodec.decode(oversized))
    }

    @Test
    fun `deeply nested corruption is rejected without recursive parsing`() {
        val encoded = "{\"version\":1,\"unknown\":" + "[".repeat(1000) + "0" + "]".repeat(1000) + "}"

        assertEquals(LibraryOrganization(), LibraryOrganizationCodec.decode(encoded))
    }

    @Test
    fun `brackets and escaped quotes in category names do not affect nesting validation`() {
        val category = LibraryCategory(
            "work",
            "[Friends] \"(old)\" & {new}",
            LibraryCategoryIcon.FRIENDS,
            CardPalette.Default.accent,
        )
        val organization = LibraryOrganization(categories = listOf(category))

        assertEquals(organization, LibraryOrganizationCodec.decode(LibraryOrganizationCodec.encode(organization)))
    }

    @Test
    fun `UTF-8 byte bound applies even when the stored character count is smaller`() {
        val encoded = "{\"version\":1,\"favorites\":[1],\"unknown\":\"" +
            "é".repeat(LibraryOrganizationCodec.MAX_ENCODED_LENGTH / 2) + "\"}"

        assertTrue(encoded.length < LibraryOrganizationCodec.MAX_ENCODED_LENGTH)
        assertEquals(LibraryOrganization(), LibraryOrganizationCodec.decode(encoded))
    }

    @Test
    fun `writes refuse an oversized encoded document before it can replace the stored state`() {
        val category = LibraryCategory(
            "é".repeat(64),
            "Work",
            LibraryCategoryIcon.WORK,
            CardPalette.Default.accent,
        )
        val organization = LibraryOrganization(
            categories = listOf(category),
            assignments = (1L..5000L).associateWith { category.id },
        )

        assertThrows(IllegalArgumentException::class.java) { LibraryOrganizationCodec.encode(organization) }
    }

    @Test
    fun `large decoded collections stay bounded`() {
        val favorites = (1..LibraryOrganizationCodec.MAX_CONTACTS + 1).joinToString(",")
        val categories = (1..LibraryOrganizationCodec.MAX_CATEGORIES + 1).joinToString(",") {
            """{"id":"category-$it","name":"Category $it"}"""
        }
        val encoded = """{"version":1,"favorites":[$favorites],"categories":[$categories]}"""

        val decoded = LibraryOrganizationCodec.decode(encoded)

        assertEquals(LibraryOrganizationCodec.MAX_CONTACTS, decoded.favorites.size)
        assertEquals(LibraryOrganizationCodec.MAX_CATEGORIES, decoded.categories.size)
    }

    @Test
    fun `writes refuse to silently drop categories or contact metadata beyond the bounds`() {
        val categories = (1..LibraryOrganizationCodec.MAX_CATEGORIES + 1).map {
            LibraryCategory("category-$it", "Category $it", LibraryCategoryIcon.OTHER, CardPalette.Default.accent)
        }
        val contacts = (1L..LibraryOrganizationCodec.MAX_CONTACTS + 1L).toList()
        assertThrows(IllegalArgumentException::class.java) {
            LibraryOrganizationCodec.encode(LibraryOrganization(categories = categories))
        }
        assertThrows(IllegalArgumentException::class.java) {
            LibraryOrganizationCodec.encode(LibraryOrganization(favorites = contacts.toSet()))
        }
        assertThrows(IllegalArgumentException::class.java) {
            LibraryOrganizationCodec.encode(LibraryOrganization(assignments = contacts.associateWith { "work" }))
        }
        assertThrows(IllegalArgumentException::class.java) {
            LibraryOrganizationCodec.encode(LibraryOrganization(cardOrder = mapOf("" to contacts)))
        }
    }
}
