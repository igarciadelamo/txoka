package es.igarciadelamo.txoka.ui.qr

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import es.igarciadelamo.txoka.domain.protocol.QrVCardCodec
import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

internal suspend fun readContactQrImage(resolver: ContentResolver, uri: Uri): VCardPayload? =
    withContext(Dispatchers.IO) {
        try {
            readSampledBitmap(resolver, uri)?.let { bitmap ->
                try {
                    val pixels = IntArray(bitmap.width * bitmap.height)
                    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                    decodeQrText(RGBLuminanceSource(bitmap.width, bitmap.height, pixels))?.let(QrVCardCodec::decode)
                } finally {
                    bitmap.recycle()
                }
            }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

private fun readSampledBitmap(resolver: ContentResolver, uri: Uri): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val boundsStream = resolver.openInputStream(uri) ?: return null
    boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (!bounds.hasSafeDimensions()) return null
    var sample = 1
    val largestDimension = maxOf(bounds.outWidth, bounds.outHeight).toLong()
    while ((largestDimension + sample - 1) / sample > MAX_IMAGE_DIMENSION) sample *= 2
    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
    return if (bitmap.width <= MAX_IMAGE_DIMENSION && bitmap.height <= MAX_IMAGE_DIMENSION) {
        bitmap
    } else {
        bitmap.recycle()
        null
    }
}

private fun BitmapFactory.Options.hasSafeDimensions(): Boolean {
    val safeBounds = outWidth in 1..MAX_SOURCE_DIMENSION && outHeight in 1..MAX_SOURCE_DIMENSION
    return safeBounds && outWidth.toLong() * outHeight <= MAX_SOURCE_PIXELS
}

internal fun decodeQrText(source: LuminanceSource): String? = try {
    QRCodeReader().decode(
        BinaryBitmap(HybridBinarizer(source)),
        mapOf(DecodeHintType.TRY_HARDER to true, DecodeHintType.CHARACTER_SET to "UTF-8"),
    ).text
} catch (_: ReaderException) {
    null
}

private const val MAX_IMAGE_DIMENSION = 2048
private const val MAX_SOURCE_DIMENSION = 65_536
private const val MAX_SOURCE_PIXELS = 100_000_000L
