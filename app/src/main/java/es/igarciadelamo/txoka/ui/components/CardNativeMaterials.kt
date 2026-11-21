@file:Suppress("MagicNumber")

package es.igarciadelamo.txoka.ui.components

import es.igarciadelamo.txoka.domain.profile.CardBackground
import es.igarciadelamo.txoka.domain.profile.CardPalette
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

internal val CardBackground.hasNativeTexture: Boolean
    get() = this == CardBackground.STAINED_GLASS || this == CardBackground.NACRE || this == CardBackground.BRUSHED_METAL

/** One bounded field per visible surface. Geometry and microscopic texture never depend on motion. */
internal class CardMaterialTexture(
    private val width: Int,
    private val height: Int,
    private val relief: CardMaterialRelief? = null,
    private val artwork: CardMaterialArtwork? = null,
    private val cells: CardMaterialCells? = null,
) {
    init {
        require(width in 1..480 && height in 1..320)
    }

    private var material: CardBackground? = null
    private var field: OpticalField? = null
    private var chosenPalette: CardPalette? = null
    private var channels: MaterialPalette? = null
    private var artworkField: CardArtworkField? = null

    fun render(background: CardBackground, palette: CardPalette, motion: CardMotion): IntArray {
        require(background.hasNativeTexture)
        if (artwork != null) {
            if (material != background || artworkField == null) {
                artworkField = CardArtworkField(width, height, background, relief, artwork, cells)
                material = background
            }
            return requireNotNull(artworkField).render(palette, motion)
        }
        if (material != background) {
            field = when (background) {
                CardBackground.STAINED_GLASS -> GlassField(width, height, relief)
                CardBackground.NACRE -> NacreField(width, height, relief)
                else -> MetalField(width, height)
            }
            material = background
        }
        if (chosenPalette != palette) {
            channels = MaterialPalette(palette)
            chosenPalette = palette
        }
        return requireNotNull(field).render(requireNotNull(channels), motion.bounded())
    }
}

private interface OpticalField {
    fun render(palette: MaterialPalette, motion: CardMotion): IntArray
}

private class MaterialPalette(palette: CardPalette) {
    private val first = palette.accent.argb.toInt()
    private val second = palette.accent2.argb.toInt()
    private val top = palette.appearance?.topRight?.argb?.toInt()
    private val bottom = palette.appearance?.bottomLeft?.argb?.toInt()

    fun channel(shift: Int, u: Float, v: Float, mix: Float): Float {
        val a = (first shr shift and 255).toFloat()
        val b = (second shr shift and 255).toFloat()
        if (top == null || bottom == null) return a + (b - a) * mix
        val c = (top shr shift and 255).toFloat()
        val d = (bottom shr shift and 255).toFloat()
        return (a * (1 - u) + c * u) * (1 - v) + (d * (1 - u) + b * u) * v
    }
}

private data class SurfaceLight(val x: Float, val y: Float, val z: Float) {
    fun reflection(nx: Float, ny: Float, nz: Float): Float = max(0f, nx * x + ny * y + nz * z)

    companion object {
        // A fixed overhead light and viewer, transformed into the tilted card's coordinates.
        fun at(motion: CardMotion): SurfaceLight {
            val yaw = motion.x * .82f
            val pitch = motion.y * .82f
            val x = -.20f * cos(yaw) - .958f * sin(yaw)
            val z = -.20f * sin(yaw) + .958f * cos(yaw)
            return SurfaceLight(x, -.20f * cos(pitch) - z * sin(pitch), -.20f * sin(pitch) + z * cos(pitch))
        }
    }
}

private data class GlassPane(val x: Float, val y: Float, val nx: Float, val ny: Float, val mix: Float)

private data class PaneLocation(val cell: Int, val edge: Float, val outwardX: Float, val outwardY: Float)

