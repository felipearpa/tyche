package com.felipearpa.tyche.bet.pending

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * Keeps the cards above the last one still when the last item shrinks while the list is scrolled
 * to its very end, as iOS does with its bottom scroll anchor. Without it the list, now shorter
 * than its scroll offset, clamps and pulls every card above down by the lost height.
 *
 * [trackLastItem] goes on the list's last item. While that item shrinks at the end of the list it
 * keeps its laid-out height, with the lost height as empty space below its content, decided in
 * the same measure pass, so the list's length and every card above stay where they were. When the
 * item grows again it takes back that space first. The space is dropped only once it is entirely
 * below the viewport, which needs the gambler to have scrolled up by at least its height, so the
 * drop is never seen.
 */
@Stable
internal class EndOfListStillness(private val listState: LazyListState) {
    private var trackedKey: Any? = null
    private var contentHeight = 0
    private var reserve by mutableIntStateOf(0)

    /** Measures the list's last item, identified by [key], keeping its height through a shrink. */
    fun Modifier.trackLastItem(key: Any): Modifier = layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        heightMeasured(key = key, height = placeable.height)
        layout(placeable.width, placeable.height + reserve) { placeable.place(0, 0) }
    }

    private fun heightMeasured(key: Any, height: Int) {
        if (key != trackedKey) {
            trackedKey = key
            contentHeight = height
            reserve = 0
            return
        }
        val change = height - contentHeight
        contentHeight = height
        reserve = when {
            change < 0 && !listState.canScrollForward -> reserve - change
            change > 0 -> (reserve - change).coerceAtLeast(0)
            else -> reserve
        }
    }

    /** The reserved space of the tracked item is entirely below the viewport. */
    internal val isReserveOffScreen: Boolean
        get() {
            if (reserve == 0) return false
            val info = listState.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { item -> item.key == trackedKey } ?: return true
            return item.offset + item.size - reserve >= info.viewportEndOffset
        }

    internal fun releaseOffScreen() {
        reserve = 0
    }
}

@Composable
internal fun rememberEndOfListStillness(listState: LazyListState): EndOfListStillness {
    val stillness = remember(listState) { EndOfListStillness(listState) }
    LaunchedEffect(stillness) {
        snapshotFlow { stillness.isReserveOffScreen }
            .distinctUntilChanged()
            .filter { isOffScreen -> isOffScreen }
            .collect { stillness.releaseOffScreen() }
    }
    return stillness
}
