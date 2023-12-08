package com.felipearpa.tyche.ui.lazy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun <T : Any> RefreshableLazyColumn(
    modifier: Modifier = Modifier,
    lazyItems: LazyPagingItems<T>,
    state: LazyListState = rememberLazyListState(),
    loadingContent: @Composable () -> Unit = {},
    loadingContentOnConcatenate: (LazyListScope.() -> Unit) = {},
    itemContent: LazyListScope.() -> Unit
) = RefreshableLazyColumn(
    modifier = modifier,
    lazyItems = lazyItems,
    state = state,
    contentPadding = PaddingValues(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    loadingContent = loadingContent,
    loadingContentOnConcatenate = loadingContentOnConcatenate,
    errorContentOnConcatenate = { contentOnConcatenateError(lazyItems = lazyItems) },
    errorContent = { ContentOnError(lazyItems = lazyItems) },
    emptyContent = { ContentOnEmpty() },
    itemContent = itemContent
)

@Composable
fun <T : Any> RefreshableLazyColumn(
    modifier: Modifier = Modifier,
    lazyItems: LazyPagingItems<T>,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Bottom,
    loadingContent: @Composable () -> Unit = {},
    loadingContentOnConcatenate: (LazyListScope.() -> Unit) = {},
    errorContentOnConcatenate: (LazyListScope.() -> Unit) = {},
    errorContent: @Composable (Throwable) -> Unit = {},
    emptyContent: @Composable () -> Unit = {},
    itemContent: LazyListScope.() -> Unit
) {
    val swipeRefreshState = rememberSwipeRefreshState(false)

    SwipeRefresh(
        modifier = modifier,
        state = swipeRefreshState,
        onRefresh = {
            swipeRefreshState.isRefreshing = false
            lazyItems.refresh()
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            lazyItems = lazyItems,
            state = state,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            loadingContent = loadingContent,
            loadingContentOnConcatenate = loadingContentOnConcatenate,
            errorContentOnConcatenate = errorContentOnConcatenate,
            errorContent = errorContent,
            emptyContent = emptyContent,
            itemContent = itemContent
        )
    }
}