private class GlassField(private val width: Int, private val height: Int, private val relief: CardMaterialRelief?) :
    OpticalField {
    private val panes = List(24) { index ->
        GlassPane(
            (index % 6 - .35f + materialSeed(index * 7 + 3) * .85f) / 4.8f,
            (index / 6 - .35f + materialSeed(index * 7 + 9) * .85f) / 2.8f,
            (materialSeed(index * 11 + 5) - .5f) * .42f,
            (materialSeed(index * 13 + 8) - .5f) * .42f,
            if (materialSeed(index * 17 + 9) > .57f) .97f else .03f,
        )
    }
    private val pourCos = FloatArray(panes.size) { cos(materialSeed(it + 109) * 6.283f) }
    private val pourSin = FloatArray(panes.size) { sin(materialSeed(it + 109) * 6.283f) }
    private val pane = IntArray(width * height)
    private val edge = FloatArray(width * height)
    private val nx = FloatArray(width * height)
    private val ny = FloatArray(width * height)
    private val nz = FloatArray(width * height)
    private val body = FloatArray(width * height)
    private val rim = FloatArray(width * height)
    private val lead = FloatArray(width * height)
    private val bubbles = FloatArray(width * height)
    private val colours = Array(panes.size) { FloatArray(3) }
    private val transmission = Array(3) { FloatArray(width * height) }
    private var chosenPalette: MaterialPalette? = null

    init {
        for (index in pane.indices) {
            val u = (index % width + .5f) / width
            val v = (index / width + .5f) / height
            val cell = closestPane(u, v)
            pane[index] = cell.cell
            edge[index] = cell.edge
            val p = panes[cell.cell]
            val dx = (u - p.x) * 1.586f
            val dy = v - p.y
            val warp = fbm(u * 5.7f + cell.cell, v * 6.4f + 3)
            var hx = (glassHeight(dx + .001f, dy, cell.cell) - glassHeight(dx - .001f, dy, cell.cell)) * 500
            var hy = (glassHeight(dx, dy + .001f, cell.cell) - glassHeight(dx, dy - .001f, cell.cell)) * 500
            if (relief != null) {
                hx = hx * .35f + (relief.sample(u + .004f, v) - relief.sample(u - .004f, v)) * 2.6f
                hy = hy * .35f + (relief.sample(u, v + .004f) - relief.sample(u, v - .004f)) * 2.6f
            }
            val bevel = exp(-max(0f, cell.edge - .0042f) * 240) * .58f
            var sx = p.nx + dx * 1.2f + hx + cell.outwardX * bevel
            var sy = p.ny + dy * .85f + hy + cell.outwardY * bevel
            val length = sqrt(1 + sx * sx + sy * sy)
            sx /= length
            sy /= length
            nx[index] = sx
            ny[index] = sy
            nz[index] = 1 / length
            val absorption = .28f + .72f * smooth((cell.edge - .0042f) * 62)
            body[index] = absorption * (.88f + .12f * warp)
            rim[index] = exp(-abs(cell.edge - .0053f) * 1100)
            lead[index] = .45f + .30f * max(0f, -cell.outwardX * .5f - cell.outwardY * .8f)
            bubbles[index] = glassBubble(u, v)
        }
    }

    private fun glassHeight(x: Float, y: Float, cell: Int): Float {
        val a = x * pourCos[cell] + y * pourSin[cell]
        val b = y * pourCos[cell] - x * pourSin[cell]
        val warp = fbm(a * 7 + cell + 4, b * 9 + cell + 17)
        return .006f * fbm(a * 24 + warp * 3, b * 7 + warp * 1.5f) + .003f * noise(a * 8 + cell, b * 11)
    }

    private fun glassBubble(u: Float, v: Float): Float {
        val column = floor(u * 39).toInt()
        val row = floor(v * 25).toInt()
        val seed = column + row * 71
        if (materialSeed(seed * 7 + 41) > .07f) return 0f
        val cx = (column + .2f + materialSeed(seed + 31) * .6f) / 39
        val cy = (row + .2f + materialSeed(seed + 61) * .6f) / 25
        val dx = (u - cx) * 1.586f
        val dy = v - cy
        val radius = .0025f + materialSeed(seed + 83) * .002f
        val distance = sqrt(dx * dx + dy * dy) / radius
        if (distance > 1.5f) return 0f
        return exp(-(distance - .86f) * (distance - .86f) * 27) * (dy / radius * -.7f + .3f)
    }

    private fun closestPane(u: Float, v: Float): PaneLocation {
        var first = 0
        var nearest = Float.MAX_VALUE
        panes.forEachIndexed { index, point ->
            val dx = (u - point.x) * 1.586f
            val dy = v - point.y
            val distance = dx * dx + dy * dy
            if (distance < nearest) {
                first = index
                nearest = distance
            }
        }
        val center = panes[first]
        var boundary = Float.MAX_VALUE
        var outwardX = 0f
        var outwardY = 0f
        panes.forEachIndexed { index, point ->
            if (index != first) {
                val dx = (u - point.x) * 1.586f
                val dy = v - point.y
                val sx = (point.x - center.x) * 1.586f
                val sy = point.y - center.y
                val length = sqrt(sx * sx + sy * sy)
                val distance = (dx * dx + dy * dy - nearest) / (2 * length)
                if (distance < boundary) {
                    boundary = distance
                    outwardX = sx / length
                    outwardY = sy / length
                }
            }
        }
        return PaneLocation(first, boundary, outwardX, outwardY)
    }

    override fun render(palette: MaterialPalette, motion: CardMotion): IntArray {
        val light = SurfaceLight.at(motion)
        if (chosenPalette !== palette) {
            panes.forEachIndexed { index, p ->
                colours[index][0] = palette.channel(16, unit(p.x), unit(p.y), p.mix)
                colours[index][1] = palette.channel(8, unit(p.x), unit(p.y), p.mix)
                colours[index][2] = palette.channel(0, unit(p.x), unit(p.y), p.mix)
            }
            pane.indices.forEach { index ->
                val colour = colours[pane[index]]
                val depth = .25f + (1 - body[index]) * 1.4f
                for (channel in 0..2) {
                    transmission[channel][index] = colour[channel] * body[index] *
                        exp(-(1 - colour[channel] / 255) * depth)
                }
            }
            chosenPalette = palette
        }
        return IntArray(pane.size) { index ->
            if (edge[index] < .0035f) {
                val curve = sin(edge[index] / .0035f * 1.57f)
                val highlight = curve * curve * lead[index]
                argb(1f, 15 + highlight * 37, 18 + highlight * 34, 19 + highlight * 31)
            } else {
                glassPixel(index, light)
            }
        }
    }

    private fun glassPixel(index: Int, light: SurfaceLight): Int {
        val reflection = light.reflection(nx[index], ny[index], nz[index])
        val p2 = reflection * reflection
        val p4 = p2 * p2
        val p16 = p4 * p4 * p4 * p4
        val p64 = p16 * p16 * p16 * p16
        val glint = p64 * p64
        val illumination = .67f + .30f * p4
        val shine = p16 * 19 + p64 * 38 + glint * 91 + rim[index] * (16 + p16 * 38) + bubbles[index] * 50
        return argb(
            1f,
            transmission[0][index] * illumination + shine * 1.08f,
            transmission[1][index] * illumination + shine * .99f,
            transmission[2][index] * illumination + shine * .88f,
        )
    }
}

