package com.felipearpa.tyche.ui.lazy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
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
    topContent: (LazyListScope.() -> Unit)? = null,
    loadingContent: (LazyListScope.() -> Unit)? = null,
    itemContent: LazyListScope.() -> Unit
) = RefreshableLazyColumn(
    modifier = modifier,
    lazyItems = lazyItems,
    contentPadding = PaddingValues(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    topContent = topContent,
    loadingContent = loadingContent,
    loadingContentOnConcatenate = { contentOnConcatenating() },
    errorContentOnConcatenate = { contentOnConcatenateError(lazyItems = lazyItems) },
    errorContent = { contentOnError(lazyItems = lazyItems) },
    emptyContent = { contentOnEmpty() },
    itemContent = itemContent
)

@Composable
fun <T : Any> RefreshableLazyColumn(
    modifier: Modifier = Modifier,
    lazyItems: LazyPagingItems<T>,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Bottom,
    topContent: (LazyListScope.() -> Unit)? = null,
    loadingContent: (LazyListScope.() -> Unit)? = null,
    loadingContentOnConcatenate: (LazyListScope.() -> Unit)? = null,
    errorContentOnConcatenate: (LazyListScope.() -> Unit)? = null,
    errorContent: (LazyListScope.(Throwable) -> Unit)? = null,
    emptyContent: (LazyListScope.() -> Unit)? = null,
    itemContent: LazyListScope.() -> Unit
) {
    val swipeRefreshState = rememberSwipeRefreshState(false)
    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = {
            swipeRefreshState.isRefreshing = false
            lazyItems.refresh()
        }
    ) {
        LazyColumn(
            modifier = modifier,
            lazyItems = lazyItems,
            topContent = topContent,
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