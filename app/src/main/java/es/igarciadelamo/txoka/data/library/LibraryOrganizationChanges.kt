package es.igarciadelamo.txoka.data.library

import es.igarciadelamo.txoka.domain.library.LibraryCategory
import es.igarciadelamo.txoka.domain.library.LibraryOrganization
import es.igarciadelamo.txoka.domain.library.LibraryOrganizationSnapshot
import es.igarciadelamo.txoka.domain.library.UNCATEGORIZED_CATEGORY_ID

internal object LibraryOrganizationChanges {
    fun saveCategory(state: LibraryOrganization, category: LibraryCategory): LibraryOrganization {
        val exists = state.categories.any { it.id == category.id }
        require(exists || state.categories.size < LibraryOrganizationCodec.MAX_CATEGORIES)
        return state.copy(
            categories = if (exists) {
                state.categories.map { if (it.id == category.id) category else it }
            } else {
                state.categories + category
            },
        )
    }

    fun deleteCategory(state: LibraryOrganization, id: String): LibraryOrganization {
        if (state.categories.none { it.id == id }) return state
        val unassigned = state.cardOrder[id].orEmpty() + state.assignments.filterValues { it == id }.keys
        val order = state.cardOrder - id
        val uncategorized = (order[UNCATEGORIZED_CATEGORY_ID].orEmpty() + unassigned).distinct()
        return state.copy(
            categories = state.categories.filterNot { it.id == id },
            assignments = state.assignments.filterValues { it != id },
            cardOrder = order + (UNCATEGORIZED_CATEGORY_ID to uncategorized),
        )
    }

    fun reorderCategories(state: LibraryOrganization, ids: List<String>): LibraryOrganization {
        val categories = state.categories.associateBy { it.id }
        val orderedIds = (ids + state.categories.map { it.id }).distinct()
        return state.copy(categories = orderedIds.mapNotNull { categories[it] })
    }

    fun assign(state: LibraryOrganization, contactId: Long, categoryId: String?): LibraryOrganization {
        require(contactId > 0)
        val category = categoryId.orEmpty()
        require(category.isEmpty() || state.categories.any { it.id == category })
        if (state.assignments[contactId].orEmpty() == category) return state
        val order = state.cardOrder.mapValues { (_, ids) -> ids.filterNot { it == contactId } }
        return state.copy(
            assignments = if (category.isEmpty()) {
                state.assignments - contactId
            } else {
                state.assignments + (contactId to category)
            },
            cardOrder = order + (category to (order[category].orEmpty() + contactId)),
        )
    }

    fun reorderCards(state: LibraryOrganization, categoryId: String?, ids: List<Long>): LibraryOrganization {
        val category = categoryId.orEmpty()
        require(category.isEmpty() || state.categories.any { it.id == category })
        val requested = ids.filter { it > 0 && state.assignments[it].orEmpty() == category }
        val existing = state.cardOrder[category].orEmpty()
        return state.copy(cardOrder = state.cardOrder + (category to (requested + existing).distinct()))
    }

    fun snapshot(state: LibraryOrganization, ids: Set<Long>): LibraryOrganizationSnapshot = LibraryOrganizationSnapshot(
        contactIds = ids.filter { it > 0 }.toSet(),
        assignments = state.assignments.filterKeys { it in ids },
        // Retaining the complete order permits reinserting removed cards between surviving neighbours.
        cardOrder = state.cardOrder.filterValues { order -> order.any { it in ids } },
        favorites = state.favorites.intersect(ids),
    )

    fun remove(state: LibraryOrganization, ids: Set<Long>): LibraryOrganization = state.copy(
        assignments = state.assignments.filterKeys { it !in ids },
        cardOrder = state.cardOrder.mapValues { (_, order) ->
            order.filterNot { it in ids }
        }.filterValues { it.isNotEmpty() },
        favorites = state.favorites - ids,
    )

    fun restore(state: LibraryOrganization, snapshot: LibraryOrganizationSnapshot): LibraryOrganization {
        val categoryIds = state.categories.map { it.id }.toSet()
        val assignments = state.assignments + snapshot.assignments.filter { (id, categoryId) ->
            id in snapshot.contactIds && categoryId in categoryIds
        }
        val orders = state.cardOrder.toMutableMap()
        snapshot.cardOrder.forEach { (categoryId, previous) ->
            val category = categoryId.takeIf { it in categoryIds }.orEmpty()
            val restored = previous.filter { it in snapshot.contactIds && assignments[it].orEmpty() == category }
            val current = orders[category].orEmpty().filterNot { it in restored }.toMutableList()
            restored.forEach { id ->
                val prior = previous.takeWhile { it != id }.lastOrNull { it in current }
                val next = previous.dropWhile { it != id }.drop(1).firstOrNull { it in current }
                val index = when {
                    prior != null -> current.indexOf(prior) + 1
                    next != null -> current.indexOf(next)
                    else -> current.size
                }
                current.add(index, id)
            }
            orders[category] = current
        }
        return state.copy(
            assignments = assignments,
            cardOrder = orders,
            favorites = state.favorites + snapshot.favorites,
        )
    }
}