private class NacreField(private val width: Int, private val height: Int, private val relief: CardMaterialRelief?) :
    OpticalField {
    private val count = width * height
    private val flow = FloatArray(count)
    private val nx = FloatArray(count)
    private val ny = FloatArray(count)
    private val nz = FloatArray(count)
    private val body = FloatArray(count)
    private val edges = FloatArray(count)
    private val redCos = FloatArray(count)
    private val redSin = FloatArray(count)
    private val greenCos = FloatArray(count)
    private val greenSin = FloatArray(count)
    private val blueCos = FloatArray(count)
    private val blueSin = FloatArray(count)

    init {
        for (index in 0 until count) {
            val u = (index % width + .5f) / width
            val v = (index / width + .5f) / height
            if (relief != null) {
                reliefPixel(index, u, v, relief)
                continue
            }
            val shell = shell(u, v)
            val du = (shell(u + .001f, v) - shell(u - .001f, v)) / .002f
            val dv = (shell(u, v + .001f) - shell(u, v - .001f)) / .002f
            val layer = fraction(shell)
            val dome = sin(layer * 3.14159f)
            val lip = exp(-layer * 35)
            val crest = exp(-(layer - .08f) * (layer - .08f) * 700)
            val warp = fbm(u * 11 + 3, v * 13 + 17)
            var sx = du * (.025f + .018f * cos(layer * 6.28f)) + (warp - .5f) * .27f
            var sy = dv * (.025f + .018f * cos(layer * 6.28f)) - .22f + (warp - .5f) * .20f
            val length = sqrt(1 + sx * sx + sy * sy)
            sx /= length
            sy /= length
            nx[index] = sx
            ny[index] = sy
            nz[index] = 1 / length
            val striae = noise(shell * 65, u * 18 + warp * 8)
            val crystalRow = floor(shell * 5).toInt()
            val crystalColumn = floor(u * 24 + noise(shell * 2, u * 3) * 7).toInt()
            val crystal = materialSeed(crystalRow * 71 + crystalColumn * 11)
            val segment = smooth((noise(u * 7 + 18, v * 8 + 4) - .25f) * 2.5f)
            flow[index] = unit(u * .61f + v * .27f + (warp - .5f) * .35f + sin(shell * 2) * .13f)
            body[index] = .79f + dome * .12f + striae * .08f - lip * .15f * segment + crystal * .04f
            edges[index] = crest * .22f * segment + (striae - .5f) * .075f + crystal * .025f
            val thickness = shell * 1.2f + warp * 2.3f + dome * .7f + crystal * .65f
            redCos[index] = cos(thickness * .83f)
            redSin[index] = sin(thickness * .83f)
            greenCos[index] = cos(thickness * 1.09f + 2.1f)
            greenSin[index] = sin(thickness * 1.09f + 2.1f)
            blueCos[index] = cos(thickness * 1.31f + 4.2f)
            blueSin[index] = sin(thickness * 1.31f + 4.2f)
        }
    }

    private fun smoothRelief(relief: CardMaterialRelief, u: Float, v: Float): Float {
        val nearby = relief.sample(u + .009f, v) + relief.sample(u - .009f, v) +
            relief.sample(u, v + .009f) + relief.sample(u, v - .009f)
        return relief.sample(u, v) * .4f + nearby * .15f
    }

    private fun reliefPixel(index: Int, u: Float, v: Float, relief: CardMaterialRelief) {
        val height = relief.sample(u, v)
        val broad = smoothRelief(relief, u, v)
        val sx = (smoothRelief(relief, u + .014f, v) - smoothRelief(relief, u - .014f, v)) * .75f +
            (relief.sample(u + .004f, v) - relief.sample(u - .004f, v)) * 1.05f + (u - .5f) * .24f
        val sy = (smoothRelief(relief, u, v + .014f) - smoothRelief(relief, u, v - .014f)) * .75f +
            (relief.sample(u, v + .004f) - relief.sample(u, v - .004f)) * 1.05f + (v - .5f) * .18f - .17f
        val length = sqrt(1 + sx * sx + sy * sy)
        nx[index] = sx / length
        ny[index] = sy / length
        nz[index] = 1 / length
        flow[index] = unit(u * .65f + v * .35f + (broad - .5f) * .22f)
        body[index] = .83f + broad * .18f + (height - broad) * .09f
        edges[index] = unit((height - broad) * 3) * .06f
        val film = broad * 13.4f + height * 1.3f + fbm(u * 4, v * 4) * 1.4f
        redCos[index] = cos(film)
        redSin[index] = sin(film)
    }

    private fun pearlescentPixel(
        index: Int,
        palette: MaterialPalette,
        light: SurfaceLight,
        phaseCos: Float,
        phaseSin: Float,
    ): Int {
        val u = (index % width + .5f) / width
        val v = (index / width + .5f) / height
        val reflection = light.reflection(nx[index], ny[index], nz[index])
        val p2 = reflection * reflection
        val p8 = p2 * p2 * p2 * p2
        val p32 = p8 * p8 * p8 * p8
        val film = .5f + .5f * (redCos[index] * phaseCos - redSin[index] * phaseSin)
        val mix = smooth((flow[index] + (film - .5f) * .50f - .18f) * 1.55f)
        val strength = body[index] * (.68f + p8 * .16f)
        val pearl = .035f + p8 * .24f + p32 * .08f + edges[index] * (.35f + p8 * .65f)
        return argb(
            1f,
            palette.channel(16, u, v, mix) * strength + 255 * pearl,
            palette.channel(8, u, v, mix) * strength + 246 * pearl,
            palette.channel(0, u, v, mix) * strength + 234 * pearl,
        )
    }

    private fun shell(u: Float, v: Float): Float {
        val warp = fbm(u * 3.3f + 8.1f, v * 3.6f + 11.4f)
        val angle = kotlin.math.atan2(v + .5f, u * 1.5f + .2f)
        val radius = sqrt((u * 1.5f + .2f) * (u * 1.5f + .2f) + (v + .5f) * (v + .5f))
        return radius * 5.3f + sin(angle * 9 + warp * 3) * .28f + warp * 1.8f +
            noise(u * 10 + warp * 3, v * 7) * .25f
    }

    override fun render(palette: MaterialPalette, motion: CardMotion): IntArray {
        val light = SurfaceLight.at(motion)
        val phase = motion.x * 3.7f + motion.y * 4.9f
        val rc = cos(phase * .83f)
        val rs = sin(phase * .83f)
        val gc = cos(phase * 1.09f)
        val gs = sin(phase * 1.09f)
        val bc = cos(phase * 1.31f)
        val bs = sin(phase * 1.31f)
        return IntArray(count) { index ->
            if (relief != null) return@IntArray pearlescentPixel(index, palette, light, rc, rs)

            val u = (index % width + .5f) / width
            val v = (index / width + .5f) / height
            val reflection = light.reflection(nx[index], ny[index], nz[index])
            val p2 = reflection * reflection
            val p8 = p2 * p2 * p2 * p2
            val luster = p8 * .26f + p8 * p8 * .24f
            val strength = body[index] * (.58f + p8 * .31f)
            val redFilm = .5f + .5f * (redCos[index] * rc - redSin[index] * rs)
            val greenFilm = .5f + .5f * (greenCos[index] * gc - greenSin[index] * gs)
            val blueFilm = .5f + .5f * (blueCos[index] * bc - blueSin[index] * bs)
            val pearl = luster + edges[index] * (.25f + p8 * .75f)
            argb(
                1f,
                palette.channel(16, u, v, flow[index]) * strength + 255 * pearl + 24 * redFilm,
                palette.channel(8, u, v, flow[index]) * strength + 248 * pearl + 24 * greenFilm,
                palette.channel(0, u, v, flow[index]) * strength + 238 * pearl + 24 * blueFilm,
            )
        }
    }
}

