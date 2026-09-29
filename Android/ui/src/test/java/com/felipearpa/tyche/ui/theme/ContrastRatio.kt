package com.felipearpa.tyche.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** WCAG 2 contrast ratio between two opaque colors. */
internal fun contrastRatio(first: Color, second: Color): Double {
    val firstLuminance = first.luminance().toDouble()
    val secondLuminance = second.luminance().toDouble()
    val brighter = maxOf(firstLuminance, secondLuminance)
    val darker = minOf(firstLuminance, secondLuminance)
    return (brighter + 0.05) / (darker + 0.05)
}
