package com.tomasrepcik.sensorbox.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.tomasrepcik.sensorbox.core.preferences.AppThemeMode

val SensorBoxRecording = Color(0xFFFF6B72)

private val DarkColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color.Black,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFD0D0D0),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF292929),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFFBDBDBD),
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF101010),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1C1C1C),
    onSurfaceVariant = Color(0xFFC7C7C7),
    outline = Color(0xFF8A8A8A),
    outlineVariant = Color(0xFF363636),
    error = SensorBoxRecording,
    errorContainer = Color(0xFF4D2025),
    onErrorContainer = Color(0xFFFFDADC),
)

private val LightColors = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color.Black,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF303030),
    secondaryContainer = Color(0xFFE8E8E8),
    onSecondaryContainer = Color.Black,
    tertiary = Color(0xFF4A4A4A),
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF1F1F1),
    onSurfaceVariant = Color(0xFF555555),
    outline = Color(0xFF707070),
    outlineVariant = Color(0xFFD8D8D8),
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
fun SensorBoxTheme(
    themeMode: AppThemeMode = AppThemeMode.AUTOMATIC,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        AppThemeMode.AUTOMATIC -> systemDarkTheme
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors

        else -> LightColors
    }
    val view = LocalView.current
    val useDarkSystemBarIcons = colors.background.luminance() > 0.5f
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.decorView.setBackgroundColor(colors.background.toArgb())
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = useDarkSystemBarIcons
                isAppearanceLightNavigationBars = useDarkSystemBarIcons
            }
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = SensorBoxTypography,
        shapes = SensorBoxShapes,
        content = content,
    )
}