private class MetalField(private val width: Int, private val height: Int) : OpticalField {
    private val grain = FloatArray(width * height)
    private val tangentSlope = FloatArray(width * height)
    private val crossSlope = FloatArray(width * height)
    private val environment = FloatArray(width * height)

    init {
        for (index in grain.indices) {
            val u = (index % width + .5f) / width
            val v = (index / width + .5f) / height
            val across = v + u * .48f
            val fine = noise(across * minOf(310f, height * .85f), u * 2.3f + 31)
            val hairline = noise(across * minOf(660f, height * 1.55f) + 17, u * 5.8f)
            val drift = fbm(u * 2.7f + 12, v * 3.1f + 8)
            grain[index] = .80f + fine * .28f + hairline * .12f
            tangentSlope[index] = (u - .48f) * .84f - (v - .5f) * .23f + (drift - .5f) * .06f
            crossSlope[index] = (u - .5f) * .28f + (v - .42f) * 1.16f + (fine - .5f) * .036f
            environment[index] = .86f + .14f * drift
        }
    }

    override fun render(palette: MaterialPalette, motion: CardMotion): IntArray {
        val light = SurfaceLight.at(motion)
        val tangent = light.x * .955f - light.y * .296f
        val across = light.x * .296f + light.y * .955f
        val fresnel = .06f + .26f * (1 - light.z) * (1 - light.z)
        return IntArray(grain.size) { index ->
            val u = (index % width + .5f) / width
            val v = (index / width + .5f) / height
            val ht = tangent - tangentSlope[index] * light.z
            val hb = across - crossSlope[index] * light.z
            val distribution = ht * ht / .36f + hb * hb / .0225f + light.z * light.z
            val anisotropic = 1 / (distribution * distribution * 1.9f)
            val luster = unit(anisotropic) * grain[index]
            val base = (.62f + luster * .24f) * environment[index] * grain[index]
            val reflection = luster * 104 + fresnel * 38
            val mix = smooth((u * .67f + v * .33f - .08f) * 1.18f)
            fun channel(shift: Int): Float {
                val tint = palette.channel(shift, u, v, mix)
                return tint * base + reflection * (.30f + tint / 360f)
            }
            argb(1f, channel(16), channel(8), channel(0))
        }
    }
}
