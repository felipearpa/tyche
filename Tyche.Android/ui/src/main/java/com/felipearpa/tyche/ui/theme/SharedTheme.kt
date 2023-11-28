package com.felipearpa.tyche.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

val ColorScheme.positive
    @Composable
    @ReadOnlyComposable
    get() = if (!isSystemInDarkTheme()) lightPositiveColor else darkPositiveColor

val ColorScheme.negative
    @Composable
    @ReadOnlyComposable
    get() = if (!isSystemInDarkTheme()) lightNegativeColor else darkNegativeColor

val ColorScheme.neutral
    @Composable
    @ReadOnlyComposable
    get() = if (!isSystemInDarkTheme()) lightNeutralColor else darkNeutralColor

val ColorScheme.shimmer
    @Composable
    @ReadOnlyComposable
    get() = if (!isSystemInDarkTheme()) lightShimmerColor else darkShimmerColor