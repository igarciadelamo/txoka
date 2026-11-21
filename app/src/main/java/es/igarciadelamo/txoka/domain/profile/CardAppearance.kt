package es.igarciadelamo.txoka.domain.profile

/** Explicit, immutable identifiers: wire values must never depend on enum order. */
@Suppress("MagicNumber")
enum class CardBackground(val wireId: Int) {
    GRADIENT(0),
    HOLOGRAM(1),
    STAINED_GLASS(2),
    NACRE(3),
    BRUSHED_METAL(4),
    FLAT(6),
}

@Suppress("MagicNumber")
enum class CardEffect(val wireId: Int) {
    NONE(0),
    WATER(1),
    EARTH(2),
    WIND(3),
    FIRE(4),
}

@Suppress("MagicNumber")
enum class TextFinish(val wireId: Int) {
    PLAIN(0),
    EMBOSSED(1),
    RAISED(2),
    FRAME(3),
    METAL(4),
    BUBBLE(5),
    ;

    val defaultColor: RgbColor
        get() = if (this == METAL) MetalColor else WhiteColor

    companion object {
        private val MetalColor = RgbColor(0x9B, 0xA5, 0xB5)
        private val WhiteColor = RgbColor(0xFF, 0xFF, 0xFF)
    }
}

@Suppress("MagicNumber")
enum class TextSize(val wireId: Int) {
    S(0),
    M(1),
    L(2),
    XL(3),
}

@Suppress("MagicNumber")
enum class CardMosaic(val wireId: Int, val assetId: String? = null) {
    NONE(0),
    TRIANGLES(1),
    SQUARES(2),
    CAIRO(3),
    STARS(4),
    HEXAGONS(5),
    DOTS(6),
    NATURE_FISH(160, "nature-fish"),
    NATURE_BIRDS(161, "nature-birds"),
    NATURE_REPTILES(162, "nature-reptiles"),
    GEO_TRUCHET_LABYRINTH(192, "geo-truchet-labyrinth"),
    GEO_TRUCHET_DOUBLE_TRACKS(193, "geo-truchet-double-tracks"),
    GEO_ORTHOGONAL_LABYRINTH(194, "geo-orthogonal-labyrinth"),
    GEO_BAUHAUS_QUARTER_ROUNDS(195, "geo-bauhaus-quarter-rounds"),
    GEO_SINE_WAVE_MOIRE(196, "geo-sine-wave-moire"),
    GEO_SAWTOOTH_WAVES(197, "geo-sawtooth-waves"),
    GEO_HERRINGBONE_CHEVRONS(198, "geo-herringbone-chevrons"),
    GEO_DIAMOND_VIBRATIONS(199, "geo-diamond-vibrations"),
    GEO_CONCENTRIC_ORBITALS(200, "geo-concentric-orbitals"),
    GEO_VESICA_PISCIS_LATTICE(201, "geo-vesica-piscis-lattice"),
    GEO_HONEYCOMB_CARTOGRAPHY(202, "geo-honeycomb-cartography"),
    GEO_ISOMETRIC_CUBE_FIELDS(203, "geo-isometric-cube-fields"),
    GEO_HEXAGON_PINWHEELS(204, "geo-hexagon-pinwheels"),
    GEO_EQUILATERAL_FRACTALS(205, "geo-equilateral-fractals"),
    GEO_TRIANGULAR_TURBINES(206, "geo-triangular-turbines"),
    GEO_RHOMBUS_MIRRORS(207, "geo-rhombus-mirrors"),
    GEO_EIGHTFOLD_STARWORK(208, "geo-eightfold-starwork"),
    GEO_TWELVEFOLD_ZELLIGE(209, "geo-twelvefold-zellige"),
    GEO_INTERLOCKING_GREEK_CROSSES(210, "geo-interlocking-greek-crosses"),
    GEO_OPTICAL_WOVEN_GRID(211, "geo-optical-woven-grid"),
}

data class CardTextStyle(
    val finish: TextFinish = TextFinish.PLAIN,
    val size: TextSize = TextSize.M,
    val color: RgbColor? = null,
) {
    val effectiveColor: RgbColor get() = color ?: finish.defaultColor

    fun canonical(): CardTextStyle = if (color == finish.defaultColor) copy(color = null) else this
}

/** Motion, animation parameters and catalogue metadata stay local to the renderer. */
data class CardAppearance(
    val background: CardBackground = CardBackground.GRADIENT,
    val mosaic: CardMosaic = CardMosaic.NONE,
    val effect: CardEffect = CardEffect.NONE,
    val name: CardTextStyle = CardTextStyle(),
    val data: CardTextStyle = CardTextStyle(finish = TextFinish.BUBBLE),
    val topRight: RgbColor? = null,
    val bottomLeft: RgbColor? = null,
) {
    val hasFourCorners: Boolean get() = topRight != null && bottomLeft != null

    fun canonical(): CardAppearance = copy(name = name.canonical(), data = data.canonical())
}
