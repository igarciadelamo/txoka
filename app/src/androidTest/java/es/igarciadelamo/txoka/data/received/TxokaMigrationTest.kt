package es.igarciadelamo.txoka.data.received

import android.content.Context
import android.database.sqlite.SQLiteDatabase
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

/** Guards the upgrade path: a database written by version 1 must open with version 2. */
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
            .addMigrations(MIGRATION_1_2)
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
}
