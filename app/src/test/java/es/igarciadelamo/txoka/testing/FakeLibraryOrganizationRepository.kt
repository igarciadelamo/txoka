package es.igarciadelamo.txoka.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import es.igarciadelamo.txoka.data.library.DataStoreLibraryOrganizationRepository
import es.igarciadelamo.txoka.data.library.LibraryOrganizationCodec
import es.igarciadelamo.txoka.domain.library.LibraryOrganization
import es.igarciadelamo.txoka.domain.library.LibraryOrganizationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FakeLibraryOrganizationRepository(initial: LibraryOrganization = LibraryOrganization()) :
    LibraryOrganizationRepository by DataStoreLibraryOrganizationRepository(LibraryMemoryDataStore(initial))

private class LibraryMemoryDataStore(initial: LibraryOrganization) : DataStore<Preferences> {
    private val mutex = Mutex()
    private val state = MutableStateFlow<Preferences>(
        mutablePreferencesOf(
            DataStoreLibraryOrganizationRepository.organizationKey to LibraryOrganizationCodec.encode(initial),
        ),
    )
    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = mutex.withLock {
        transform(state.value).also { state.value = it }
    }
}
