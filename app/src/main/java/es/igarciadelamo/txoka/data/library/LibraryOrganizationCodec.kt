package es.igarciadelamo.txoka.data.library

import es.igarciadelamo.txoka.domain.library.LibraryCategory
import es.igarciadelamo.txoka.domain.library.LibraryCategoryIcon
import es.igarciadelamo.txoka.domain.library.LibraryOrganization
import es.igarciadelamo.txoka.domain.library.LibraryViewMode
import es.igarciadelamo.txoka.domain.library.UNCATEGORIZED_CATEGORY_ID
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.RgbColor
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object LibraryOrganizationCodec {
    const val MAX_CATEGORIES = 128
    const val MAX_CONTACTS = 10_000
    const val MAX_ENCODED_LENGTH = 524_288
    private const val VERSION = 1
    private const val MAX_JSON_DEPTH = 8
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(organization: LibraryOrganization): String {
        require(organization.categories.size <= MAX_CATEGORIES)
        require(organization.assignments.size <= MAX_CONTACTS && organization.favorites.size <= MAX_CONTACTS)
        require(organization.cardOrder.values.sumOf { it.size } <= MAX_CONTACTS)
        val wire = StoredOrganization(
            version = VERSION,
            categories = organization.categories.map { category ->
                StoredCategory(
                    category.id,
                    category.name,
                    category.icon.name,
                    category.accent.hex,
                    category.accent2?.hex,
                )
            },
            assignments = organization.assignments.mapKeys { (id, _) -> id.toString() },
            cardOrder = organization.cardOrder,
            favorites = organization.favorites.toList(),
            viewMode = organization.viewMode.name,
        )
        return json.encodeToString(wire).also {
            require(it.length <= MAX_ENCODED_LENGTH && it.toByteArray(Charsets.UTF_8).size <= MAX_ENCODED_LENGTH)
        }
    }

    fun decode(value: String?): LibraryOrganization {
        val raw = value?.takeIf { it.isNotEmpty() && it.length <= MAX_ENCODED_LENGTH }
            ?: return LibraryOrganization()
        if (!isSafeDocument(raw)) return LibraryOrganization()
        val wire = parse(raw)?.takeIf { it.version == VERSION } ?: return LibraryOrganization()
        val categories = wire.categories.take(MAX_CATEGORIES).mapNotNull { it.toCategory() }.distinctBy { it.id }
        val categoryIds = categories.map { it.id }.toSet()
        val assignments = wire.assignments.entries.take(MAX_CONTACTS).mapNotNull { (id, categoryId) ->
            id.toLongOrNull()?.takeIf { it > 0 && categoryId in categoryIds }?.let { it to categoryId }
        }.toMap()
        return LibraryOrganization(
            categories = categories,
            assignments = assignments,
            cardOrder = cleanOrder(wire.cardOrder, categoryIds, assignments),
            favorites = wire.favorites.asSequence().filter { it > 0 }.take(MAX_CONTACTS).toSet(),
            viewMode = LibraryViewMode.entries.firstOrNull { it.name == wire.viewMode } ?: LibraryViewMode.COVER_FLOW,
        )
    }

    private fun parse(value: String): StoredOrganization? = try {
        json.decodeFromString<StoredOrganization>(value)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun isSafeDocument(value: String): Boolean =
        value.toByteArray(Charsets.UTF_8).size <= MAX_ENCODED_LENGTH && hasSafeNesting(value)

    private fun hasSafeNesting(value: String): Boolean {
        var depth = 0
        var quoted = false
        var escaped = false
        for (character in value) {
            when {
                escaped -> escaped = false
                quoted -> when (character) {
                    '\\' -> escaped = true
                    '"' -> quoted = false
                }
                character == '"' -> quoted = true
                character == '{' || character == '[' -> depth++
                character == '}' || character == ']' -> depth--
            }
            if (depth !in 0..MAX_JSON_DEPTH) return false
        }
        return depth == 0 && !quoted
    }

    private fun cleanOrder(
        order: Map<String, List<Long>>,
        categoryIds: Set<String>,
        assignments: Map<Long, String>,
    ): Map<String, List<Long>> {
        var remaining = MAX_CONTACTS
        return order.entries.asSequence().filter { (categoryId, _) ->
            categoryId == UNCATEGORIZED_CATEGORY_ID || categoryId in categoryIds
        }.associate { (categoryId, ids) ->
            val cleaned = ids.asSequence().filter { id ->
                id > 0 && assignments[id].orEmpty() == categoryId
            }.distinct().take(remaining).toList()
            remaining -= cleaned.size
            categoryId to cleaned
        }.filterValues { it.isNotEmpty() }
    }

    private fun StoredCategory.toCategory(): LibraryCategory? = try {
        LibraryCategory(
            id = id,
            name = name.trim(),
            icon = LibraryCategoryIcon.entries.firstOrNull { it.name == icon } ?: LibraryCategoryIcon.OTHER,
            accent = RgbColor.parse(accent) ?: CardPalette.Default.accent,
            accent2 = accent2?.let(RgbColor::parse),
        )
    } catch (_: IllegalArgumentException) {
        null
    }

    @Serializable
    private data class StoredOrganization(
        val version: Int = 0,
        val categories: List<StoredCategory> = emptyList(),
        val assignments: Map<String, String> = emptyMap(),
        val cardOrder: Map<String, List<Long>> = emptyMap(),
        val favorites: List<Long> = emptyList(),
        val viewMode: String = LibraryViewMode.COVER_FLOW.name,
    )

    @Serializable
    private data class StoredCategory(
        val id: String = "",
        val name: String = "",
        val icon: String = LibraryCategoryIcon.OTHER.name,
        val accent: String = CardPalette.Default.accent.hex,
        val accent2: String? = null,
    )
}
