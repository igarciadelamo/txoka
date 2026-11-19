package es.igarciadelamo.txoka.ui.profile

import es.igarciadelamo.txoka.domain.profile.HsvColor
import es.igarciadelamo.txoka.domain.profile.RgbColor

/** Keeps the editing coordinates separate from RGB so black does not erase hue or saturation. */
internal data class ColourDraft(val hsv: HsvColor, val hex: String) {
    val color: RgbColor get() = hsv.toRgb()
    val validHex: Boolean get() = RgbColor.parse(hex) != null

    fun withHsv(value: HsvColor): ColourDraft = ColourDraft(value, value.toRgb().hex)

    fun withHex(value: String): ColourDraft {
        val parsed = RgbColor.parse(value) ?: return copy(hex = value)
        val converted = HsvColor.fromRgb(parsed)
        val preserved = converted.copy(
            hue = if (converted.saturation == 0f) hsv.hue else converted.hue,
            saturation = if (converted.value == 0f) hsv.saturation else converted.saturation,
        )
        return ColourDraft(preserved, value)
    }

    companion object {
        fun from(color: RgbColor): ColourDraft = ColourDraft(HsvColor.fromRgb(color), color.hex)
    }
}
