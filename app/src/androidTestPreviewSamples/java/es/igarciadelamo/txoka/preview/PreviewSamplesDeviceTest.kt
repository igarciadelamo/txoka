package es.igarciadelamo.txoka.preview

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ProviderInfo
import android.content.res.AssetManager
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.igarciadelamo.txoka.data.received.TxokaDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PreviewSamplesDeviceTest {
    private lateinit var directory: File
    private lateinit var isolatedContext: Context

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        directory = File(instrumentation.targetContext.cacheDir, "preview-samples-${UUID.randomUUID()}")
        isolatedContext = object : ContextWrapper(instrumentation.targetContext) {
            override fun getApplicationContext(): Context = this

            override fun getDatabasePath(name: String): File {
                val path = File(name)
                return if (path.isAbsolute) path else File(directory, "databases/$name")
            }

            override fun getNoBackupFilesDir(): File = File(directory, "no_backup")

            override fun getAssets(): AssetManager = instrumentation.context.assets
        }
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun providerSeedsEightReceivedContactsBeforeRoomOpens() = runTest {
        startProvider()
        withDatabase { database ->
            val contacts = database.receivedContactDao().observeAll().first()
            assertEquals(8, contacts.size)
            assertEquals(8, contacts.map { it.email }.toSet().size)
            assertTrue(contacts.all { it.email?.endsWith("@example.com") == true && it.phone == null })
        }
        assertFalse(File(directory, "datastore").exists())
    }

    @Test
    fun repeatedStartupPreservesNotesAndDeletedLibrary() = runTest {
        startProvider()
        withDatabase { database ->
            database.receivedContactDao().updateNote(1, "Tester note")
        }
        startProvider()
        withDatabase { database ->
            assertEquals("Tester note", database.receivedContactDao().observe(1).first()?.note)
            database.receivedContactDao().deleteAll()
        }
        startProvider()
        withDatabase { database ->
            assertTrue(database.receivedContactDao().observeAll().first().isEmpty())
        }
    }

    @Test
    fun existingRoomDatabaseIsNeverReplacedOrRepopulatedAfterRemoval() = runTest {
        withDatabase { database ->
            assertTrue(database.receivedContactDao().observeAll().first().isEmpty())
        }
        startProvider()
        withDatabase { database ->
            assertTrue(database.receivedContactDao().observeAll().first().isEmpty())
        }
        listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
            File(isolatedContext.getDatabasePath("txoka.db").path + suffix).delete()
        }
        startProvider()
        assertFalse(isolatedContext.getDatabasePath("txoka.db").exists())
    }

    private fun startProvider() {
        PreviewSamplesProvider().attachInfo(
            isolatedContext,
            ProviderInfo().apply {
                authority = "es.igarciadelamo.txoka.preview.test.samples"
                exported = false
            },
        )
    }

    private fun openDatabase(): TxokaDatabase {
        val path = isolatedContext.getDatabasePath("txoka.db")
        path.parentFile?.mkdirs()
        return Room.databaseBuilder(isolatedContext, TxokaDatabase::class.java, path.absolutePath).build()
    }

    private suspend fun withDatabase(block: suspend (TxokaDatabase) -> Unit) {
        val database = openDatabase()
        try {
            block(database)
        } finally {
            database.close()
        }
    }
}
