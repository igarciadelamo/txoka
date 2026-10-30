package es.igarciadelamo.txoka.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand
val Indigo = Color(0xFF4F46E5)
val IndigoPressed = Color(0xFF4338CA)
val Cyan = Color(0xFF06B6D4)
val IndigoSoft = Color(0xFFEDEBFD)

// Light canvas
val CanvasLight = Color(0xFFF7F5F2)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceMutedLight = Color(0xFFF1EEE9)
val InkLight = Color(0xFF171614)
val InkSecondaryLight = Color(0xFF5F5C56)
val InkTertiaryLight = Color(0xFF8E8A83)
val HairlineLight = Color(0x14171614)

// Dark canvas
val CanvasDark = Color(0xFF121113)
val SurfaceDark = Color(0xFF1C1B1D)
val SurfaceMutedDark = Color(0xFF262427)
val InkDark = Color(0xFFF4F1EC)
val InkSecondaryDark = Color(0xFFA9A49C)
val InkTertiaryDark = Color(0xFF7C7770)
val HairlineDark = Color(0x17FFFFFF)
val IndigoSoftDark = Color(0xFF2A2750)

val Success = Color(0xFF0F9D6A)
val SuccessSoft = Color(0x1F0F9D6A)
val Warning = Color(0xFFB45309)
val WarningSoft = Color(0xFFFDF3E7)
val ErrorRed = Color(0xFFB3261E)
val ErrorSoft = Color(0xFFFBEAE9)

/**
 * Roles that Material 3 does not name but the product needs: hairline dividers, tonal chips and
 * semantic feedback colours.
 */
@Immutable
data class TxokaExtraColors(
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val hairline: Color,
    val accentSoft: Color,
    val canvas: Color,
    val surfaceMuted: Color,
)

val LightExtraColors = TxokaExtraColors(
    success = Success,
    successSoft = SuccessSoft,
    warning = Warning,
    warningSoft = WarningSoft,
    hairline = HairlineLight,
    accentSoft = IndigoSoft,
    canvas = CanvasLight,
    surfaceMuted = SurfaceMutedLight,
)

val DarkExtraColors = TxokaExtraColors(
    success = Color(0xFF34D399),
    successSoft = Color(0x2622C55E),
    warning = Color(0xFFFBBF24),
    warningSoft = Color(0x26F59E0B),
    hairline = HairlineDark,
    accentSoft = IndigoSoftDark,
    canvas = CanvasDark,
    surfaceMuted = SurfaceMutedDark,
)

val LocalTxokaExtraColors = staticCompositionLocalOf { LightExtraColors }
