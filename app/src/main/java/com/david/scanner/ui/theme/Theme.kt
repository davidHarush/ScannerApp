package com.david.scanner.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Custom color palette for scanner app
private val ScannerLightColors = lightColorScheme(
    primary = Color(0xFF2196F3), // Modern blue
    primaryContainer = Color(0xFFE3F2FD),
    secondary = Color(0xFF4CAF50), // Success green
    secondaryContainer = Color(0xFFE8F5E8),
    tertiary = Color(0xFFFF9800), // Warning orange
    surface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFFF5F5F5),
    background = Color(0xFFFFFFFF),
    error = Color(0xFFE53935),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onSurface = Color(0xFF1C1B1F),
    onBackground = Color(0xFF1C1B1F)
)

private val ScannerDarkColors = darkColorScheme(
    primary = Color(0xFF64B5F6),
    primaryContainer = Color(0xFF1565C0),
    secondary = Color(0xFF81C784),
    secondaryContainer = Color(0xFF388E3C),
    tertiary = Color(0xFFFFB74D),
    surface = Color(0xFF121212),
    surfaceVariant = Color(0xFF1E1E1E),
    background = Color(0xFF000000),
    error = Color(0xFFEF5350),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onSurface = Color(0xFFE1E3E3),
    onBackground = Color(0xFFE1E3E3)
)

@Composable
fun ScannerAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ScannerDarkColors
        else -> ScannerLightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = ScannerLightColors,
        typography = ScannerTypography,
        shapes = ScannerShapes,
        content = content
    )
}
