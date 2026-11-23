package es.igarciadelamo.txoka.preview

import android.content.Context
import java.io.DataInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files

internal object PreviewSampleInstaller {
    private const val DATABASE = "txoka.db"
    private const val MARKER = "preview-samples-v1.attempted"
    private const val ASSET = "preview-samples/txoka.db"
    private val SqliteHeader = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    enum class Outcome { INSTALLED, ALREADY_ATTEMPTED, EXISTING_DATABASE }

    fun install(context: Context): Outcome = install(
        context.getDatabasePath(DATABASE),
        File(context.noBackupFilesDir, MARKER),
    ) { context.assets.open(ASSET) }

    fun install(database: File, marker: File, fixture: () -> InputStream): Outcome {
        val markerDirectory = marker.parentFile ?: throw IOException("Missing marker directory")
        ensureDirectory(markerDirectory)
        return RandomAccessFile(File(markerDirectory, "$MARKER.lock"), "rw").use { lockFile ->
            lockFile.channel.lock().use {
                if (marker.exists()) return Outcome.ALREADY_ATTEMPTED
                val databaseExists = hasDatabase(database)
                if (!marker.createNewFile()) return Outcome.ALREADY_ATTEMPTED
                FileOutputStream(marker).use { stream ->
                    stream.write("1\n".toByteArray(Charsets.US_ASCII))
                    stream.fd.sync()
                }
                if (databaseExists) return Outcome.EXISTING_DATABASE
                publish(database, fixture)
                Outcome.INSTALLED
            }
        }
    }

    private fun publish(database: File, fixture: () -> InputStream) {
        val directory = database.parentFile ?: throw IOException("Missing database directory")
        ensureDirectory(directory)
        val temporary = File.createTempFile("preview-samples-", ".db", directory)
        try {
            FileOutputStream(temporary).use { output ->
                fixture().use { input -> input.copyTo(output) }
                output.fd.sync()
            }
            val header = ByteArray(SqliteHeader.size)
            DataInputStream(temporary.inputStream()).use { it.readFully(header) }
            if (!header.contentEquals(SqliteHeader)) throw IOException("Invalid sample database header")
            if (hasDatabase(database)) throw FileAlreadyExistsException(database.path)
            // Publish the complete file before Room opens, without replacing an existing destination.
            Files.move(temporary.toPath(), database.toPath())
        } finally {
            temporary.delete()
        }
    }

    private fun hasDatabase(database: File): Boolean = listOf("", "-wal", "-shm", "-journal").any { suffix ->
        File(database.path + suffix).exists()
    }

    private fun ensureDirectory(directory: File) {
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Cannot create sample directory")
    }
}
