package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ROGColorScheme = darkColorScheme(
    primary              = Color(0xFFCC0000),
    onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = Color(0xFF1A0000),
    onPrimaryContainer   = Color(0xFFFF4444),
    secondary            = Color(0xFF999999),
    onSecondary          = Color(0xFFFFFFFF),
    background           = Color(0xFF0A0A0A),
    onBackground         = Color(0xFFFFFFFF),
    surface              = Color(0xFF141414),
    onSurface            = Color(0xFFFFFFFF),
    surfaceVariant       = Color(0xFF1C1C1C),
    onSurfaceVariant     = Color(0xFF999999),
    outline              = Color(0xFF333333),
    outlineVariant       = Color(0xFF2A2A2A),
    error                = Color(0xFFFF4444),
    onError              = Color(0xFFFFFFFF),
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