@file:Suppress("MagicNumber")

package es.igarciadelamo.txoka.ui.components

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import es.igarciadelamo.txoka.domain.profile.CardPalette
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

internal fun drawCardWater(
    canvas: Canvas,
    paint: Paint,
    height: Float,
    palette: CardPalette,
    pose: CardMotion,
    time: Float,
) {
    val blue = mixMaterialColor(palette.accent.argb.toInt(), Color.rgb(23, 143, 197), .84f)
    val pale = mixMaterialColor(palette.accent2.argb.toInt(), Color.rgb(217, 249, 255), .88f)
    val surface = { u: Float -> waterLevel(u, height, pose, time) }
    val level = traceWater(surface)
    val body = Path(level).apply {
        lineTo(368f, height + 8)
        lineTo(-8f, height + 8)
        close()
    }
    canvas.save()
    canvas.clipPath(body)
    paint.resetMaterial()
    paint.shader = LinearGradient(
        0f,
        surface(180f),
        32 + pose.x * 30,
        height,
        intArrayOf(alphaColor(blue, .055f), alphaColor(blue, .095f), alphaColor(blue, .17f)),
        floatArrayOf(0f, .35f, 1f),
        Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, 0f, 360f, height, paint)
    drawWaterReflections(canvas, paint, height, pale, pose, time, surface)
    for (index in 0 until 23) {
        val depth = .18f + materialSeed(index + 41) * .4f
        val progress = fraction(materialSeed(index + 71) - time * (.025f + depth * .055f))
        val x = 9 + materialSeed(index + 101) * 342 + pose.x * depth * 20 +
            sin(index * 2.7f + time * 1.2f + progress * 6) * (2 + depth * 4)
        val y = surface(x) + progress * (height - surface(x) + 12) - 6 + pose.y * depth * 5
        val radius = .65f + materialSeed(index + 131) * 2.1f
        drawWaterBubble(canvas, paint, x, y, radius, .4f + depth * .35f, pale, blue, pose)
    }
    LargeBubbles.forEachIndexed { index, bubble ->
        val progress = fraction(bubble[1] - time * (.025f + bubble[3] * .040f))
        val x = bubble[0] * 360 + pose.x * bubble[3] * 22 +
            sin(index * 3.1f + time * .95f + progress * 4) * (2 + bubble[3] * 3.5f)
        val radius = bubble[2]
        val y = surface(x) + progress * (height - surface(x) + radius * 2) - radius + pose.y * bubble[3] * 6
        val fade = min(
            1f,
            min(
                max(0f, (y + radius - surface(x)) / (radius * 1.3f)),
                max(0f, (height + radius - y) / (radius * 1.3f)),
            ),
        )
        drawWaterBubble(canvas, paint, x, y, radius, (.63f + bubble[3] * .31f) * fade, pale, blue, pose)
    }
    drawWallFoam(canvas, paint, pale, pose, time, surface)
    canvas.restore()
    paint.resetMaterial()
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 3.5f
    paint.color = Color.argb(58, 11, 73, 109)
    canvas.save()
    canvas.translate(0f, 1.3f)
    canvas.drawPath(level, paint)
    canvas.restore()
    paint.strokeWidth = .95f
    paint.shader = LinearGradient(
        0f,
        0f,
        360f,
        0f,
        intArrayOf(alphaColor(pale, .35f), Color.argb(210, 246, 255, 255), alphaColor(pale, .35f)),
        floatArrayOf(0f, .5f + sin(time * .85f + pose.x) * .28f, 1f),
        Shader.TileMode.CLAMP,
    )
    canvas.drawPath(level, paint)
    paint.resetMaterial()
}

internal fun waterLevel(u: Float, height: Float, pose: CardMotion, time: Float): Float =
    height * (.43f + pose.y * .105f) - pose.x * (u - 180) * .225f +
        sin(u / 67 + time * 1.85f + pose.y * .8f) * (4.4f + abs(pose.x) * 2) +
        sin(u / 29 - time * 2.25f + pose.x * .7f) * 1.6f +
        sin(time * 1.1f + pose.y) * (u - 180) / 180 * (1.7f + abs(pose.x) * 2)

private fun traceWater(surface: (Float) -> Float): Path = Path().apply {
    moveTo(-8f, surface(-8f))
    for (x in 0..368 step 6) lineTo(x.toFloat(), surface(x.toFloat()))
}

