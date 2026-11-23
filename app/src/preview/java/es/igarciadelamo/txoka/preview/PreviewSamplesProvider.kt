package es.igarciadelamo.txoka.preview

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log
import java.io.IOException

class PreviewSamplesProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        val applicationContext = context ?: return false
        return try {
            PreviewSampleInstaller.install(applicationContext)
            true
        } catch (exception: IOException) {
            Log.w("PreviewSamples", "Sample installation skipped", exception)
            false
        } catch (exception: SecurityException) {
            Log.w("PreviewSamples", "Sample installation skipped", exception)
            false
        } catch (exception: UnsupportedOperationException) {
            Log.w("PreviewSamples", "Sample installation skipped", exception)
            false
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        0
}
