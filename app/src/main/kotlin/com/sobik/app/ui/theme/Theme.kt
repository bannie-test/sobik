package com.sobik.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF1B5E9E),
    secondary = Color(0xFF2E7D32),
    tertiary = Color(0xFFEF6C00),
    secondaryContainer = Color(0xFFDDE8F5),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF90CAF9),
    secondary = Color(0xFFA5D6A7),
    tertiary = Color(0xFFFFB74D),
)

@Composable
fun SobikTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
