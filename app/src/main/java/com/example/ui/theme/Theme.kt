package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BananaGold,
    onPrimary = Color(0xFF221A00),
    primaryContainer = Color(0xFF3E3200),
    onPrimaryContainer = BananaGold,
    secondary = ElectricCyan,
    onSecondary = Color(0xFF003644),
    secondaryContainer = Color(0xFF004D60),
    onSecondaryContainer = Color(0xFFBBE9FF),
    tertiary = StudioPurple,
    onTertiary = Color.White,
    background = BrandMidnight,
    surface = BrandSurfaceDark,
    surfaceVariant = BrandSurfaceElevated,
    onBackground = BrandTextPrimaryDark,
    onSurface = BrandTextPrimaryDark,
    onSurfaceVariant = BrandTextSecondaryDark,
    outline = BrandBorder
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF7A5900),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDF9E),
    onPrimaryContainer = Color(0xFF261900),
    secondary = Color(0xFF00677D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBBE9FF),
    onSecondaryContainer = Color(0xFF001F27),
    tertiary = StudioPurple,
    onTertiary = Color.White,
    background = BrandSurfaceLight,
    surface = BrandCardLight,
    surfaceVariant = Color(0xFFEBEFF4),
    onBackground = BrandTextPrimaryLight,
    onSurface = BrandTextPrimaryLight,
    onSurfaceVariant = BrandTextSecondaryLight,
    outline = Color(0xFFD0D7DE)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep branded palette for creative studio feel
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
