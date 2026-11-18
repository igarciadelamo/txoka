@file:Suppress("MagicNumber")

package es.igarciadelamo.txoka.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import es.igarciadelamo.txoka.domain.profile.CardAppearance
import es.igarciadelamo.txoka.domain.profile.CardBackground
import es.igarciadelamo.txoka.domain.profile.CardEffect
import es.igarciadelamo.txoka.domain.profile.CardMosaic
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.ui.components.artwork.rememberCardMosaicMask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.foundation.Canvas as ComposeCanvas

@Composable
fun CardSurface(
    palette: CardPalette,
    motion: CardMotion = CardMotion(),
    paused: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val stationary = palette.appearance?.effect in listOf(null, CardEffect.NONE)
    CardMaterialLayer(palette, motion, rememberCardTime(paused || stationary), modifier)
}

@Composable
internal fun CardMaterialLayer(palette: CardPalette, motion: CardMotion, time: Float, modifier: Modifier) {
    val appearance = palette.appearance ?: CardAppearance()
    var dimensions by remember { mutableStateOf(IntSize.Zero) }
    val mask = rememberCardMosaicMask(appearance.mosaic, dimensions.width, dimensions.height)
    val painter = remember { CardMaterialPainter() }
    val pixels = rememberMaterialPixels(appearance.effect, dimensions, motion, time, palette)
    ComposeCanvas(modifier.onSizeChanged { dimensions = it }) {
        drawIntoCanvas {
            painter.draw(
                it.nativeCanvas,
                size.width,
                size.height,
                palette,
                appearance,
                motion.bounded(),
                time,
                mask?.asAndroidBitmap(),
                pixels?.asAndroidBitmap(),
            )
        }
    }
}

