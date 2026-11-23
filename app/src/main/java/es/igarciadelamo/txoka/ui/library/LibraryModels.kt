package es.igarciadelamo.txoka.ui.library

import es.igarciadelamo.txoka.domain.library.LibraryCategory
import es.igarciadelamo.txoka.domain.library.LibraryOrganization
import es.igarciadelamo.txoka.domain.library.UNCATEGORIZED_CATEGORY_ID
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.domain.received.ReceivedContact

data class LibrarySection(val category: LibraryCategory?, val contacts: List<ReceivedContact>)

enum class LibraryFilter { ALL, FAVORITES, RECENT }

internal fun librarySections(
    contacts: List<ReceivedContact>,
    organization: LibraryOrganization,
): List<LibrarySection> {
    val categoryIds = organization.categories.map { it.id }.toSet()
    val grouped = contacts.groupBy { organization.assignments[it.id]?.takeIf(categoryIds::contains) }
    return (organization.categories.map { it to grouped[it.id].orEmpty() } + (null to grouped[null].orEmpty()))
        .map { (category, cards) ->
            val order = organization.cardOrder[category?.id ?: UNCATEGORIZED_CATEGORY_ID].orEmpty()
                .withIndex().associate { it.value to it.index }
            LibrarySection(category, cards.sortedBy { order[it.id] ?: Int.MAX_VALUE })
        }
}

internal fun ReceivedContact.libraryProfile(): Profile = Profile(name, phone, email, linkedin, instagram, card)
