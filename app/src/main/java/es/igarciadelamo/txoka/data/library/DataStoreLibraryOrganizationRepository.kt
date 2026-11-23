package es.igarciadelamo.txoka.data.library

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import es.igarciadelamo.txoka.domain.library.LibraryCategory
import es.igarciadelamo.txoka.domain.library.LibraryOrganization
import es.igarciadelamo.txoka.domain.library.LibraryOrganizationRepository
import es.igarciadelamo.txoka.domain.library.LibraryOrganizationSnapshot
import es.igarciadelamo.txoka.domain.library.LibraryViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

class DataStoreLibraryOrganizationRepository(private val dataStore: DataStore<Preferences>) :
    LibraryOrganizationRepository {
    override val organization: Flow<LibraryOrganization> = dataStore.data.catch { failure ->
        if (failure is IOException) emit(emptyPreferences()) else throw failure
    }.map { preferences -> LibraryOrganizationCodec.decode(preferences[organizationKey]) }.distinctUntilChanged()

    override suspend fun saveCategory(category: LibraryCategory) {
        update { LibraryOrganizationChanges.saveCategory(it, category) }
    }

    override suspend fun deleteCategory(id: String) {
        update { LibraryOrganizationChanges.deleteCategory(it, id) }
    }

    override suspend fun reorderCategories(ids: List<String>) {
        update { LibraryOrganizationChanges.reorderCategories(it, ids) }
    }

    override suspend fun assign(contactId: Long, categoryId: String?) {
        update { LibraryOrganizationChanges.assign(it, contactId, categoryId) }
    }

    override suspend fun reorderCards(categoryId: String?, ids: List<Long>) {
        update { LibraryOrganizationChanges.reorderCards(it, categoryId, ids) }
    }

    override suspend fun setFavorite(contactId: Long, favorite: Boolean) {
        require(contactId > 0)
        update { state ->
            state.copy(favorites = if (favorite) state.favorites + contactId else state.favorites - contactId)
        }
    }

    override suspend fun removeContacts(ids: Set<Long>): LibraryOrganizationSnapshot {
        var snapshot = LibraryOrganizationSnapshot()
        update { state ->
            snapshot = LibraryOrganizationChanges.snapshot(state, ids)
            LibraryOrganizationChanges.remove(state, ids)
        }
        return snapshot
    }

    override suspend fun restoreContacts(snapshot: LibraryOrganizationSnapshot) {
        update { LibraryOrganizationChanges.restore(it, snapshot) }
    }

    override suspend fun prune(knownIds: Set<Long>) {
        update { state ->
            val storedIds = state.assignments.keys + state.cardOrder.values.flatten() + state.favorites
            LibraryOrganizationChanges.remove(state, storedIds.toSet() - knownIds)
        }
    }

    override suspend fun setViewMode(mode: LibraryViewMode) {
        update { it.copy(viewMode = mode) }
    }

    private suspend fun update(transform: (LibraryOrganization) -> LibraryOrganization) {
        dataStore.edit { preferences ->
            val previous = LibraryOrganizationCodec.decode(preferences[organizationKey])
            preferences[organizationKey] = LibraryOrganizationCodec.encode(transform(previous))
        }
    }

    internal companion object {
        val organizationKey = stringPreferencesKey("library.organization")
    }
}
