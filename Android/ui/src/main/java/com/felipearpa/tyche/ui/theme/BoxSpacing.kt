package com.felipearpa.tyche.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class BoxSpacing(
    val small: Dp = 4.dp,
    val medium: Dp = 8.dp,
    val large: Dp = 16.dp,
    val extraLarge: Dp = 24.dp,
    val xxLarge: Dp = 32.dp,
    val xxxLarge: Dp = 40.dp,
    val huge: Dp = 48.dp,
    val massive: Dp = 64.dp
)

val LocalBoxSpacing = staticCompositionLocalOf { BoxSpacing() }
