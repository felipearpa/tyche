package com.felipearpa.tyche.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import com.felipearpa.tyche.ui.theme.shimmer
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.placeholder.material.shimmer

fun Modifier.shimmer() = composed {
    placeholder(
        visible = true,
        highlight = PlaceholderHighlight.shimmer(),
        color = MaterialTheme.colorScheme.shimmer
    )
}