@file:Suppress("MagicNumber")

package es.igarciadelamo.txoka.ui.components

import es.igarciadelamo.txoka.domain.profile.CardBackground
import es.igarciadelamo.txoka.domain.profile.CardPalette
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** All geometry stays in the RGB artwork; these cached fields only relight and recolour its pixels. */
internal class CardArtworkField(
    private val width: Int,
    private val height: Int,
    private val background: CardBackground,
    relief: CardMaterialRelief?,
    artwork: CardMaterialArtwork,
    cells: CardMaterialCells?,
) {
    private val source = artwork.raster(width, height)
    private val lightness = FloatArray(source.size)
    private val a = FloatArray(source.size)
    private val b = FloatArray(source.size)
    private val dye = FloatArray(source.size)
    private val mix = FloatArray(source.size)
    private val nx = FloatArray(source.size)
    private val ny = FloatArray(source.size)
    private val nz = FloatArray(source.size)
    private val baseline = FloatArray(source.size)
    private val filmSin = FloatArray(source.size)
    private val filmCos = FloatArray(source.size)
    private var chosenPalette: CardPalette? = null
    private var colours = source
    private val first = MaterialLab.fromArgb(CardMaterialArtwork.ReferencePalette.accent.argb.toInt())
    private val second = MaterialLab.fromArgb(CardMaterialArtwork.ReferencePalette.accent2.argb.toInt())

    init {
        val regions = if (background == CardBackground.STAINED_GLASS) cells?.raster(width, height) else null
        val count = maxOf(0, regions?.maxOrNull() ?: 0) + 1
        val planesX = FloatArray(count) { (materialSeed(it * 13 + 6) - .5f) * .38f }
        val planesY = FloatArray(count) { (materialSeed(it * 17 + 3) - .5f) * .38f }
        source.indices.forEach { index ->
            val u = (index % width + .5f) / width
            val v = (index / width + .5f) / height
            val lab = MaterialLab.fromArgb(source[index])
            lightness[index] = lab.lightness
            a[index] = lab.a
            b[index] = lab.b
            dye[index] = smooth((sqrt(lab.a * lab.a + lab.b * lab.b) - .012f) * 18)
            val da = second.a - first.a
            val db = second.b - first.b
            mix[index] = unit(((lab.a - first.a) * da + (lab.b - first.b) * db) / (da * da + db * db))
            var sx = (u - .5f) * .24f
            var sy = (v - .5f) * .18f - .12f
            if (regions != null) {
                sx += planesX[regions[index].coerceAtLeast(0)]
                sy += planesY[regions[index].coerceAtLeast(0)]
            }
            if (relief != null) {
                sx += (relief.sample(u + .004f, v) - relief.sample(u - .004f, v)) * 1.1f
                sy += (relief.sample(u, v + .004f) - relief.sample(u, v - .004f)) * 1.1f
            }
            val length = sqrt(1 + sx * sx + sy * sy)
            nx[index] = sx / length
            ny[index] = sy / length
            nz[index] = 1 / length
            baseline[index] = response(index, ArtworkLight.Neutral)
            val phase = (relief?.sample(u, v) ?: noise(u * 4, v * 4)) * 9.4f
            filmSin[index] = sin(phase)
            filmCos[index] = cos(phase)
        }
    }

    fun render(palette: CardPalette, motion: CardMotion): IntArray {
        if (chosenPalette != palette) {
            colours = recolour(palette)
            chosenPalette = palette
        }
        val pose = motion.bounded()
        if (pose.x == 0f && pose.y == 0f) return colours.copyOf()
        val light = ArtworkLight.at(pose)
        val phase = pose.x * 2.1f + pose.y * 2.7f
        val phaseCos = cos(phase)
        val phaseSin = sin(phase)
        return IntArray(source.size) { index ->
            val exposure = (response(index, light) - baseline[index]).coerceIn(-.12f, .12f)
            val hue = if (background == CardBackground.NACRE) {
                (filmSin[index] * (phaseCos - 1) + filmCos[index] * phaseSin) * .035f * dye[index]
            } else {
                0f
            }
            artworkGain(colours[index], 1 + exposure, hue, palette)
        }
    }

    private fun response(index: Int, light: ArtworkLight): Float {
        val dot = max(0f, nx[index] * light.x + ny[index] * light.y + nz[index] * light.z)
        val p2 = dot * dot
        val p8 = p2 * p2 * p2 * p2
        if (background == CardBackground.BRUSHED_METAL) {
            val tangent = light.x * .955f - light.y * .296f - nx[index] * .45f
            val across = light.x * .296f + light.y * .955f - ny[index] * .75f
            val distribution = tangent * tangent / .36f + across * across / .025f + light.z * light.z
            return dot * .25f + .16f / (distribution * distribution * 1.9f)
        }
        val specular = if (background == CardBackground.STAINED_GLASS) p8 * p8 * p8 * p8 else p8
        return dot * .28f + specular * .18f
    }

    private fun recolour(palette: CardPalette): IntArray {
        val appearance = palette.appearance
        if (matchesReferencePalette(palette)) return source
        val targetFirst = MaterialLab.fromArgb(palette.accent.argb.toInt())
        val targetSecond = MaterialLab.fromArgb(palette.accent2.argb.toInt())
        val top = appearance?.topRight?.let { MaterialLab.fromArgb(it.argb.toInt()) } ?: targetSecond
        val bottom = appearance?.bottomLeft?.let { MaterialLab.fromArgb(it.argb.toInt()) } ?: targetFirst
        return IntArray(source.size) { index ->
            if (dye[index] == 0f) return@IntArray source[index]
            val t = mix[index]
            val u = (index % width + .5f) / width
            val v = (index / width + .5f) / height
            val tr = u * (1 - v)
            val bl = (1 - u) * v
            val deltaL = (targetFirst.lightness - first.lightness) * (1 - t) +
                (targetSecond.lightness - second.lightness) * t +
                (top.lightness - targetSecond.lightness) * tr + (bottom.lightness - targetFirst.lightness) * bl
            val deltaA = (targetFirst.a - first.a) * (1 - t) + (targetSecond.a - second.a) * t +
                (top.a - targetSecond.a) * tr + (bottom.a - targetFirst.a) * bl
            val deltaB = (targetFirst.b - first.b) * (1 - t) + (targetSecond.b - second.b) * t +
                (top.b - targetSecond.b) * tr + (bottom.b - targetFirst.b) * bl
            MaterialLab.toArgb(
                unit(lightness[index] + deltaL * dye[index]),
                a[index] + deltaA * dye[index],
                b[index] + deltaB * dye[index],
            )
        }
    }

    private fun matchesReferencePalette(palette: CardPalette): Boolean {
        val reference = CardMaterialArtwork.ReferencePalette
        val matchesBase = palette.accent == reference.accent && palette.accent2 == reference.accent2
        return matchesBase && matchesReferenceCorners(palette)
    }

    private fun matchesReferenceCorners(palette: CardPalette): Boolean {
        val appearance = palette.appearance ?: return true
        val reference = CardMaterialArtwork.ReferencePalette
        val matchesTop = appearance.topRight == null || appearance.topRight == reference.accent2
        val matchesBottom = appearance.bottomLeft == null || appearance.bottomLeft == reference.accent
        return matchesTop && matchesBottom
    }
}

private data class ArtworkLight(val x: Float, val y: Float, val z: Float) {
    companion object {
        val Neutral = ArtworkLight(-.20f, -.20f, .958f)

        fun at(motion: CardMotion): ArtworkLight {
            val yaw = motion.x * .82f
            val pitch = motion.y * .82f
            val x = Neutral.x * cos(yaw) - Neutral.z * sin(yaw)
            val z = Neutral.x * sin(yaw) + Neutral.z * cos(yaw)
            return ArtworkLight(x, Neutral.y * cos(pitch) - z * sin(pitch), Neutral.y * sin(pitch) + z * cos(pitch))
        }
    }
}
