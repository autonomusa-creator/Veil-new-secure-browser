package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val VeilDarkColorScheme = darkColorScheme(
    primary = VeilDarkShield,
    onPrimary = Color(0xFF042F2B),
    primaryContainer = VeilDarkShieldContainer,
    onPrimaryContainer = VeilDarkOnShieldContainer,
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = VeilDarkSurfaceElevated,
    onSecondaryContainer = VeilDarkTextPrimary,
    tertiary = VeilWarningAmberDark,
    background = VeilDarkBg,
    onBackground = VeilDarkTextPrimary,
    surface = VeilDarkSurface,
    onSurface = VeilDarkTextPrimary,
    surfaceVariant = VeilDarkSurfaceVariant,
    onSurfaceVariant = VeilDarkTextSecondary,
    surfaceContainerLow = VeilDarkSurface,
    surfaceContainer = VeilDarkSurfaceVariant,
    surfaceContainerHigh = VeilDarkSurfaceElevated,
    outline = VeilDarkOutline,
    outlineVariant = Color(0xFF1F2A2E),
    error = VeilDangerRedDark,
    onError = Color(0xFF450A0A)
)

private val VeilLightColorScheme = lightColorScheme(
    primary = VeilLightShield,
    onPrimary = Color.White,
    primaryContainer = VeilLightShieldContainer,
    onPrimaryContainer = VeilLightOnShieldContainer,
    secondary = Color(0xFF475569),
    onSecondary = Color.White,
    secondaryContainer = VeilLightSurfaceElevated,
    onSecondaryContainer = VeilLightTextPrimary,
    tertiary = VeilWarningAmber,
    background = VeilLightBg,
    onBackground = VeilLightTextPrimary,
    surface = VeilLightSurface,
    onSurface = VeilLightTextPrimary,
    surfaceVariant = VeilLightSurfaceVariant,
    onSurfaceVariant = VeilLightTextSecondary,
    surfaceContainerLow = VeilLightSurface,
    surfaceContainer = VeilLightSurfaceVariant,
    surfaceContainerHigh = VeilLightSurfaceElevated,
    outline = VeilLightOutline,
    outlineVariant = Color(0xFFE2E8E4),
    error = VeilDangerRed,
    onError = Color.White
)

val VeilShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    DARK("Dark"),
    LIGHT("Light")
}

@Composable
fun VeilTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val colorScheme = if (useDarkTheme) VeilDarkColorScheme else VeilLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VeilTypography,
        shapes = VeilShapes,
        content = content
    )
}
