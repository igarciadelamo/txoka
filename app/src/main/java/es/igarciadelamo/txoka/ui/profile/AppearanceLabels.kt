package es.igarciadelamo.txoka.ui.profile

import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.profile.CardBackground
import es.igarciadelamo.txoka.domain.profile.CardEffect
import es.igarciadelamo.txoka.domain.profile.CardMosaic
import es.igarciadelamo.txoka.domain.profile.TextFinish
import es.igarciadelamo.txoka.domain.profile.TextSize

internal val CardBackground.label: Int
    get() = when (this) {
        CardBackground.GRADIENT -> R.string.appearance_gradient
        CardBackground.HOLOGRAM -> R.string.appearance_hologram
        CardBackground.STAINED_GLASS -> R.string.appearance_bg_stained_glass
        CardBackground.NACRE -> R.string.appearance_bg_nacre
        CardBackground.BRUSHED_METAL -> R.string.appearance_bg_brushed_metal
        CardBackground.FLAT -> R.string.appearance_flat
    }

internal val CardEffect.label: Int
    get() = when (this) {
        CardEffect.NONE -> R.string.appearance_no_effect
        CardEffect.WATER -> R.string.appearance_water
        CardEffect.EARTH -> R.string.appearance_earth
        CardEffect.WIND -> R.string.appearance_wind
        CardEffect.FIRE -> R.string.appearance_fire
    }

internal val TextFinish.label: Int
    get() = when (this) {
        TextFinish.PLAIN -> R.string.appearance_plain
        TextFinish.EMBOSSED -> R.string.appearance_embossed
        TextFinish.RAISED -> R.string.appearance_raised
        TextFinish.FRAME -> R.string.appearance_frame
        TextFinish.METAL -> R.string.appearance_metal
        TextFinish.BUBBLE -> R.string.appearance_bubble
    }

internal val TextSize.label: Int
    get() = when (this) {
        TextSize.S -> R.string.appearance_size_small
        TextSize.M -> R.string.appearance_size_medium
        TextSize.L -> R.string.appearance_size_large
        TextSize.XL -> R.string.appearance_size_extra_large
    }

private val MosaicLabels = mapOf(
    CardMosaic.NONE to R.string.appearance_none,
    CardMosaic.TRIANGLES to R.string.appearance_triangles,
    CardMosaic.SQUARES to R.string.appearance_squares,
    CardMosaic.CAIRO to R.string.appearance_cairo,
    CardMosaic.STARS to R.string.appearance_stars,
    CardMosaic.HEXAGONS to R.string.appearance_hexagons,
    CardMosaic.DOTS to R.string.appearance_dots,
    CardMosaic.NATURE_FISH to R.string.appearance_fish,
    CardMosaic.NATURE_BIRDS to R.string.appearance_birds,
    CardMosaic.NATURE_REPTILES to R.string.appearance_reptiles,
    CardMosaic.GEO_TRUCHET_LABYRINTH to R.string.appearance_truchet_labyrinth,
    CardMosaic.GEO_TRUCHET_DOUBLE_TRACKS to R.string.appearance_truchet_double_tracks,
    CardMosaic.GEO_ORTHOGONAL_LABYRINTH to R.string.appearance_orthogonal_labyrinth,
    CardMosaic.GEO_BAUHAUS_QUARTER_ROUNDS to R.string.appearance_bauhaus_quarter_rounds,
    CardMosaic.GEO_SINE_WAVE_MOIRE to R.string.appearance_sine_wave_moire,
    CardMosaic.GEO_SAWTOOTH_WAVES to R.string.appearance_sawtooth_waves,
    CardMosaic.GEO_HERRINGBONE_CHEVRONS to R.string.appearance_herringbone_chevrons,
    CardMosaic.GEO_DIAMOND_VIBRATIONS to R.string.appearance_diamond_vibrations,
    CardMosaic.GEO_CONCENTRIC_ORBITALS to R.string.appearance_concentric_orbitals,
    CardMosaic.GEO_VESICA_PISCIS_LATTICE to R.string.appearance_vesica_piscis_lattice,
    CardMosaic.GEO_HONEYCOMB_CARTOGRAPHY to R.string.appearance_honeycomb_cartography,
    CardMosaic.GEO_ISOMETRIC_CUBE_FIELDS to R.string.appearance_isometric_cube_fields,
    CardMosaic.GEO_HEXAGON_PINWHEELS to R.string.appearance_hexagon_pinwheels,
    CardMosaic.GEO_EQUILATERAL_FRACTALS to R.string.appearance_equilateral_fractals,
    CardMosaic.GEO_TRIANGULAR_TURBINES to R.string.appearance_triangular_turbines,
    CardMosaic.GEO_RHOMBUS_MIRRORS to R.string.appearance_rhombus_mirrors,
    CardMosaic.GEO_EIGHTFOLD_STARWORK to R.string.appearance_eightfold_starwork,
    CardMosaic.GEO_TWELVEFOLD_ZELLIGE to R.string.appearance_twelvefold_zellige,
    CardMosaic.GEO_INTERLOCKING_GREEK_CROSSES to R.string.appearance_interlocking_greek_crosses,
    CardMosaic.GEO_OPTICAL_WOVEN_GRID to R.string.appearance_optical_woven_grid,
)

internal val CardMosaic.label: Int get() = MosaicLabels.getValue(this)

internal enum class MosaicCategory(val label: Int) {
    ALL(R.string.appearance_all),
    GEOMETRIC(R.string.appearance_geometric),
    LABYRINTHS(R.string.appearance_labyrinths),
    OPTICAL(R.string.appearance_optical),
    WOVEN(R.string.appearance_woven),
    ROSETTES(R.string.appearance_rosettes),
    BIRDS(R.string.appearance_birds),
    FISH(R.string.appearance_fish),
    REPTILES(R.string.appearance_reptiles),
    ;

    fun includes(mosaic: CardMosaic): Boolean = this == ALL || mosaic.wireId in CategoryIds.getValue(this)
}

private val CategoryIds = mapOf(
    MosaicCategory.GEOMETRIC to ((1..6) + (192..211)).toSet(),
    MosaicCategory.LABYRINTHS to (192..194).toSet(),
    MosaicCategory.OPTICAL to setOf(195, 196, 197, 199, 200, 201, 207, 211),
    MosaicCategory.WOVEN to setOf(198, 210, 211),
    MosaicCategory.ROSETTES to setOf(204, 206, 208, 209),
    MosaicCategory.BIRDS to setOf(161),
    MosaicCategory.FISH to setOf(160),
    MosaicCategory.REPTILES to setOf(162),
)
