@file:Suppress("MagicNumber")

package es.igarciadelamo.txoka.ui.components

import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.RgbColor
import kotlin.math.floor
import kotlin.math.roundToInt

/** The approved colour artwork is immutable; optical fields never replace its shapes or highlights. */
internal class CardMaterialArtwork private constructor(private val pixels: IntArray) {
    fun raster(width: Int, height: Int): IntArray {
        require(width in 1..480 && height in 1..320)
        if (width == WIDTH && height == HEIGHT) return pixels.copyOf()
        return IntArray(width * height) { index ->
            val x = ((index % width + .5f) * WIDTH / width - .5f).coerceIn(0f, WIDTH - 1f)
            val y = ((index / width + .5f) * HEIGHT / height - .5f).coerceIn(0f, HEIGHT - 1f)
            val ix = floor(x).toInt()
            val iy = floor(y).toInt()
            val right = minOf(ix + 1, WIDTH - 1)
            val bottom = minOf(iy + 1, HEIGHT - 1)
            val fx = x - ix
            val fy = y - iy
            fun channel(shift: Int): Float {
                val a = (pixels[iy * WIDTH + ix] shr shift and 255).toFloat()
                val b = (pixels[iy * WIDTH + right] shr shift and 255).toFloat()
                val c = (pixels[bottom * WIDTH + ix] shr shift and 255).toFloat()
                val d = (pixels[bottom * WIDTH + right] shr shift and 255).toFloat()
                return (a + (b - a) * fx) * (1 - fy) + (c + (d - c) * fx) * fy
            }
            argb(1f, channel(16), channel(8), channel(0))
        }
    }

    companion object {
        const val WIDTH = 480
        const val HEIGHT = 303
        val ReferencePalette = CardPalette(RgbColor(225, 93, 104), RgbColor(17, 166, 172))

        fun fromBytes(bytes: ByteArray): CardMaterialArtwork {
            require(bytes.size == WIDTH * HEIGHT * 3)
            return CardMaterialArtwork(
                IntArray(WIDTH * HEIGHT) { index ->
                    (255 shl 24) or ((bytes[index * 3].toInt() and 255) shl 16) or
                        ((bytes[index * 3 + 1].toInt() and 255) shl 8) or (bytes[index * 3 + 2].toInt() and 255)
                },
            )
        }
    }
}

private val LinearColour = FloatArray(256) { index ->
    val value = index / 255.0
    (if (value <= .04045) value / 12.92 else Math.pow((value + .055) / 1.055, 2.4)).toFloat()
}

private val DisplayColour = FloatArray(4097) { index ->
    val value = index / 4096.0
    ((if (value <= .0031308) value * 12.92 else 1.055 * Math.pow(value, 1 / 2.4) - .055) * 255).toFloat()
}

internal data class MaterialLab(val lightness: Float, val a: Float, val b: Float) {
    companion object {
        fun fromArgb(color: Int): MaterialLab {
            val r = LinearColour[color shr 16 and 255]
            val g = LinearColour[color shr 8 and 255]
            val b = LinearColour[color and 255]
            val l = Math.cbrt((r * .4122214708f + g * .5363325363f + b * .0514459929f).toDouble()).toFloat()
            val m = Math.cbrt((r * .2119034982f + g * .6806995451f + b * .1073969566f).toDouble()).toFloat()
            val s = Math.cbrt((r * .0883024619f + g * .2817188376f + b * .6299787005f).toDouble()).toFloat()
            return MaterialLab(
                l * .2104542553f + m * .7936177850f - s * .0040720468f,
                l * 1.9779984951f - m * 2.4285922050f + s * .4505937099f,
                l * .0259040371f + m * .7827717662f - s * .8086757660f,
            )
        }

        fun toArgb(lightness: Float, a: Float, b: Float): Int {
            val ll = lightness + .3963377774f * a + .2158037573f * b
            val mm = lightness - .1055613458f * a - .0638541728f * b
            val ss = lightness - .0894841775f * a - 1.2914855480f * b
            val l = ll * ll * ll
            val m = mm * mm * mm
            val s = ss * ss * ss
            return argb(
                1f,
                display(4.0767416621f * l - 3.3077115913f * m + .2309699292f * s),
                display(-1.2684380046f * l + 2.6097574011f * m - .3413193965f * s),
                display(-.0041960863f * l - .7034186147f * m + 1.7076147010f * s),
            )
        }

        private fun display(value: Float): Float {
            val position = unit(value) * 4096
            val index = position.toInt().coerceAtMost(4095)
            return DisplayColour[index] + (DisplayColour[index + 1] - DisplayColour[index]) * (position - index)
        }
    }
}

internal fun artworkGain(color: Int, gain: Float, hue: Float, palette: CardPalette): Int {
    fun channel(shift: Int, first: Int, second: Int): Int =
        (((color shr shift and 255) * gain + (second - first) * hue).roundToInt()).coerceIn(0, 255)
    return (255 shl 24) or (channel(16, palette.accent.red, palette.accent2.red) shl 16) or
        (channel(8, palette.accent.green, palette.accent2.green) shl 8) or
        channel(0, palette.accent.blue, palette.accent2.blue)
}
