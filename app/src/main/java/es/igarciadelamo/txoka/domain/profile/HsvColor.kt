package es.igarciadelamo.txoka.domain.profile

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Colour in HSV, the space the picker wheel works in. It lives in the domain so the conversions
 * are unit-tested and the UI only has to draw.
 */
data class HsvColor(val hue: Float, val saturation: Float, val value: Float) {
    init {
        require(hue in HUE_RANGE && saturation in UNIT_RANGE && value in UNIT_RANGE) {
            "HSV components out of range"
        }
    }

    fun toRgb(): RgbColor {
        val sector = (hue % FULL_TURN) / SECTOR_SIZE
        val chroma = value * saturation
        val second = chroma * (1f - abs(sector % 2f - 1f))
        val offset = value - chroma
        val (red, green, blue) = when (sector.toInt()) {
            SECTOR_RED -> Triple(chroma, second, NO_COLOUR)
            SECTOR_YELLOW -> Triple(second, chroma, NO_COLOUR)
            SECTOR_GREEN -> Triple(NO_COLOUR, chroma, second)
            SECTOR_CYAN -> Triple(NO_COLOUR, second, chroma)
            SECTOR_BLUE -> Triple(second, NO_COLOUR, chroma)
            else -> Triple(chroma, NO_COLOUR, second)
        }
        return RgbColor(
            red = (red + offset).toChannel(),
            green = (green + offset).toChannel(),
            blue = (blue + offset).toChannel(),
        )
    }

    companion object {
        private const val FULL_TURN = 360f
        private const val SECTOR_SIZE = 60f
        private const val SIX_SECTORS = 6f
        private const val SECTOR_OFFSET_GREEN = 2f
        private const val SECTOR_OFFSET_BLUE = 4f
        private const val MAX_CHANNEL = 255f
        private const val MAX_CHANNEL_INT = 255
        private const val MIN_CHANNEL_INT = 0
        private const val NO_COLOUR = 0f

        private const val SECTOR_RED = 0
        private const val SECTOR_YELLOW = 1
        private const val SECTOR_GREEN = 2
        private const val SECTOR_CYAN = 3
        private const val SECTOR_BLUE = 4

        private val HUE_RANGE = 0f..FULL_TURN
        private val UNIT_RANGE = 0f..1f

        fun fromRgb(color: RgbColor): HsvColor {
            val red = color.red / MAX_CHANNEL
            val green = color.green / MAX_CHANNEL
            val blue = color.blue / MAX_CHANNEL
            val maximum = maxOf(red, green, blue)
            val delta = maximum - minOf(red, green, blue)
            return HsvColor(
                hue = hueOf(red, green, blue, maximum, delta),
                saturation = if (maximum == 0f) 0f else delta / maximum,
                value = maximum,
            )
        }

        private fun hueOf(red: Float, green: Float, blue: Float, maximum: Float, delta: Float): Float {
            if (delta == 0f) return 0f
            val raw = when {
                maximum == red -> (green - blue) / delta
                maximum == green -> (blue - red) / delta + SECTOR_OFFSET_GREEN
                else -> (red - green) / delta + SECTOR_OFFSET_BLUE
            }
            return (raw % SIX_SECTORS + SIX_SECTORS) % SIX_SECTORS * SECTOR_SIZE
        }

        private fun Float.toChannel(): Int =
            (this * MAX_CHANNEL).roundToInt().coerceIn(MIN_CHANNEL_INT, MAX_CHANNEL_INT)
    }
}
