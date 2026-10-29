package es.igarciadelamo.txoka.domain.profile

import java.util.Locale
import kotlin.math.pow

/**
 * An sRGB colour with no dependency on the UI framework, so palettes can be parsed,
 * validated and compared from pure Kotlin.
 */
data class RgbColor(val red: Int, val green: Int, val blue: Int) {
    init {
        require(red in CHANNEL_RANGE && green in CHANNEL_RANGE && blue in CHANNEL_RANGE) {
            "Colour channels must be in 0..255"
        }
    }

    val hex: String
        get() = String.format(Locale.ROOT, "#%02X%02X%02X", red, green, blue)

    val argb: Long
        get() = OPAQUE_ALPHA or (red.toLong() shl RED_SHIFT) or (green.toLong() shl GREEN_SHIFT) or blue.toLong()

    /** Relative luminance as defined by WCAG 2.1. */
    fun relativeLuminance(): Double =
        RED_WEIGHT * linearize(red) + GREEN_WEIGHT * linearize(green) + BLUE_WEIGHT * linearize(blue)

    /** True when dark text reads better than light text on top of this colour. */
    fun prefersDarkContent(): Boolean = relativeLuminance() > DARK_CONTENT_THRESHOLD

    fun lighten(amount: Double): RgbColor {
        val ratio = amount.coerceIn(0.0, 1.0)
        return RgbColor(
            red = mixWithWhite(red, ratio),
            green = mixWithWhite(green, ratio),
            blue = mixWithWhite(blue, ratio),
        )
    }

    companion object {
        private const val MIN_CHANNEL = 0
        private const val MAX_CHANNEL = 255
        private const val HEX_LENGTH = 6
        private const val HEX_RADIX = 16
        private const val RED_START = 0
        private const val GREEN_START = 2
        private const val BLUE_START = 4
        private const val RED_SHIFT = 16
        private const val GREEN_SHIFT = 8
        private const val OPAQUE_ALPHA = 0xFF000000L
        private const val DARK_CONTENT_THRESHOLD = 0.45

        private const val RED_WEIGHT = 0.2126
        private const val GREEN_WEIGHT = 0.7152
        private const val BLUE_WEIGHT = 0.0722
        private const val SRGB_THRESHOLD = 0.03928
        private const val SRGB_DIVISOR = 12.92
        private const val SRGB_OFFSET = 0.055
        private const val SRGB_SCALE = 1.055
        private const val SRGB_EXPONENT = 2.4

        private val CHANNEL_RANGE = MIN_CHANNEL..MAX_CHANNEL

        fun parse(hex: String): RgbColor? {
            val value = hex.trim().removePrefix("#")
            if (value.length != HEX_LENGTH || value.any { it.digitToIntOrNull(HEX_RADIX) == null }) return null
            return RgbColor(
                red = value.substring(RED_START, GREEN_START).toInt(HEX_RADIX),
                green = value.substring(GREEN_START, BLUE_START).toInt(HEX_RADIX),
                blue = value.substring(BLUE_START, HEX_LENGTH).toInt(HEX_RADIX),
            )
        }

        private fun linearize(channel: Int): Double {
            val normalized = channel / MAX_CHANNEL.toDouble()
            return if (normalized <= SRGB_THRESHOLD) {
                normalized / SRGB_DIVISOR
            } else {
                ((normalized + SRGB_OFFSET) / SRGB_SCALE).pow(SRGB_EXPONENT)
            }
        }

        private fun mixWithWhite(channel: Int, ratio: Double): Int =
            (channel + (MAX_CHANNEL - channel) * ratio).toInt().coerceIn(CHANNEL_RANGE)
    }
}
