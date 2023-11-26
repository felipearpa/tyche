package com.felipearpa.tyche.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val positiveColor: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF8BC34A) else Color(0xFF689F38)

val neutralColor: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFFFFEB3B) else Color(0xFFFBC02D)

val negativeColor: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFFEF9A9A) else Color(0xFFEF5350)

val shimmerColor: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF37474F) else Color(0xFFE8F5E9)