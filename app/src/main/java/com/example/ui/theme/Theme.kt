package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AstroDarkColorScheme = darkColorScheme(
    primary = AstroCyan,
    onPrimary = CosmicAbyss,
    primaryContainer = NebulaElevated,
    onPrimaryContainer = StarlightWhite,
    secondary = AstroMagenta,
    onSecondary = StarlightWhite,
    secondaryContainer = NebulaSurface,
    onSecondaryContainer = StarlightWhite,
    tertiary = AstroGold,
    onTertiary = CosmicAbyss,
    background = CosmicAbyss,
    onBackground = StarlightWhite,
    surface = CosmicMidnight,
    onSurface = StarlightWhite,
    surfaceVariant = NebulaSurface,
    onSurfaceVariant = StarlightMuted,
    outline = StardustOutline,
    error = DangerCrimson,
    onError = StarlightWhite
)

private val AstroShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

@Composable
fun AstroPopTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AstroDarkColorScheme,
        typography = AstroTypography,
        shapes = AstroShapes,
        content = content
    )
}
