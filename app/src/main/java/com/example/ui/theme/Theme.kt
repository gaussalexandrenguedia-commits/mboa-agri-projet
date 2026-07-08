package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrightPlantationGreen,
    onPrimary = Color(0xFF0D2517),
    secondary = WarmClayTerracotta,
    onSecondary = Color.White,
    tertiary = SoftMaizeYellow,
    onTertiary = Color(0xFF3B2800),
    background = EcoDarkBg,
    onBackground = EcoDarkText,
    surface = EcoDarkSurface,
    onSurface = EcoDarkText,
    surfaceVariant = EcoDarkCard,
    onSurfaceVariant = EcoDarkText
)

private val LightColorScheme = lightColorScheme(
    primary = PlantationGreen,
    onPrimary = Color.White,
    secondary = ClayTerracotta,
    onSecondary = Color.White,
    tertiary = MaizeYellow,
    onTertiary = Color.White,
    background = EcoLightBg,
    onBackground = Color(0xFF132219),
    surface = EcoLightSurface,
    onSurface = Color(0xFF132219),
    surfaceVariant = EcoLightCard,
    onSurfaceVariant = Color(0xFF1E3A2B)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
