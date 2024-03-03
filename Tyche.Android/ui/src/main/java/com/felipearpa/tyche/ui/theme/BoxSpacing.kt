package com.felipearpa.tyche.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class BoxSpacing(
    val small: Dp = 4.dp,
    val medium: Dp = 8.dp,
    val large: Dp = 16.dp
)

private val LocalBoxSpacing = compositionLocalOf { BoxSpacing() }

val MaterialTheme.boxSpacing
    @Composable
    @ReadOnlyComposable
    get() = LocalBoxSpacing.current