package com.felipearpa.tyche.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import com.felipearpa.tyche.ui.theme.shimmerColor
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.placeholder.material.shimmer

fun Modifier.shimmer() = composed {
    placeholder(
        visible = true,
        highlight = PlaceholderHighlight.shimmer(),
        color = shimmerColor
    )
}