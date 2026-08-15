package com.motionapps.sensorbox.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val SensorBoxRecording = Color(0xFFFF6B72)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9AAEFF),
    onPrimary = Color(0xFF11172A),
    primaryContainer = Color(0xFF222C4A),
    onPrimaryContainer = Color(0xFFDDE4FF),
    secondary = Color(0xFFBAC4D8),
    onSecondary = Color(0xFF202A38),
    secondaryContainer = Color(0xFF28313D),
    onSecondaryContainer = Color(0xFFDEE6F2),
    tertiary = Color(0xFFAEB9CA),
    background = Color(0xFF0B0D10),
    onBackground = Color(0xFFF1F3F6),
    surface = Color(0xFF111419),
    onSurface = Color(0xFFF1F3F6),
    surfaceVariant = Color(0xFF181C22),
    onSurfaceVariant = Color(0xFFAAB2BF),
    outline = Color(0xFF4A5360),
    outlineVariant = Color(0xFF292F38),
    error = SensorBoxRecording,
    errorContainer = Color(0xFF4D2025),
    onErrorContainer = Color(0xFFFFDADC),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF445DA8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE4FF),
    onPrimaryContainer = Color(0xFF17234A),
    secondary = Color(0xFF566174),
    secondaryContainer = Color(0xFFDCE4F2),
    onSecondaryContainer = Color(0xFF182331),
    tertiary = Color(0xFF596475),
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF1A1D22),
    surface = Color.White,
    onSurface = Color(0xFF1A1D22),
    surfaceVariant = Color(0xFFEEF1F5),
    onSurfaceVariant = Color(0xFF59616D),
    outline = Color(0xFF747D8A),
    outlineVariant = Color(0xFFD9DEE6),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val SensorBoxTypography = Typography(
    headlineLarge = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 25.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
)

private val SensorBoxShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
)

@Composable
fun SensorBoxTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = SensorBoxTypography,
        shapes = SensorBoxShapes,
        content = content,
    )
}
