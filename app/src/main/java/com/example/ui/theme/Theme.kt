package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightPinkColorScheme = lightColorScheme(
    primary = RosePrimary,
    onPrimary = OnRosePrimary,
    primaryContainer = RosePrimaryContainer,
    onPrimaryContainer = OnRosePrimaryContainer,
    secondary = BerrySecondary,
    onSecondary = OnBerrySecondary,
    secondaryContainer = BerrySecondaryContainer,
    onSecondaryContainer = OnBerrySecondaryContainer,
    tertiary = CoralTertiary,
    onTertiary = OnCoralTertiary,
    tertiaryContainer = CoralTertiaryContainer,
    onTertiaryContainer = OnCoralTertiaryContainer,
    background = BlushBackground,
    onBackground = OnBlushBackground,
    surface = BlushSurface,
    onSurface = OnBlushSurface,
    surfaceVariant = BlushSurfaceVariant,
    onSurfaceVariant = OnBlushSurfaceVariant,
    outline = BlushOutline,
    outlineVariant = BlushOutlineVariant,
    surfaceContainerLow = BlushSurfaceContainerLow,
    surfaceContainer = BlushSurfaceContainer,
    surfaceContainerHigh = BlushSurfaceContainerHigh
)

private val DarkPinkColorScheme = darkColorScheme(
    primary = DarkRosePrimary,
    onPrimary = DarkOnRosePrimary,
    primaryContainer = DarkRosePrimaryContainer,
    onPrimaryContainer = DarkOnRosePrimaryContainer,
    secondary = DarkBerrySecondary,
    onSecondary = DarkOnBerrySecondary,
    secondaryContainer = DarkBerrySecondaryContainer,
    onSecondaryContainer = DarkOnBerrySecondaryContainer,
    tertiary = DarkCoralTertiary,
    onTertiary = DarkOnCoralTertiary,
    tertiaryContainer = DarkCoralTertiaryContainer,
    onTertiaryContainer = DarkOnCoralTertiaryContainer,
    background = DarkBlushBackground,
    onBackground = DarkOnBlushBackground,
    surface = DarkBlushSurface,
    onSurface = DarkOnBlushSurface,
    surfaceVariant = DarkBlushSurfaceVariant,
    onSurfaceVariant = DarkOnBlushSurfaceVariant,
    outline = DarkBlushOutline,
    surfaceContainer = DarkBlushSurfaceContainer
)

private val EduFlowShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

@Composable
fun EduFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkPinkColorScheme else LightPinkColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = EduFlowTypography,
        shapes = EduFlowShapes,
        content = content
    )
}
