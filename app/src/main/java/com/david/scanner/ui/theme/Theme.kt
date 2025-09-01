package com.david.scanner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

@Composable
fun ScannerAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ScannerLightColors,
        typography = ScannerTypography,
        shapes = ScannerShapes,
        content = content
    )
}
