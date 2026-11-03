package es.igarciadelamo.txoka.domain.profile

/**
 * Two-colour gradient used to render a contact card. The second colour is always stored
 * explicitly so every device renders the same card, whichever version it runs.
 */
data class CardPalette(val accent: RgbColor, val accent2: RgbColor) {
    companion object {
        // Both stops are dark enough for white text at 4.5:1 or better.
        val Default = CardPalette(
            accent = RgbColor(0x4F, 0x46, 0xE5),
            accent2 = RgbColor(0x0E, 0x74, 0x90),
        )

        val Presets: List<CardPalette> = listOf(
            Default,
            CardPalette(accent = RgbColor(0x0E, 0xA5, 0xE9), accent2 = RgbColor(0x22, 0xD3, 0xEE)),
            CardPalette(accent = RgbColor(0x10, 0xB9, 0x81), accent2 = RgbColor(0x34, 0xD3, 0x99)),
            CardPalette(accent = RgbColor(0xF9, 0x73, 0x16), accent2 = RgbColor(0xFB, 0xBF, 0x24)),
            CardPalette(accent = RgbColor(0xE1, 0x1D, 0x48), accent2 = RgbColor(0xBE, 0x12, 0x3C)),
            CardPalette(accent = RgbColor(0x7C, 0x3A, 0xED), accent2 = RgbColor(0xC0, 0x26, 0xD3)),
            CardPalette(accent = RgbColor(0x33, 0x41, 0x55), accent2 = RgbColor(0x64, 0x74, 0x8B)),
        )

        private const val DERIVED_ACCENT_LIGHTNESS = 0.22

        /**
         * Second stop for a single-colour card. It moves away from the text colour, so the
         * gradient never drags the content towards the contrast limit.
         */
        fun fromAccent(accent: RgbColor): CardPalette {
            val sameColour = CardPalette(accent = accent, accent2 = accent)
            val lighter = CardContrast.contentColor(sameColour) == CardContrast.DarkContent
            val second = if (lighter) {
                accent.lighten(DERIVED_ACCENT_LIGHTNESS)
            } else {
                accent.darken(DERIVED_ACCENT_LIGHTNESS)
            }
            return CardPalette(accent = accent, accent2 = second)
        }
    }
}
