@file:Suppress("MagicNumber")

package es.igarciadelamo.txoka.ui.components.artwork

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import es.igarciadelamo.txoka.domain.profile.CardMosaic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

private val MosaicDispatcher = Dispatchers.Default.limitedParallelism(1)

/** White alpha is the first ink; transparent pixels reveal the second ink or the underlying material. */
object CardMosaicMasks {
    private data class Key(val mosaic: CardMosaic, val width: Int, val height: Int)

    private val cache = object : LruCache<Key, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: Key, value: Bitmap): Int = value.byteCount
    }

    fun mask(context: Context, mosaic: CardMosaic, width: Int, height: Int): Bitmap? {
        if (mosaic == CardMosaic.NONE || !validBounds(width, height)) return null
        val scale = (MAX_SIDE.toDouble() / max(width, height)).coerceAtMost(1.0)
        val key =
            Key(mosaic, (width * scale).roundToInt().coerceAtLeast(1), (height * scale).roundToInt().coerceAtLeast(1))
        cache.get(key)?.let { return it }
        val result = runCatching {
            val artwork = CardArtworkCatalog.find(mosaic.wireId)
            if (artwork == null) {
                procedural(key)
            } else {
                val source = context.assets.open("card-artwork/${artwork.file}").bufferedReader().use { it.readText() }
                BundledSvgMask.render(source, key.width, key.height)
            }
        }.getOrNull() ?: return null
        return synchronized(cache) {
            cache.get(key) ?: result.also { cache.put(key, it) }
        }
    }

    private fun validBounds(width: Int, height: Int): Boolean =
        width > 0 && height > 0 && width.toDouble() / height in MIN_ASPECT..MAX_ASPECT

    private fun procedural(key: Key): Bitmap {
        val bitmap = Bitmap.createBitmap(key.width, key.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val scale = key.width / CardMosaicGeometry.WIDTH
        canvas.scale(scale, scale)
        if (key.mosaic == CardMosaic.DOTS) canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val clear = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        CardMosaicGeometry.polygons(key.mosaic, key.height / scale).forEach { shape ->
            val path = Path().apply {
                moveTo(shape.points[0].x, shape.points[0].y)
                shape.points.drop(1).forEach { point -> lineTo(point.x, point.y) }
                close()
            }
            paint.xfermode = if (shape.firstInk) null else clear
            canvas.drawPath(path, paint)
        }
        return bitmap.extractAlpha().also { bitmap.recycle() }
    }

    private const val MAX_SIDE = 1024
    private const val MIN_ASPECT = 1.0 / 32
    private const val MAX_ASPECT = 32.0
}

/** Rasterises local vectors off the UI thread and retains their antialiased ink coverage. */
@Composable
fun rememberCardMosaicMask(mosaic: CardMosaic, width: Int, height: Int): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    val bitmap by produceState<Bitmap?>(null, mosaic, width, height) {
        value = withContext(MosaicDispatcher) { CardMosaicMasks.mask(context, mosaic, width, height) }
    }
    return remember(bitmap) { bitmap?.asImageBitmap() }
}
