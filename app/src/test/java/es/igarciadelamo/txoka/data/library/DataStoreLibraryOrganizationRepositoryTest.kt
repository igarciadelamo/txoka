package es.igarciadelamo.txoka.data.library

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import es.igarciadelamo.txoka.data.profile.DataStoreProfileRepository
import es.igarciadelamo.txoka.data.received.RoomReceivedContactRepository
import es.igarciadelamo.txoka.domain.library.LibraryCategory
import es.igarciadelamo.txoka.domain.library.LibraryCategoryIcon
import es.igarciadelamo.txoka.domain.library.LibraryOrganization
import es.igarciadelamo.txoka.domain.library.LibraryOrganizationSnapshot
import es.igarciadelamo.txoka.domain.library.LibraryViewMode
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import es.igarciadelamo.txoka.domain.protocol.VCardSerializer
import es.igarciadelamo.txoka.domain.protocol.toVCardPayload
import es.igarciadelamo.txoka.domain.received.SaveResult
import es.igarciadelamo.txoka.testing.FakeReceivedContactDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class DataStoreLibraryOrganizationRepositoryTest {
    private val store = OrganizationTestDataStore()
    private val repository = DataStoreLibraryOrganizationRepository(store)
    private val work = category("work")
    private val friends = category("friends")

    @Test
    fun `existing cards start uncategorized and edits survive reopening the repository`() = runTest {
        assertEquals(LibraryOrganization(), repository.organization.first())
        repository.saveCategory(work)
        repository.assign(1, work.id)
        repository.setFavorite(1, true)
        repository.setViewMode(LibraryViewMode.SHELVES)

        val reopened = DataStoreLibraryOrganizationRepository(store)
        assertEquals(repository.organization.first(), reopened.organization.first())
    }

    @Test
    fun `editing a category preserves its shelf position and unrelated preferences`() = runTest {
        val unrelated = stringPreferencesKey("unrelated")
        store.updateData { mutablePreferencesOf(unrelated to "keep") }
        repository.saveCategory(work)
        repository.saveCategory(friends)
        val edited = work.copy(name = "New work", accent2 = CardPalette.Default.accent2)

        repository.saveCategory(edited)

        assertEquals(listOf(edited, friends), repository.organization.first().categories)
        assertEquals("keep", store.data.first()[unrelated])
    }

    @Test
    fun `category reorder ignores unknown ids and retains omitted categories`() = runTest {
        repository.saveCategory(work)
        repository.saveCategory(friends)

        repository.reorderCategories(listOf("unknown", friends.id, friends.id))

        assertEquals(listOf(friends, work), repository.organization.first().categories)
    }

    @Test
    fun `moving a card removes its old shelf position and appends it to its new shelf`() = runTest {
        repository.saveCategory(work)
        repository.saveCategory(friends)
        repository.reorderCards(null, listOf(1, 2))
        repository.assign(3, friends.id)
        repository.assign(1, work.id)
        repository.assign(1, friends.id)
        repository.assign(1, friends.id)

        val moved = repository.organization.first()
        assertEquals(listOf(2L), moved.cardOrder[""])
        assertEquals(listOf(3L, 1L), moved.cardOrder[friends.id])
        assertEquals(friends.id, moved.assignments[1])
        assertNull(moved.cardOrder[work.id])

        repository.assign(1, null)
        assertEquals(listOf(2L, 1L), repository.organization.first().cardOrder[""])
        assertFalse(1L in repository.organization.first().assignments)
    }

    @Test
    fun `card ordering keeps omitted cards and excludes cards assigned to other shelves`() = runTest {
        repository.saveCategory(work)
        repository.assign(4, work.id)
        repository.reorderCards(null, listOf(1, 2, 3))

        repository.reorderCards(null, listOf(3, 3, 4, 0, -1))

        assertEquals(listOf(3L, 1L, 2L), repository.organization.first().cardOrder[""])
        assertEquals(listOf(4L), repository.organization.first().cardOrder[work.id])
    }

    @Test
    fun `deleting a category unassigns its cards preserves favorites and leaves other shelves intact`() = runTest {
        repository.saveCategory(work)
        repository.saveCategory(friends)
        repository.assign(1, work.id)
        repository.assign(2, work.id)
        repository.assign(3, friends.id)
        repository.reorderCards(null, listOf(4))
        repository.reorderCards(work.id, listOf(2, 1))
        repository.setFavorite(2, true)

        repository.deleteCategory(work.id)
        repository.deleteCategory("")
        repository.deleteCategory("absent")

        val remaining = repository.organization.first()
        assertEquals(listOf(friends), remaining.categories)
        assertEquals(mapOf(3L to friends.id), remaining.assignments)
        assertEquals(listOf(4L, 2L, 1L), remaining.cardOrder[""])
        assertEquals(listOf(3L), remaining.cardOrder[friends.id])
        assertEquals(setOf(2L), remaining.favorites)
    }

    @Test
    fun `deletion snapshot restores assignments favorites and the original positions among survivors`() = runTest {
        repository.saveCategory(work)
        (1L..4L).forEach { repository.assign(it, work.id) }
        repository.setFavorite(2, true)
        repository.setFavorite(4, true)
        repository.reorderCards(work.id, listOf(4, 2, 3, 1))
        val before = repository.organization.first()

        val snapshot = repository.removeContacts(setOf(2, 3))

        assertEquals(listOf(4L, 1L), repository.organization.first().cardOrder[work.id])
        assertEquals(setOf(4L), repository.organization.first().favorites)
        assertFalse(2L in repository.organization.first().assignments)

        repository.restoreContacts(snapshot)
        assertEquals(before, repository.organization.first())
    }

    @Test
    fun `undo empty library retains uncategorized ordering and can be safely repeated`() = runTest {
        repository.reorderCards(null, listOf(3, 1, 2))
        repository.setFavorite(1, true)
        val before = repository.organization.first()

        val snapshot = repository.removeContacts(setOf(1, 2, 3))
        assertEquals(LibraryOrganization(), repository.organization.first())
        repository.restoreContacts(snapshot)
        repository.restoreContacts(snapshot)

        assertEquals(before, repository.organization.first())
    }

    @Test
    fun `undo after a category was removed returns its deleted cards to uncategorized`() = runTest {
        repository.saveCategory(work)
        repository.assign(1, work.id)
        repository.setFavorite(1, true)
        val snapshot = repository.removeContacts(setOf(1))
        repository.deleteCategory(work.id)

        repository.restoreContacts(snapshot)

        assertTrue(repository.organization.first().assignments.isEmpty())
        assertEquals(listOf(1L), repository.organization.first().cardOrder[""])
        assertEquals(setOf(1L), repository.organization.first().favorites)
    }

    @Test
    fun `undo respects new cards and current ordering of surviving cards`() = runTest {
        repository.reorderCards(null, listOf(1, 2, 3, 4))
        val snapshot = repository.removeContacts(setOf(2, 3))
        repository.reorderCards(null, listOf(5, 4, 1))

        repository.restoreContacts(snapshot)

        val restored = repository.organization.first().cardOrder[""].orEmpty()
        assertEquals(listOf(5L, 4L, 1L), restored.filterNot { it == 2L || it == 3L })
        assertEquals(setOf(1L, 2L, 3L, 4L, 5L), restored.toSet())
    }

    @Test
    fun `pruning orphaned ids preserves categories favorites order and the selected view`() = runTest {
        repository.saveCategory(work)
        repository.assign(1, work.id)
        repository.assign(2, work.id)
        repository.reorderCards(null, listOf(3, 4))
        repository.setFavorite(2, true)
        repository.setFavorite(3, true)
        repository.setViewMode(LibraryViewMode.LIST)

        repository.prune(setOf(1, 3))
        repository.setFavorite(3, false)

        assertEquals(
            LibraryOrganization(
                categories = listOf(work),
                assignments = mapOf(1L to work.id),
                cardOrder = mapOf(work.id to listOf(1L), "" to listOf(3L)),
                viewMode = LibraryViewMode.LIST,
            ),
            repository.organization.first(),
        )
    }

    @Test
    fun `concurrent edits merge without losing assignments or favorites`() = runTest {
        repository.saveCategory(work)
        (1L..20L).map { id ->
            launch {
                repository.assign(id, work.id)
                repository.setFavorite(id, true)
            }
        }.joinAll()

        val result = repository.organization.first()
        assertEquals((1L..20L).toSet(), result.favorites)
        assertEquals((1L..20L).associateWith { work.id }, result.assignments)
    }

    @Test
    fun `organization remains private while received notes profiles and NFC or QR payloads stay intact`() = runTest {
        val profileStore = OrganizationTestDataStore()
        val profiles = DataStoreProfileRepository(profileStore)
        val profile = Profile(name = "Marta", email = "marta@example.com")
        profiles.save(profile)
        val sharedBefore = VCardSerializer.encode(profile.toVCardPayload())
        val received = RoomReceivedContactRepository(FakeReceivedContactDao())
        val saved = received.save(VCardPayload(name = "Ana", email = "ana@example.com")) as SaveResult.Saved
        received.updateNote(saved.id, "Private reunion")
        val contactBefore = received.contact(saved.id).first()

        repository.saveCategory(work.copy(name = "Secret category"))
        repository.assign(saved.id, work.id)
        repository.setFavorite(saved.id, true)
        repository.deleteCategory(work.id)
        profiles.delete()

        assertEquals(contactBefore, received.contact(saved.id).first())
        assertEquals(sharedBefore, VCardSerializer.encode(profile.toVCardPayload()))
        assertFalse(sharedBefore.contains("Secret category"))
        assertFalse(sharedBefore.contains("Private reunion"))
        assertEquals(setOf(saved.id), repository.organization.first().favorites)
        assertFalse(profileStore.data.first().asMap().keys.any { it.name.startsWith("library.") })
        assertFalse(store.data.first().asMap().keys.any { it.name.startsWith("profile.") })
    }

    @Test
    fun `clear library undo keeps category mappings attached to the restored contact ids`() = runTest {
        val received = RoomReceivedContactRepository(FakeReceivedContactDao())
        val saved = received.save(VCardPayload(name = "Ana", email = "ana@example.com")) as SaveResult.Saved
        received.updateNote(saved.id, "Private reunion")
        repository.saveCategory(work)
        repository.assign(saved.id, work.id)
        repository.setFavorite(saved.id, true)
        val organizationBefore = repository.organization.first()
        val contacts = received.deleteAll()
        val snapshot = repository.removeContacts(contacts.map { it.id }.toSet())

        received.restore(contacts)
        repository.restoreContacts(snapshot)

        assertEquals(contacts.single(), received.contact(saved.id).first())
        assertEquals(organizationBefore, repository.organization.first())
    }

    @Test
    fun `corrupt metadata recovers on the next write without touching unrelated preferences`() = runTest {
        val unrelated = stringPreferencesKey("other")
        store.updateData {
            mutablePreferencesOf(DataStoreLibraryOrganizationRepository.organizationKey to "bad", unrelated to "keep")
        }

        repository.saveCategory(work)

        assertEquals(listOf(work), repository.organization.first().categories)
        assertEquals("keep", store.data.first()[unrelated])
    }

    @Test
    fun `IO failures read as empty while unexpected failures propagate`() = runTest {
        val unreadable = DataStoreLibraryOrganizationRepository(FailingOrganizationDataStore(IOException("unreadable")))
        assertEquals(LibraryOrganization(), unreadable.organization.first())
        val failure = IllegalStateException("unexpected")
        val unexpected = DataStoreLibraryOrganizationRepository(FailingOrganizationDataStore(failure))
        try {
            unexpected.organization.first()
            throw AssertionError("Expected the original failure")
        } catch (actual: IllegalStateException) {
            assertEquals(failure, actual)
        }
    }

    @Test
    fun `invalid moves and failed edits preserve the stored organization`() = runTest {
        repository.saveCategory(work)
        val before = repository.organization.first()
        listOf<suspend () -> Unit>(
            { repository.assign(1, "missing") },
            { repository.assign(0, work.id) },
            { repository.reorderCards("missing", listOf(1)) },
            { repository.setFavorite(0, true) },
        ).forEach { operation ->
            try {
                operation()
                throw AssertionError("Expected validation failure")
            } catch (_: IllegalArgumentException) {
                assertEquals(before, repository.organization.first())
            }
        }
        repository.restoreContacts(LibraryOrganizationSnapshot())
        assertEquals(before, repository.organization.first())
    }

    private fun category(id: String): LibraryCategory = LibraryCategory(
        id = id,
        name = id,
        icon = LibraryCategoryIcon.WORK,
        accent = CardPalette.Default.accent,
    )
}

private class OrganizationTestDataStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
    private val mutex = Mutex()
    private val state = MutableStateFlow(initial)
    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = mutex.withLock {
        transform(state.value).also { state.value = it }
    }
}

private class FailingOrganizationDataStore(private val failure: Throwable) : DataStore<Preferences> {
    override val data: Flow<Preferences> = flow { throw failure }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = throw failure
}
