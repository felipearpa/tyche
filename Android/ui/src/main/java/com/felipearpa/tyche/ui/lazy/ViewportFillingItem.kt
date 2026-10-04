package com.felipearpa.tyche.ui.lazy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints

/**
 * The list item of a full-list state, such as an empty or error state: [content] centered in the
 * part of the list's padded viewport left below the items before it, such as a screen's
 * identity and totals. Without items before it, it is centered in the whole padded viewport.
 *
 * That height is a minimum: content taller than it (large text, a short or landscape window)
 * makes the item taller, and the list scrolls to reveal it, so a recovery action stays
 * reachable.
 *
 * Inside [RefreshableLazyPagingColumn] the space above is measured from the list's layout. In
 * any other list the item assumes nothing is above it.
 */
fun LazyListScope.viewportFillingItem(content: @Composable ColumnScope.() -> Unit) {
    item(key = VIEWPORT_FILLING_ITEM_KEY, contentType = VIEWPORT_FILLING_ITEM_KEY) {
        val lazyListState = LocalViewportFillingListState.current
        val heightAbove = remember(lazyListState) {
            derivedStateOf { lazyListState?.layoutInfo?.heightAbove(key = VIEWPORT_FILLING_ITEM_KEY) ?: 0 }
        }

        Layout(
            content = {
                // Measures the padded viewport height, as `fillParentMaxHeight` defines it.
                Box(modifier = Modifier.fillParentMaxHeight())
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = content,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { measurables, constraints ->
            val viewportHeight = measurables[0].measure(Constraints()).height
            // Read during layout, so a change in the content above relayouts only this item.
            val minHeight = (viewportHeight - heightAbove.value).coerceAtLeast(0)
            val placeable = measurables[1].measure(
                constraints.copy(minWidth = constraints.maxWidth, minHeight = 0),
            )
            val height = maxOf(minHeight, placeable.height)
            layout(constraints.maxWidth, height) {
                placeable.place(x = 0, y = (height - placeable.height) / 2)
            }
        }
    }
}

/**
 * The height of the list content before the item with [key], or `null` (treated as none) while
 * that item has not been laid out yet.
 *
 * Measured as the item's offset from the first item, which stays the same while the list
 * scrolls. Once the first item has scrolled out, the content is taller than the viewport and
 * no minimum applies, so the whole viewport height is reported.
 */
private fun LazyListLayoutInfo.heightAbove(key: Any): Int? {
    val item = visibleItemsInfo.firstOrNull { it.key == key } ?: return null
    val first = visibleItemsInfo.first()
    if (first.index != 0) return viewportSize.height
    return item.offset - first.offset
}

/** The state of the list a [viewportFillingItem] is in, provided by [RefreshableLazyPagingColumn]. */
internal val LocalViewportFillingListState = staticCompositionLocalOf<LazyListState?> { null }

private const val VIEWPORT_FILLING_ITEM_KEY = "ViewportFillingItem"
