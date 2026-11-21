package es.igarciadelamo.txoka.domain.library

import kotlinx.coroutines.flow.Flow

interface LibraryOrganizationRepository {
    val organization: Flow<LibraryOrganization>

    suspend fun saveCategory(category: LibraryCategory)
    suspend fun deleteCategory(id: String)
    suspend fun reorderCategories(ids: List<String>)
    suspend fun assign(contactId: Long, categoryId: String?)
    suspend fun reorderCards(categoryId: String?, ids: List<Long>)
    suspend fun setFavorite(contactId: Long, favorite: Boolean)
    suspend fun removeContacts(ids: Set<Long>): LibraryOrganizationSnapshot
    suspend fun restoreContacts(snapshot: LibraryOrganizationSnapshot)
    suspend fun prune(knownIds: Set<Long>)
    suspend fun setViewMode(mode: LibraryViewMode)
}
