package es.igarciadelamo.txoka.preview

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.nio.file.FileAlreadyExistsException

class PreviewSampleInstallerTest {
    @get:Rule
    val temporary = TemporaryFolder()

    private val fixture = "SQLite format 3\u0000complete fixture".toByteArray()
    private val database get() = File(temporary.root, "databases/txoka.db")
    private val marker get() = File(temporary.root, "no_backup/samples.attempted")

    @Test
    fun `fresh install publishes a complete fixture and durable marker`() {
        assertEquals(PreviewSampleInstaller.Outcome.INSTALLED, install())
        assertArrayEquals(fixture, database.readBytes())
        assertTrue(marker.isFile)
        assertEquals(listOf("txoka.db"), database.parentFile?.list()?.toList())
    }

    @Test
    fun `repeat preserves edits without opening the fixture`() {
        install()
        database.writeText("user edits")

        assertEquals(
            PreviewSampleInstaller.Outcome.ALREADY_ATTEMPTED,
            PreviewSampleInstaller.install(database, marker) { error("Must not read fixture twice") },
        )
        assertEquals("user edits", database.readText())
    }

    @Test
    fun `deleting the database never repopulates samples`() {
        install()
        assertTrue(database.delete())

        assertEquals(PreviewSampleInstaller.Outcome.ALREADY_ATTEMPTED, install())
        assertFalse(database.exists())
    }

    @Test
    fun `existing database is preserved and permanently opts out`() {
        database.parentFile?.mkdirs()
        database.writeText("existing contacts and notes")

        assertEquals(PreviewSampleInstaller.Outcome.EXISTING_DATABASE, install())
        assertEquals("existing contacts and notes", database.readText())
        assertTrue(database.delete())
        assertEquals(PreviewSampleInstaller.Outcome.ALREADY_ATTEMPTED, install())
        assertFalse(database.exists())
    }

    @Test
    fun `orphan SQLite sidecars prevent seeding`() {
        listOf("-wal", "-shm", "-journal").forEach { suffix ->
            val isolated = temporary.newFolder()
            val target = File(isolated, "txoka.db")
            val guard = File(isolated, "attempted")
            File(target.path + suffix).writeText("existing SQLite state")

            assertEquals(
                PreviewSampleInstaller.Outcome.EXISTING_DATABASE,
                PreviewSampleInstaller.install(target, guard) { error("Must preserve existing SQLite state") },
            )
            assertFalse(target.exists())
            assertTrue(guard.exists())
        }
    }

    @Test
    fun `failed copy keeps its marker and removes the unpublished temporary file`() {
        assertThrows(IOException::class.java) {
            PreviewSampleInstaller.install(database, marker) { throw IOException("Interrupted asset read") }
        }

        assertFalse(database.exists())
        assertTrue(marker.exists())
        assertTrue(database.parentFile?.list().isNullOrEmpty())
        assertEquals(PreviewSampleInstaller.Outcome.ALREADY_ATTEMPTED, install())
    }

    @Test
    fun `invalid fixture never publishes an incomplete database`() {
        assertThrows(IOException::class.java) {
            PreviewSampleInstaller.install(database, marker) { "broken fixture".byteInputStream() }
        }
        assertFalse(database.exists())
        assertTrue(marker.exists())
    }

    @Test
    fun `database appearing during copy cannot be replaced by publication`() {
        assertThrows(FileAlreadyExistsException::class.java) {
            PreviewSampleInstaller.install(database, marker) {
                database.writeText("concurrent user database")
                fixture.inputStream()
            }
        }
        assertEquals("concurrent user database", database.readText())
        assertTrue(marker.exists())
    }

    @Test
    fun `SQLite sidecar appearing during copy prevents publication`() {
        val sidecar = File(database.path + "-wal")
        assertThrows(FileAlreadyExistsException::class.java) {
            PreviewSampleInstaller.install(database, marker) {
                sidecar.writeText("concurrent SQLite state")
                fixture.inputStream()
            }
        }
        assertFalse(database.exists())
        assertEquals("concurrent SQLite state", sidecar.readText())
        assertTrue(marker.exists())
    }

    private fun install(): PreviewSampleInstaller.Outcome =
        PreviewSampleInstaller.install(database, marker) { fixture.inputStream() }
}
