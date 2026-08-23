package com.motionapps.sensorbox.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.dynamicColorScheme

private val SensorBoxWearColors = ColorScheme(
    primary = Color(0xFF9CCBFF),
    primaryDim = Color(0xFF70A9DD),
    primaryContainer = Color(0xFF174A70),
    onPrimary = Color(0xFF003353),
    onPrimaryContainer = Color(0xFFCDE5FF),
    secondary = Color(0xFFB7C9DD),
    secondaryDim = Color(0xFF91A4B7),
    secondaryContainer = Color(0xFF304659),
    onSecondary = Color(0xFF223240),
    onSecondaryContainer = Color(0xFFD3E5FA),
)

@Composable
fun WearSensorBoxTheme(content: @Composable () -> Unit) {
    val colors = dynamicColorScheme(LocalContext.current) ?: SensorBoxWearColors
    MaterialTheme(colorScheme = colors, content = content)
}
