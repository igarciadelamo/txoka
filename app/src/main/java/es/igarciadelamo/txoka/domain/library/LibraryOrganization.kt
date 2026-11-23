package es.igarciadelamo.txoka.domain.library

import es.igarciadelamo.txoka.domain.profile.RgbColor

const val UNCATEGORIZED_CATEGORY_ID = ""
const val MAX_CATEGORY_NAME_LENGTH = 40

/** Private device metadata. It is deliberately separate from profiles and shared vCards. */
data class LibraryCategory(
    val id: String,
    val name: String,
    val icon: LibraryCategoryIcon,
    val accent: RgbColor,
    val accent2: RgbColor? = null,
) {
    init {
        require(id.isNotBlank() && id.length <= MAX_CATEGORY_ID_LENGTH && id.all { it.isLetterOrDigit() || it == '-' })
        require(name.isNotBlank() && name == name.trim() && name.length <= MAX_CATEGORY_NAME_LENGTH)
        require(name.none { it.isISOControl() })
    }

    private companion object {
        const val MAX_CATEGORY_ID_LENGTH = 64
    }
}

enum class LibraryCategoryIcon { PEOPLE, WORK, FRIENDS, FAMILY, EVENT, TRAVEL, STUDY, OTHER }

enum class LibraryViewMode { SHELVES, COVER_FLOW, LIST }

data class LibraryOrganization(
    val categories: List<LibraryCategory> = emptyList(),
    val assignments: Map<Long, String> = emptyMap(),
    val cardOrder: Map<String, List<Long>> = emptyMap(),
    val favorites: Set<Long> = emptySet(),
    val viewMode: LibraryViewMode = LibraryViewMode.COVER_FLOW,
)

data class LibraryOrganizationSnapshot(
    val contactIds: Set<Long> = emptySet(),
    val assignments: Map<Long, String> = emptyMap(),
    val cardOrder: Map<String, List<Long>> = emptyMap(),
    val favorites: Set<Long> = emptySet(),
)
