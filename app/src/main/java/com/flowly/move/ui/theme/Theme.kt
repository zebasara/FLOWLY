package com.flowly.move.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FlowlyColorScheme = darkColorScheme(
    primary            = FlowlyAccent,
    onPrimary          = FlowlyBg,
    primaryContainer   = FlowlyCard,
    onPrimaryContainer = FlowlyText,
    secondary          = FlowlyAccent2,
    onSecondary        = FlowlyBg,
    secondaryContainer = FlowlyCard2,
    tertiary           = FlowlyAccent3,
    background         = FlowlyBg,
    onBackground       = FlowlyText,
    surface            = FlowlyCard,
    onSurface          = FlowlyText,
    surfaceVariant     = FlowlyCard2,
    onSurfaceVariant   = FlowlyMuted,
    outline            = FlowlyBorder,
    error              = FlowlyDanger,
    onError            = Color.White,
)

@Composable
fun FlowlyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FlowlyColorScheme,
        typography  = FlowlyTypography,
        content     = content
    )
}
