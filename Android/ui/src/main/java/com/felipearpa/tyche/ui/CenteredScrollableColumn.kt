package com.felipearpa.tyche.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Centers [content] in the space [modifier] gives it, for a screen-filling message such as
 * an empty or error state.
 *
 * When the content is taller than that space (large text, a short or landscape window), it
 * scrolls instead of being clipped, so a recovery action stays reachable. [contentModifier]
 * applies inside the scrolling area, so inset and design padding set there scroll with the
 * content while the scrolling area itself can reach the window edges.
 */
@Composable
fun CenteredScrollableColumn(
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .then(contentModifier),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}
