package es.igarciadelamo.txoka.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColorScheme: ColorScheme = lightColorScheme(
    primary = Indigo,
    onPrimary = SurfaceLight,
    primaryContainer = IndigoSoft,
    onPrimaryContainer = Indigo,
    secondary = Cyan,
    onSecondary = SurfaceLight,
    secondaryContainer = Color(0xFFE0F7FB),
    onSecondaryContainer = Color(0xFF0E5A66),
    tertiary = InkSecondaryLight,
    onTertiary = SurfaceLight,
    background = CanvasLight,
    onBackground = InkLight,
    surface = SurfaceLight,
    onSurface = InkLight,
    surfaceVariant = SurfaceMutedLight,
    onSurfaceVariant = InkSecondaryLight,
    surfaceTint = Indigo,
    surfaceContainerLowest = SurfaceLight,
    surfaceContainerLow = Color(0xFFFBF9F6),
    surfaceContainer = SurfaceMutedLight,
    surfaceContainerHigh = Color(0xFFECE8E1),
    surfaceContainerHighest = Color(0xFFE6E1D9),
    outline = InkTertiaryLight,
    outlineVariant = HairlineLight,
    error = ErrorRed,
    onError = SurfaceLight,
    errorContainer = ErrorSoft,
    onErrorContainer = ErrorRed,
)

private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF8B85F5),
    onPrimary = Color(0xFF14122B),
    primaryContainer = IndigoSoftDark,
    onPrimaryContainer = Color(0xFFD9D5FF),
    secondary = Color(0xFF67E8F9),
    onSecondary = Color(0xFF062A31),
    secondaryContainer = Color(0xFF14343A),
    onSecondaryContainer = Color(0xFFB8F1F8),
    tertiary = InkSecondaryDark,
    onTertiary = Color(0xFF141214),
    background = CanvasDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = SurfaceMutedDark,
    onSurfaceVariant = InkSecondaryDark,
    surfaceTint = Color(0xFF8B85F5),
    surfaceContainerLowest = Color(0xFF0C0B0D),
    surfaceContainerLow = Color(0xFF171618),
    surfaceContainer = SurfaceMutedDark,
    surfaceContainerHigh = Color(0xFF2E2C30),
    surfaceContainerHighest = Color(0xFF39363B),
    outline = InkTertiaryDark,
    outlineVariant = HairlineDark,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF4A1D1B),
    onErrorContainer = Color(0xFFF9DEDC),
)

@Composable
fun TxokaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extras = if (darkTheme) DarkExtraColors else LightExtraColors

    CompositionLocalProvider(LocalTxokaExtraColors provides extras) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TxokaTypography,
            shapes = TxokaShapes,
            content = content,
        )
    }
}

/** Shortcut for the non Material colours. */
val MaterialTheme.txoka: TxokaExtraColors
    @Composable get() = LocalTxokaExtraColors.current