private fun drawWaterReflections(
    canvas: Canvas,
    paint: Paint,
    height: Float,
    pale: Int,
    pose: CardMotion,
    time: Float,
    surface: (Float) -> Float,
) {
    val glowX = 75 + pose.x * 26 + sin(time * .8f) * 24
    val glowY = surface(glowX) + (height - surface(glowX)) * .24f
    paint.resetMaterial()
    paint.shader = RadialGradient(glowX, glowY, 155f, alphaColor(pale, .11f), Color.TRANSPARENT, Shader.TileMode.CLAMP)
    canvas.drawRect(0f, 0f, 360f, height, paint)
    for (index in 0 until 3) {
        val topX = 35 + index * 133 + sin(time * .92f + index * 2.1f) * 34 + pose.x * 19
        canvas.save()
        canvas.translate(topX + 12 + pose.x * 16, surface(topX) + (height - surface(topX)) * .62f)
        canvas.rotate(-12 - pose.x * 14)
        canvas.scale(27f + index * 6, max(1f, (height - surface(topX)) * .8f))
        paint.shader = RadialGradient(0f, 0f, 1f, alphaColor(pale, .13f), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawRect(-1f, -1f, 1f, 1f, paint)
        canvas.restore()
        val start = fraction(index * .37f + time * .055f) * 420 - 45
        val reflection = Path()
        for (step in 0..16) {
            val x = start + step * 6
            val y = surface(x) + 5 + index * 7 + sin(x / 23 - time * 2.1f + index) * 1.7f
            if (step == 0) reflection.moveTo(x, y) else reflection.lineTo(x, y)
        }
        paint.shader = LinearGradient(
            start,
            0f,
            start + 100,
            0f,
            intArrayOf(Color.TRANSPARENT, alphaColor(pale, .24f), Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = if (index == 0) 1.7f else 1.05f
        canvas.drawPath(reflection, paint)
        paint.style = Paint.Style.FILL
    }
}

private fun drawWaterBubble(
    canvas: Canvas,
    paint: Paint,
    x: Float,
    y: Float,
    radius: Float,
    opacity: Float,
    pale: Int,
    blue: Int,
    pose: CardMotion,
) {
    if (opacity <= 0f) return
    val bounds = RectF(x - radius, y - radius, x + radius, y + radius * (1.035f + pose.y * .025f))
    paint.resetMaterial()
    paint.shader = RadialGradient(
        x - radius * (.30f + pose.x * .08f),
        y - radius * (.34f + pose.y * .08f),
        radius * 1.25f,
        intArrayOf(
            alphaColor(pale, .095f * opacity),
            alphaColor(blue, .016f * opacity),
            alphaColor(pale, .065f * opacity),
            alphaColor(pale, .33f * opacity),
            alphaColor(blue, .20f * opacity),
        ),
        floatArrayOf(0f, .48f, .89f, .965f, 1f),
        Shader.TileMode.CLAMP,
    )
    canvas.drawOval(bounds, paint)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = .65f
    paint.shader = LinearGradient(
        x - radius,
        y - radius,
        x + radius,
        y + radius,
        intArrayOf(
            alphaColor(Color.WHITE, .66f * opacity),
            alphaColor(pale, .20f * opacity),
            alphaColor(blue, .20f * opacity),
            alphaColor(Color.WHITE, .49f * opacity),
        ),
        floatArrayOf(0f, .34f, .57f, 1f),
        Shader.TileMode.CLAMP,
    )
    canvas.drawOval(bounds, paint)
    paint.shader = null
    paint.color = alphaColor(Color.WHITE, .54f * opacity)
    paint.strokeWidth = .55f
    canvas.drawArc(x - radius * .82f, y - radius * .84f, x + radius * .82f, y + radius * .84f, 189f, 74f, false, paint)
    paint.style = Paint.Style.FILL
    paint.color = alphaColor(Color.WHITE, .74f * opacity)
    canvas.drawOval(x - radius * .5f, y - radius * .62f, x - radius * .22f, y - radius * .52f, paint)
}

private fun drawWallFoam(
    canvas: Canvas,
    paint: Paint,
    pale: Int,
    pose: CardMotion,
    time: Float,
    surface: (Float) -> Float,
) {
    for (side in listOf(-1, 1)) {
        val impact = unit(
            .27f + max(0f, pose.x * side) * .65f + abs(pose.y) * .24f +
                (.5f + .5f * sin(time * 1.85f + side * 1.7f)) * .23f,
        )
        for (index in 0 until 28) {
            val key = index + if (side < 0) 311 else 419
            val life = fraction(materialSeed(key) + time * (.19f + materialSeed(key + 41) * .19f))
            val fade = sin(life * PI).toFloat().pow(2)
            val reach = materialSeed(key + 13).pow(1.7f) * (13 + impact * 20)
            val x = (if (side < 0) 0f else 360f) - side *
                (reach + sin(life * 6 + key + time) * (1.4f + impact * 1.8f))
            val y = surface(x) + .8f + (1 - life) * (3 + materialSeed(key + 21) * 15)
            val radius = .5f + materialSeed(key + 31) * (1.1f + impact * .9f)
            paint.resetMaterial()
            paint.color = alphaColor(pale, fade * (.05f + impact * .12f))
            canvas.drawCircle(x, y, radius, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = .35f + impact * .16f
            paint.color = alphaColor(Color.WHITE, fade * (.30f + impact * .36f))
            canvas.drawCircle(x, y, radius, paint)
            paint.resetMaterial()
            paint.color = alphaColor(Color.WHITE, fade * (.42f + impact * .22f))
            canvas.drawCircle(x - radius * .28f, y - radius * .32f, max(.22f, radius * .22f), paint)
        }
    }
}

internal fun alphaColor(color: Int, alpha: Float): Int = (color and 0xFFFFFF) or ((unit(alpha) * 255).toInt() shl 24)

internal fun mixMaterialColor(first: Int, second: Int, amount: Float): Int {
    fun channel(shift: Int): Float = (first shr shift and 255) * (1 - amount) + (second shr shift and 255) * amount
    return argb(1f, channel(16), channel(8), channel(0))
}

private val LargeBubbles = arrayOf(
    floatArrayOf(.15f, .25f, 18f, .83f),
    floatArrayOf(.79f, .18f, 13f, .57f),
    floatArrayOf(.56f, .76f, 22f, .96f),
    floatArrayOf(.23f, .85f, 12f, .48f),
    floatArrayOf(.90f, .65f, 17f, .72f),
    floatArrayOf(.065f, .58f, 9f, .35f),
    floatArrayOf(.72f, .45f, 10f, .42f),
)
