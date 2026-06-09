package com.skynet.monitoring.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = SkyBlue,
    onPrimary = Color_White,
    primaryContainer = SkyBlueContainer,
    onPrimaryContainer = SkyBlueDark,
    secondary = OnSurfaceVariant,
    background = Surface,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    error = ErrorRed,
    onError = Color_White,
)

private val DarkColorScheme = darkColorScheme(
    primary = SkyBlue80,
    onPrimary = SkyBlueDark,
    primaryContainer = SkyBlueDark,
    onPrimaryContainer = SkyBlueContainer,
    secondary = OnSurfaceVariantDark,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    error = ErrorRed80,
    onError = SkyBlueDark,
)

/**
 * Tema SkyNet. Sengaja TIDAK pakai dynamic color (Material You wallpaper-based)
 * agar warna brand biru konsisten di semua perangkat teknisi.
 */
@Composable
fun MonitoringTeknisiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
