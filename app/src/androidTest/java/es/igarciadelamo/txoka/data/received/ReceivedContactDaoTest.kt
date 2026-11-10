package es.igarciadelamo.txoka.data.received

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReceivedContactDaoTest {
    private lateinit var database: TxokaDatabase
    private lateinit var dao: ReceivedContactDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TxokaDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.receivedContactDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertsAndObservesOrderedByNewestFirst() = runTest {
        dao.insert(entity(name = "Older", receivedAt = 1L))
        dao.insert(entity(name = "Newer", receivedAt = 2L))

        val contacts = dao.observeAll().first()

        assertEquals(listOf("Newer", "Older"), contacts.map { it.name })
    }

    @Test
    fun findsMatchesByPhoneEmailOrHandle() = runTest {
        val id = dao.insert(
            entity(
                name = "Marta",
                phoneKey = "34600111222",
                emailKey = "marta@example.com",
                linkedin = "marta-ruiz",
                instagram = "marta.ruiz",
            ),
        )

        assertEquals(id, dao.findMatch("34600111222", "", "", "")?.id)
        assertEquals(id, dao.findMatch("", "marta@example.com", "", "")?.id)
        assertEquals(id, dao.findMatch("", "", "marta-ruiz", "")?.id)
        assertEquals(id, dao.findMatch("", "", "", "marta.ruiz")?.id)
        assertNull(dao.findMatch("", "", "", ""))
        assertNull(dao.findMatch("1", "other@example.com", "", ""))
    }

    @Test
    fun updatesTheCardButKeepsTheNote() = runTest {
        val id = dao.insert(entity(name = "Marta", phoneKey = "34600111222", receivedAt = 1L))
        dao.updateNote(id, "Meetup")

        dao.updateCard(
            entity(name = "Marta Ruiz", phoneKey = "34600999888", receivedAt = 2L).copy(id = id),
        )

        val stored = dao.observe(id).first()
        assertEquals("Marta Ruiz", stored?.name)
        assertEquals("34600999888", stored?.phoneKey)
        assertEquals(2L, stored?.receivedAt)
        assertEquals("Meetup", stored?.note)
    }

    @Test
    fun updatesNotesAndDeletes() = runTest {
        val id = dao.insert(entity(name = "Marta"))

        dao.updateNote(id, "Meetup")
        assertEquals("Meetup", dao.observe(id).first()?.note)

        dao.delete(id)
        assertNull(dao.observe(id).first())
    }

    private fun entity(
        name: String,
        phoneKey: String = "",
        emailKey: String = "",
        linkedin: String? = null,
        instagram: String? = null,
        receivedAt: Long = 0L,
    ) = ReceivedContactEntity(
        name = name,
        phone = null,
        email = null,
        linkedin = linkedin,
        instagram = instagram,
        phoneKey = phoneKey,
        emailKey = emailKey,
        note = "",
        receivedAt = receivedAt,
        accentHex = "#4F46E5",
        accent2Hex = "#06B6D4",
    )
}
