package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = TerracottaPrimaryDark,
    onPrimary = OnTerracottaDark,
    primaryContainer = TerracottaContainerDark,
    onPrimaryContainer = OnTerracottaContainerDark,
    secondary = AmberSecondaryDark,
    onSecondary = OnAmberDark,
    secondaryContainer = AmberContainerDark,
    onSecondaryContainer = OnAmberContainerDark,
    tertiary = TealTertiaryDark,
    onTertiary = OnTealDark,
    tertiaryContainer = TealContainerDark,
    onTertiaryContainer = OnTealContainerDark,
    background = LoungeDark,
    onBackground = IvoryInkDark,
    surface = SurfaceWarmDark,
    onSurface = IvoryInkDark,
    surfaceVariant = SurfaceContainerDark,
    onSurfaceVariant = MutedIvoryDark,
    surfaceContainerLow = SurfaceWarmDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    outline = OutlineWarmDark,
    outlineVariant = OutlineVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = TerracottaPrimaryLight,
    onPrimary = OnTerracottaLight,
    primaryContainer = TerracottaContainerLight,
    onPrimaryContainer = OnTerracottaContainerLight,
    secondary = AmberSecondaryLight,
    onSecondary = OnAmberLight,
    secondaryContainer = AmberContainerLight,
    onSecondaryContainer = OnAmberContainerLight,
    tertiary = TealTertiaryLight,
    onTertiary = OnTealLight,
    tertiaryContainer = TealContainerLight,
    onTertiaryContainer = OnTealContainerLight,
    background = PaperCreamLight,
    onBackground = EspressoInkLight,
    surface = SurfaceWarmLight,
    onSurface = EspressoInkLight,
    surfaceVariant = SurfaceContainerLight,
    onSurfaceVariant = MutedInkLight,
    surfaceContainerLow = SurfaceWarmLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    outline = OutlineWarmLight,
    outlineVariant = OutlineVariantLight
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
