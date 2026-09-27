package com.felipearpa.tyche.ui.lazy

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.felipearpa.tyche.ui.CenteredScrollableColumn

/**
 * A list item that fills the list's padded viewport and centers [content] in it, as the
 * single item of an empty or error state. Content taller than the viewport scrolls inside
 * the item rather than being clipped; see [CenteredScrollableColumn].
 */
@Composable
fun LazyItemScope.ViewportFillingItem(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    CenteredScrollableColumn(
        modifier = Modifier.fillParentMaxSize(),
        contentModifier = modifier,
        content = content,
    )
}
