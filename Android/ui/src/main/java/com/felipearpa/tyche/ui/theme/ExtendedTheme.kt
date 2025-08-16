package com.felipearpa.tyche.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class ExtendedColorScheme(
    val gain: Color,
    val drop: Color,
    val steady: Color,
    val placeholder: Color,
    val loadingBackground: Color
)

internal val lightExtendedColorScheme = ExtendedColorScheme(
    gain = lightGainColor,
    drop = lightDropColor,
    steady = lightSteadyColor,
    placeholder = lightPlaceholderColor,
    loadingBackground = lightLoadingBackgroundColor
)

internal val darkExtendedColorScheme = ExtendedColorScheme(
    gain = darkGainColor,
    drop = darkDropColor,
    steady = darkSteadyColor,
    placeholder = darkPlaceholderColor,
    loadingBackground = darkLoadingBackgroundColor
)

val LocalExtendedColorScheme = staticCompositionLocalOf { lightExtendedColorScheme }
