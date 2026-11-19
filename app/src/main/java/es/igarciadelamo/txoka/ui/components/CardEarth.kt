@file:Suppress("MagicNumber")

package es.igarciadelamo.txoka.ui.components

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import es.igarciadelamo.txoka.domain.profile.CardPalette
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

internal fun drawCardEarth(
    canvas: Canvas,
    paint: Paint,
    height: Float,
    palette: CardPalette,
    pose: CardMotion,
    time: Float,
) {
    val sand = mixMaterialColor(palette.accent2.argb.toInt(), Color.rgb(224, 191, 132), .87f)
    val pale = mixMaterialColor(sand, Color.rgb(255, 241, 205), .72f)
    val shade = mixMaterialColor(palette.accent.argb.toInt(), Color.rgb(88, 68, 45), .83f)
    val warm = mixMaterialColor(sand, Color.rgb(187, 140, 82), .55f)
    for (band in 0 until 3) {
        val line = Path().apply {
            moveTo(-12f, dune(-12f, height, band, pose))
            for (u in -4..372 step 8) lineTo(u.toFloat(), dune(u.toFloat(), height, band, pose))
        }
        val bank = Path(line).apply {
            lineTo(372f, height + 10)
            lineTo(-12f, height + 10)
            close()
        }
        paint.resetMaterial()
        paint.shader = LinearGradient(
            0f,
            height * (.36f + band * .23f),
            0f,
            height + 5,
            intArrayOf(alphaColor(pale, .19f), alphaColor(sand, .22f), alphaColor(warm, .16f), alphaColor(shade, .17f)),
            floatArrayOf(0f, .12f, .54f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(bank, paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = .8f
        paint.color = alphaColor(pale, .30f)
        canvas.drawPath(line, paint)
    }
    drawPebbles(canvas, paint, height, pale, pose)
    drawSandGrains(canvas, paint, height, sand, pale, shade, pose, time)
}

private fun dune(u: Float, height: Float, band: Int, pose: CardMotion): Float =
    height * (.39f + band * .23f) + sin(u / 104 + band * 1.6f) * (13 + band * 3) +
        sin(u / 47 + band * .7f) * 2.2f + pose.x * (u - 180) * .055f + pose.y * (7 + band * 4)

private fun drawPebbles(canvas: Canvas, paint: Paint, height: Float, pale: Int, pose: CardMotion) {
    for (index in 0 until 18) {
        val depth = .35f + materialSeed(index + 600) * .65f
        val x = 13 + materialSeed(index + 610) * 335 + pose.x * depth * 8
        val y = height * (.50f + materialSeed(index + 620) * .43f) + pose.y * depth * 7
        val rx = 1.5f + materialSeed(index + 630) * 6.3f
        val ry = rx * (.53f + materialSeed(index + 640) * .29f)
        val rotation = (materialSeed(index + 650) - .5f) * 52
        val stone = mixMaterialColor(Color.rgb(139, 124, 99), Color.rgb(203, 188, 158), materialSeed(index + 660))
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(rotation)
        paint.resetMaterial()
        paint.color = Color.argb((33 + depth * 15).toInt(), 31, 25, 19)
        canvas.drawOval(-rx * 1.18f + 1.2f, ry * .04f, rx * 1.18f + 1.2f, ry * 1.36f, paint)
        paint.shader = RadialGradient(
            -rx * (.32f + pose.x * .12f),
            -ry * (.38f + pose.y * .1f),
            rx * 1.18f,
            intArrayOf(
                alphaColor(mixMaterialColor(stone, Color.rgb(255, 246, 221), .45f), .60f),
                alphaColor(stone, .56f),
                alphaColor(mixMaterialColor(stone, Color.rgb(59, 48, 35), .57f), .58f),
            ),
            floatArrayOf(0f, .45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawOval(-rx, -ry, rx, ry, paint)
        paint.shader = null
        paint.color = alphaColor(pale, .25f)
        canvas.drawOval(-rx * .28f, -ry * .21f, -rx * .06f, -ry * .05f, paint)
        canvas.restore()
    }
}

private fun drawSandGrains(
    canvas: Canvas,
    paint: Paint,
    height: Float,
    sand: Int,
    pale: Int,
    shade: Int,
    pose: CardMotion,
    time: Float,
) {
    val direction = 1 + pose.x * .46f
    for (index in 0 until 230) {
        val depth = .3f + materialSeed(index + 700) * .7f
        val life = fraction(materialSeed(index + 800) + time * (.026f + depth * .07f) * direction)
        val x = life * 392 - 16 + pose.x * depth * 6
        val y = height * (.24f + materialSeed(index + 900) * .74f) +
            sin(life * 12 + index) * (1 + depth * 2.6f) + pose.y * depth * 10
        val fade = sin(life * PI).toFloat()
        val radius = .25f + materialSeed(index + 1000) * .48f
        paint.resetMaterial()
        paint.color = alphaColor(if (index % 4 != 0) pale else shade, fade * (.14f + depth * .25f))
        canvas.drawOval(x - radius * (1.4f + depth), y - radius, x + radius * (1.4f + depth), y + radius, paint)
        if (index % 13 == 0) {
            paint.color = alphaColor(sand, fade * .16f)
            paint.strokeWidth = .45f
            canvas.drawLine(x - 3 - depth * 3, y + .8f, x, y, paint)
        }
    }
}

internal fun drawCardEmbers(canvas: Canvas, paint: Paint, height: Float, pose: CardMotion, time: Float) {
    for (index in 0 until 28) {
        val seed = materialSeed(index * 137 + 17)
        val life = fraction(seed + time * (.06f + materialSeed(index * 19) * .065f))
        val x = materialSeed(index * 29 + 3) * 360 + pose.x * life * 57 + sin(life * 8 + index) * 7
        val y = height * (1.03f - life * .88f) + pose.y * life * 12
        val alpha = sin(life * PI).toFloat() * (.32f + materialSeed(index * 31) * .5f)
        paint.resetMaterial()
        paint.color = Color.argb((max(0f, alpha) * 255).toInt(), 255, 218, 133)
        paint.strokeWidth = .5f + seed * .7f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(x, y, x - pose.x * 1.5f, y + 1 + seed * 2.2f, paint)
    }
}