@Composable
private fun rememberMaterialPixels(
    effect: CardEffect,
    dimensions: IntSize,
    motion: CardMotion,
    time: Float,
    palette: CardPalette,
): ImageBitmap? {
    var pixels by remember(effect) { mutableStateOf<ImageBitmap?>(null) }
    val latestMotion by rememberUpdatedState(motion)
    val latestTime by rememberUpdatedState(time)
    LaunchedEffect(effect, dimensions, palette.accent, palette.accent2) {
        if (effect in listOf(CardEffect.FIRE, CardEffect.WIND) && dimensions.width > 0 && dimensions.height > 0) {
            val width = minOf(if (effect == CardEffect.FIRE) 224 else 176, maxOf(1, dimensions.width / 2))
            val height = (width.toFloat() * dimensions.height / dimensions.width).roundToInt().coerceIn(1, 256)
            snapshotFlow { latestMotion to latestTime }.conflate().collect { (pose, clock) ->
                pixels = withContext(Dispatchers.Default) {
                    val values = CardEffectPixels.render(effect, width, height, pose, clock, palette)
                    Bitmap.createBitmap(values, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
                }
                delay(MATERIAL_FRAME_INTERVAL_MILLIS)
            }
        }
    }
    return pixels
}

private class CardMaterialPainter {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val destination = RectF()
    private val maskTransfer = PorterDuffXfermode(PorterDuff.Mode.DST_IN)

    fun draw(
        canvas: Canvas,
        width: Float,
        height: Float,
        palette: CardPalette,
        appearance: CardAppearance,
        motion: CardMotion,
        time: Float,
        mask: Bitmap?,
        effect: Bitmap?,
    ) {
        destination.set(0f, 0f, width, height)
        paint.resetMaterial()
        background(canvas, width, height, palette, appearance, motion)
        if (mask != null && appearance.mosaic != CardMosaic.NONE) {
            mosaic(canvas, width, height, palette, appearance, motion, mask)
        }
        if (appearance.background == CardBackground.HOLOGRAM) {
            paint.shader = RadialGradient(
                width * (.28f - motion.x * .24f),
                height * (.12f - motion.y * .24f),
                width * .65f,
                intArrayOf(Color.argb(25, 255, 250, 245), Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(destination, paint)
        }
        paint.resetMaterial()
        paint.color = Color.argb(if (appearance.effect == CardEffect.FIRE) 71 else 41, 0, 0, 0)
        if (appearance.background != CardBackground.FLAT || appearance.effect != CardEffect.NONE) {
            canvas.drawRect(destination, paint)
        }
        paint.resetMaterial()
        effect?.let { canvas.drawBitmap(it, null as Rect?, destination, paint) }
        canvas.save()
        canvas.scale(width / LOGICAL_WIDTH, width / LOGICAL_WIDTH)
        val logicalHeight = height * LOGICAL_WIDTH / width
        when (appearance.effect) {
            CardEffect.WATER -> drawCardWater(canvas, paint, logicalHeight, palette, motion, time)
            CardEffect.EARTH -> drawCardEarth(canvas, paint, logicalHeight, palette, motion, time)
            CardEffect.FIRE -> drawCardEmbers(canvas, paint, logicalHeight, motion, time)
            else -> Unit
        }
        canvas.restore()
    }

    private fun background(
        canvas: Canvas,
        width: Float,
        height: Float,
        palette: CardPalette,
        appearance: CardAppearance,
        motion: CardMotion,
    ) {
        val first = palette.accent.argb.toInt()
        val second = palette.accent2.argb.toInt()
        if (appearance.background == CardBackground.FLAT) {
            paint.color = if (appearance.mosaic == CardMosaic.NONE) first else second
        } else if (appearance.hasFourCorners) {
            drawFourCorners(canvas, width, height, palette, appearance, motion, paint)
            return
        } else {
            paint.shader = gradient(width, height, motion, intArrayOf(first, second))
        }
        canvas.drawRect(destination, paint)
        if (appearance.background == CardBackground.HOLOGRAM) {
            paint.shader = foil(width, height, motion, first, second)
            paint.alpha = 200
            canvas.drawRect(destination, paint)
            paint.alpha = 255
        }
    }

    private fun mosaic(
        canvas: Canvas,
        width: Float,
        height: Float,
        palette: CardPalette,
        appearance: CardAppearance,
        motion: CardMotion,
        mask: Bitmap,
    ) {
        val layer = canvas.saveLayer(destination, null)
        paint.resetMaterial()
        val first = palette.accent.argb.toInt()
        val second = palette.accent2.argb.toInt()
        when (appearance.background) {
            CardBackground.FLAT -> paint.color = first
            CardBackground.GRADIENT -> {
                paint.shader = gradient(width, height, motion, intArrayOf(second, first))
                paint.alpha = 148
            }
            CardBackground.HOLOGRAM -> {
                paint.shader = foil(width, height, motion, second, first, .45f)
                paint.alpha = 204
            }
        }
        canvas.drawRect(destination, paint)
        paint.resetMaterial()
        paint.xfermode = maskTransfer
        canvas.drawBitmap(mask, null as Rect?, destination, paint)
        paint.xfermode = null
        canvas.restoreToCount(layer)
    }
}

private fun gradient(width: Float, height: Float, motion: CardMotion, colors: IntArray): Shader {
    val angle = motion.x * 1.85f + motion.y * 1.55f
    val dx = .58f * cos(angle) - .42f * sin(angle)
    val dy = .58f * sin(angle) + .42f * cos(angle)
    val extent = abs(dx) + abs(dy)
    return LinearGradient(
        width * (.5f - dx * extent),
        height * (.5f - dy * extent),
        width * (.5f + dx * extent),
        height * (.5f + dy * extent),
        colors,
        null,
        Shader.TileMode.CLAMP,
    )
}

private fun foil(width: Float, height: Float, motion: CardMotion, first: Int, second: Int, phase: Float = 0f): Shader {
    val colors = IntArray(17) { index -> foilColor(index / 16f + phase, motion, first, second) }
    return gradient(width, height, CardMotion(motion.x * .7f, motion.y * .7f), colors)
}

private fun drawFourCorners(
    canvas: Canvas,
    width: Float,
    height: Float,
    palette: CardPalette,
    appearance: CardAppearance,
    motion: CardMotion,
    paint: Paint,
) {
    val colors = intArrayOf(
        palette.accent.argb.toInt(),
        requireNotNull(appearance.topRight).argb.toInt(),
        requireNotNull(appearance.bottomLeft).argb.toInt(),
        palette.accent2.argb.toInt(),
    )
    val vertices = FloatArray(16 * 10 * 12)
    val vertexColors = IntArray(vertices.size / 2)
    val angle = motion.x * 1.85f + motion.y * 1.55f
    val cosine = cos(angle)
    val sine = sin(angle)
    var vertex = 0
    for (row in 0 until 10) {
        for (column in 0 until 16) {
            val points = arrayOf(0 to 0, 1 to 0, 1 to 1, 0 to 0, 1 to 1, 0 to 1)
            points.forEach { (dx, dy) ->
                val x = (column + dx) / 16f
                val y = (row + dy) / 10f
                vertices[vertex * 2] = x * width
                vertices[vertex * 2 + 1] = y * height
                val u = ((x - .5f) * cosine - (y - .5f) * sine + .5f).coerceIn(0f, 1f)
                val v = ((x - .5f) * sine + (y - .5f) * cosine + .5f).coerceIn(0f, 1f)
                vertexColors[vertex] = bilinearColor(colors, u, v)
                vertex++
            }
        }
    }
    paint.shader = null
    canvas.drawVertices(
        Canvas.VertexMode.TRIANGLES, vertices.size, vertices, 0, null, 0, vertexColors, 0, null, 0, 0, paint,
    )
    if (appearance.background == CardBackground.HOLOGRAM) {
        paint.shader = foil(width, height, motion, colors[0], colors[3])
        paint.alpha = 170
        canvas.drawRect(0f, 0f, width, height, paint)
        paint.resetMaterial()
    }
}

private fun bilinearColor(colors: IntArray, x: Float, y: Float): Int {
    fun channel(shift: Int): Float =
        (colors[0] shr shift and 255) * (1 - x) * (1 - y) + (colors[1] shr shift and 255) * x * (1 - y) +
            (colors[2] shr shift and 255) * (1 - x) * y + (colors[3] shr shift and 255) * x * y
    return argb(1f, channel(16), channel(8), channel(0))
}

internal fun Paint.resetMaterial() {
    reset()
    isAntiAlias = true
    isFilterBitmap = true
    style = Paint.Style.FILL
}

private const val LOGICAL_WIDTH = 360f
private const val MATERIAL_FRAME_INTERVAL_MILLIS = 50L
