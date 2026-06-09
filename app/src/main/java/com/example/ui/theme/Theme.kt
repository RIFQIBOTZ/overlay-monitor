package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ROGColorScheme = darkColorScheme(
    primary              = Color(0xFFFF6B00),
    onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = Color(0xFF2A1500),
    onPrimaryContainer   = Color(0xFFFF8C35),
    secondary            = Color(0xFFAAAAAA),
    onSecondary          = Color(0xFFFFFFFF),
    background           = Color(0xFF0D0D0D),
    onBackground         = Color(0xFFFFFFFF),
    surface              = Color(0xFF1A1A1A),
    onSurface            = Color(0xFFFFFFFF),
    surfaceVariant       = Color(0xFF222222),
    onSurfaceVariant     = Color(0xFFAAAAAA),
    outline              = Color(0xFF444444),
    outlineVariant       = Color(0xFF333333),
    error                = Color(0xFFFF4444),
    onError              = Color(0xFFFFFFFF),
    errorContainer       = Color(0xFF2A0000),
    onErrorContainer     = Color(0xFFFF8888)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ROGColorScheme,
        typography  = Typography,
        content     = content
    )
}