package com.pipel.tyche.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

private val DarkColorPalette = darkColors(
    primary = Purple200,
    primaryVariant = Purple700,
    secondary = Teal200
)

private val LightColorPalette = lightColors(
    primary = nord10,
    primaryVariant = nord10Variant,
    secondary = nord9,
    secondaryVariant = nord9Variant,
    background = nord6,
    surface = nord5,
    error = nord11,
    onPrimary = nord0,
    onSecondary = nord0,
    onBackground = nord0,
    onSurface = nord0,
    onError = nord6
)

@Composable
fun TycheTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}