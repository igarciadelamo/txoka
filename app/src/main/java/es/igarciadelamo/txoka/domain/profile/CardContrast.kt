package es.igarciadelamo.txoka.domain.profile

/**
 * Chooses the text colour of a card. Both gradient stops count, so the content stays readable
 * across the whole card and not only on the first colour.
 */
object CardContrast {
    /** WCAG 2.1 AA for body text. Large text only needs 3:1, so this covers both. */
    const val BODY_MIN_RATIO = 4.5

    val LightContent = RgbColor(0xFF, 0xFF, 0xFF)
    val DarkContent = RgbColor(0x1C, 0x1B, 0x1F)

    fun contentColor(palette: CardPalette): RgbColor {
        val light = worstRatio(palette, LightContent)
        val dark = worstRatio(palette, DarkContent)
        return if (light >= dark) LightContent else DarkContent
    }

    /** Contrast of [content] against the least readable stop of [palette]. */
    fun worstRatio(palette: CardPalette, content: RgbColor): Double =
        minOf(palette.accent.contrastRatio(content), palette.accent2.contrastRatio(content))
}
