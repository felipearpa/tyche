package com.felipearpa.tyche.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.felipearpa.tyche.ui.theme.LocalExtendedColorScheme
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.placeholder.material.shimmer

@Composable
fun Modifier.shimmer() =
    placeholder(
        visible = true,
        highlight = PlaceholderHighlight.shimmer(),
        color = LocalExtendedColorScheme.current.placeholder,
    )
