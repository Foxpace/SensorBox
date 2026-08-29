package com.tomasrepcik.sensorbox.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.dynamicColorScheme
import com.tomasrepcik.sensorbox.core.preferences.AppThemeMode

private val DarkWearColors = ColorScheme(
    primary = Color.White,
    primaryDim = Color(0xFFD0D0D0),
    primaryContainer = Color(0xFF242424),
    onPrimary = Color.Black,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFD0D0D0),
    secondaryDim = Color(0xFFAAAAAA),
    secondaryContainer = Color(0xFF242424),
    onSecondary = Color.Black,
    onSecondaryContainer = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surfaceContainer = Color(0xFF171717),
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFC7C7C7),
    outline = Color(0xFF8A8A8A),
    outlineVariant = Color(0xFF363636),
)

private val LightWearColors = ColorScheme(
    primary = Color.Black,
    primaryDim = Color(0xFF303030),
    primaryContainer = Color(0xFFE8E8E8),
    onPrimary = Color.White,
    onPrimaryContainer = Color.Black,
    secondary = Color(0xFF303030),
    secondaryDim = Color(0xFF555555),
    secondaryContainer = Color(0xFFE8E8E8),
    onSecondary = Color.White,
    onSecondaryContainer = Color.Black,
    background = Color.White,
    onBackground = Color.Black,
    surfaceContainer = Color(0xFFF1F1F1),
    onSurface = Color.Black,
    onSurfaceVariant = Color(0xFF555555),
    outline = Color(0xFF707070),
    outlineVariant = Color(0xFFD8D8D8),
)

@Composable
fun WearSensorBoxTheme(
    themeMode: AppThemeMode = AppThemeMode.AUTOMATIC,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.AUTOMATIC -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val fallback = if (darkTheme) DarkWearColors else LightWearColors
    val colors = if (dynamicColor && darkTheme) dynamicColorScheme(LocalContext.current) ?: fallback else fallback
    MaterialTheme(colorScheme = colors, content = content)
}
