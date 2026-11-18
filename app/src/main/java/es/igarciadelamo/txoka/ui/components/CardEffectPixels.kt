@file:Suppress("MagicNumber")

package es.igarciadelamo.txoka.ui.components

import es.igarciadelamo.txoka.domain.profile.CardEffect
import es.igarciadelamo.txoka.domain.profile.CardPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Bounded, deterministic material fields; no animation state or image data enters NFC. */
internal object CardEffectPixels {
    fun render(
        effect: CardEffect,
        width: Int,
        height: Int,
        motion: CardMotion,
        time: Float,
        palette: CardPalette,
    ): IntArray {
        require(width in 1..288 && height in 1..256)
        val pose = motion.bounded()
        val clock = if (time.isFinite()) time.coerceIn(-100_000f, 100_000f) else 0f
        return IntArray(width * height) { index ->
            val u = (index % width + .5f) / width
            val v = (index / width + .5f) / height
            when (effect) {
                CardEffect.FIRE -> fire(u, v, pose, clock, palette)
                CardEffect.WIND -> mist(u, v, pose, clock, palette)
                else -> 0
            }
        }
    }

    private fun fire(u: Float, v: Float, pose: CardMotion, time: Float, palette: CardPalette): Int {
        val rise = (1 - v) / (.66f - pose.y * .11f)
        if (rise > 1.65f) return 0
        val taper = max(0f, 1 - rise * .62f)
        val flow = u - pose.x * rise * .21f
        val sway = fbm(flow * 3.2f + 2.4f, rise * 2.1f - time * .42f) - .5f
        val x = flow * 13.8f + sway * 2.4f + pose.y * .54f
        val y = rise * 5.4f - time * 1.95f
        val turbulence = fbm(x, y)
        val filaments = noise(x * 2.3f + turbulence * .8f, y * 1.27f - time * .36f)
        val fuel = .84f + noise(flow * 11.5f + 31, 4.7f) * .27f
        val density = (1 - rise) * fuel + (turbulence - .47f) * 1.65f + (filaments - .5f) * .53f - .12f
        val body = smooth((density + .025f) * 5.2f)
        val haze = smooth((density + .16f) * 2.5f) * .085f * taper
        val edge = smooth(min(u, 1 - u) * 18 + .28f)
        val heat = unit((1 - rise) * .48f + density * .30f + (filaments - .5f) * .24f + (turbulence - .5f) * .22f)
        val core = smooth((heat - .63f) * 3.5f)
        val hotSpot = smooth((heat - .9f) * 10) * body * .07f
        val alpha = ((body * .72f * taper + haze) * edge).coerceIn(0f, .78f) + hotSpot
        val orange = smooth(heat * 1.55f)
        val warmth = (palette.accent2.red - palette.accent2.blue) / 255f
        return argb(
            alpha,
            220 + orange * 35,
            28 + orange * 125 + core * 95 + warmth * 5,
            core * core * 195 + orange * 2,
        )
    }

