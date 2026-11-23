package es.igarciadelamo.txoka.data.received

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.igarciadelamo.txoka.domain.profile.CardAppearance
import es.igarciadelamo.txoka.domain.profile.CardAppearanceCodec
import es.igarciadelamo.txoka.domain.profile.CardBackground
import es.igarciadelamo.txoka.domain.profile.CardEffect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Guards upgrades from both released schemas without rebuilding or losing received cards. */
@RunWith(AndroidJUnit4::class)
class TxokaMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "migration-test.db"

    @Before
    fun removeDatabaseBefore() {
        context.deleteDatabase(databaseName)
    }

    @After
    fun removeDatabaseAfter() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migratesVersion1AndKeepsStoredCards() = runTest {
        writeVersion1Database()

        val database = Room.databaseBuilder(context, TxokaDatabase::class.java, databaseName)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
        val contacts = database.receivedContactDao().observeAll().first()
        database.close()

        assertEquals(1, contacts.size)
        assertEquals("Marta Ruiz", contacts.single().name)
        assertEquals("+34600111222", contacts.single().phone)
        assertEquals("+34600111222", contacts.single().phoneKey)
        assertEquals("", contacts.single().emailKey)
        assertNull(contacts.single().linkedin)
        assertNull(contacts.single().instagram)
        assertNull(contacts.single().appearance)
    }

    @Test
    fun migratesVersion2AndKeepsAllCardData() = runTest {
        writeVersion2Database()

        val database = Room.databaseBuilder(context, TxokaDatabase::class.java, databaseName)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
        try {
            val contact = database.receivedContactDao().observeAll().first().single()

            assertEquals(1L, contact.id)
            assertEquals("Marta Ruiz", contact.name)
            assertEquals("+34600111222", contact.phone)
            assertEquals("marta@example.com", contact.email)
            assertEquals("marta-ruiz", contact.linkedin)
            assertEquals("marta.ruiz", contact.instagram)
            assertEquals("+34600111222", contact.phoneKey)
            assertEquals("marta@example.com", contact.emailKey)
            assertEquals("Meetup", contact.note)
            assertEquals(1700000000000L, contact.receivedAt)
            assertEquals("#0E7C7B", contact.accentHex)
            assertEquals("#7FD1AE", contact.accent2Hex)
            assertNull(contact.appearance)

            val appearance = CardAppearance(background = CardBackground.HOLOGRAM, effect = CardEffect.WATER)
            database.receivedContactDao().updateCard(contact.copy(appearance = CardAppearanceCodec.encode(appearance)))
            val restoredAppearance = database.receivedContactDao().observeAll().first().single().appearance
            assertEquals(appearance, restoredAppearance?.let(CardAppearanceCodec::decode))
        } finally {
            database.close()
        }
    }

    /** The version 1 table as Room created it, before LinkedIn and Instagram existed. */
    private fun writeVersion1Database() {
        val path = context.getDatabasePath(databaseName).apply { parentFile?.mkdirs() }
        val database = SQLiteDatabase.openOrCreateDatabase(path, null)
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `received_contacts` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `phone` TEXT,
                `email` TEXT,
                `phoneKey` TEXT NOT NULL,
                `emailKey` TEXT NOT NULL,
                `note` TEXT NOT NULL,
                `receivedAt` INTEGER NOT NULL,
                `accentHex` TEXT NOT NULL,
                `accent2Hex` TEXT NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            INSERT INTO `received_contacts`
                (`name`, `phone`, `phoneKey`, `emailKey`, `note`, `receivedAt`, `accentHex`, `accent2Hex`)
            VALUES ('Marta Ruiz', '+34600111222', '+34600111222', '', '', 1700000000000, '#0E7C7B', '#7FD1AE')
            """.trimIndent(),
        )
        database.version = 1
        database.close()
    }

    private fun writeVersion2Database() {
        writeVersion1Database()
        val database = SQLiteDatabase.openDatabase(
            context.getDatabasePath(databaseName).path,
            null,
            SQLiteDatabase.OPEN_READWRITE,
        )
        database.execSQL("ALTER TABLE received_contacts ADD COLUMN linkedin TEXT")
        database.execSQL("ALTER TABLE received_contacts ADD COLUMN instagram TEXT")
        database.execSQL(
            """
            UPDATE received_contacts SET
                email = 'marta@example.com', emailKey = 'marta@example.com',
                linkedin = 'marta-ruiz', instagram = 'marta.ruiz', note = 'Meetup'
            WHERE id = 1
            """.trimIndent(),
        )
        database.version = 2
        database.close()
    }
}