    private fun mist(u: Float, v: Float, pose: CardMotion, time: Float, palette: CardPalette): Int {
        val flow = u - time * (.13f + pose.x * .16f) - pose.x * (.11f + v * .11f)
        val altitude = v + pose.y * .14f
        val warp = noise(flow * 2.7f + 7.3f, altitude * 3.1f - time * .035f)
        val curl = noise(flow * 3.3f + 24.6f, altitude * 4.4f + 16.2f + time * .045f)
        val fx = flow * 4.1f + warp * 1.2f
        val fy = altitude * 5.1f + curl * .74f
        val billow = fbm(fx, fy)
        val center = .31f + pose.y * .09f + sin(u * 3.1f + time * .09f + pose.x) * .055f
        val farEnvelope = gaussian((v - center) / .24f)
        val farDensity = smooth((billow - .30f) * 2.8f)
        val nearX = (u - time * (.23f + pose.x * .19f) - pose.x * .23f) * 5.9f + warp * 1.15f + 18
        val nearY = altitude * 8.4f + curl * 1.4f - time * .06f + 35
        val strands = fbm(nearX, nearY)
        val nearCenter = .73f + pose.y * .12f + sin(u * 4.3f - time * .12f) * .065f
        val nearEnvelope = gaussian((v - nearCenter) / .20f)
        val nearDensity = smooth((strands - .34f) * 3f)
        val farAlpha = farDensity * farEnvelope * .42f
        val nearAlpha = nearDensity * nearEnvelope * .36f
        val alpha = (1 - (1 - farAlpha) * (1 - nearAlpha)).coerceIn(0f, .56f)
        val incoming = noise(fx - .11f, fy - .18f)
        val outgoing = noise(fx + .11f, fy + .18f)
        val light = unit(.68f + (incoming - outgoing) * 1.6f + billow * .12f)
        fun channel(shade: Float, bright: Float, first: Int, second: Int): Float =
            (shade * .94f + first * .06f) * (1 - light) + (bright * .94f + second * .06f) * light
        return argb(
            alpha,
            channel(174f, 244f, palette.accent.red, palette.accent2.red),
            channel(199f, 251f, palette.accent.green, palette.accent2.green),
            channel(214f, 254f, palette.accent.blue, palette.accent2.blue),
        )
    }

    private fun gaussian(value: Float): Float = exp(-value * value)
}

private val NoiseField = FloatArray(64 * 64).also { field ->
    var seed = 0x6d2b79f5L
    field.indices.forEach { index ->
        seed = (seed * 1664525 + 1013904223) and 0xFFFFFFFFL
        field[index] = (seed / 4294967295.0).toFloat()
    }
}

internal fun noise(x: Float, y: Float): Float {
    val ix = floor(x).toInt()
    val iy = floor(y).toInt()
    val sx = smooth(x - ix)
    val sy = smooth(y - iy)
    val row = (iy and 63) * 64
    val next = ((iy + 1) and 63) * 64
    val a = NoiseField[row + (ix and 63)]
    val b = NoiseField[row + ((ix + 1) and 63)]
    val c = NoiseField[next + (ix and 63)]
    val d = NoiseField[next + ((ix + 1) and 63)]
    return (a + (b - a) * sx) * (1 - sy) + (c + (d - c) * sx) * sy
}

internal fun fbm(x: Float, y: Float): Float = noise(x, y) * .57f + noise(x * 2.03f + 7.1f, y * 2.03f + 1.7f) * .28f +
    noise(x * 4.11f + 13.8f, y * 4.11f + 3.4f) * .15f

internal fun unit(value: Float): Float = value.coerceIn(0f, 1f)

internal fun smooth(value: Float): Float {
    val n = unit(value)
    return n * n * (3 - 2 * n)
}

internal fun fraction(value: Float): Float = value - floor(value)

internal fun materialSeed(value: Int): Float {
    val sample = sin(value * 127.1 + 311.7) * 43758.5453123
    return (sample - floor(sample)).toFloat()
}

internal fun argb(alpha: Float, red: Float, green: Float, blue: Float): Int =
    ((unit(alpha) * 255).roundToInt() shl 24) or (red.roundToInt().coerceIn(0, 255) shl 16) or
        (green.roundToInt().coerceIn(0, 255) shl 8) or blue.roundToInt().coerceIn(0, 255)

internal fun foilColor(position: Float, motion: CardMotion, first: Int, second: Int): Int {
    val yaw = motion.x * 50 * PI / 180
    val pitch = motion.y * 50 * PI / 180
    val path = sin(yaw) * cos(pitch) * 440 + sin(pitch) * 360 + position * 510
    fun channel(shift: Int, wavelength: Int): Float {
        val a = first shr shift and 255
        val b = second shr shift and 255
        val tint = a * (1 - unit(position)) + b * unit(position)
        val film = .5 + .5 * cos(2 * PI * path / wavelength)
        return (tint * .46 + (68 + film * 187) * .54).toFloat()
    }
    return argb(1f, channel(16, 650), channel(8, 510), channel(0, 410))
}
