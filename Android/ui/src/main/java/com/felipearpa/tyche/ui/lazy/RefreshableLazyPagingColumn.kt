package com.felipearpa.tyche.ui.lazy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.felipearpa.ui.lazy.LazyPagingColumn
import com.felipearpa.ui.lazy.LazyPagingColumnState
import com.felipearpa.ui.lazy.rememberLazyPagingColumnState

/**
 * A pull-to-refresh paging list whose viewport can extend under app bars and system bars.
 *
 * Pass the screen's insets as [contentPadding] rather than padding the list's parent, so
 * rows scroll to the window edges while the first and last rows can still be fully
 * revealed. Both refresh indicators start below [contentPadding]'s top edge, keeping them
 * clear of a top app bar that the list scrolls beneath. The lazy-paging library's own
 * refreshable column pins its pull indicator to the viewport top, so this composes its
 * plain paging column with a Material pull-to-refresh box instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <Item : Any> RefreshableLazyPagingColumn(
    modifier: Modifier = Modifier,
    lazyPagingItems: LazyPagingItems<Item>,
    lazyListState: LazyListState = rememberLazyListState(),
    lazyPagingColumnState: LazyPagingColumnState = rememberLazyPagingColumnState(lazyPagingItems),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    loadingContent: LazyListScope.() -> Unit = {},
    errorContent: LazyListScope.(Throwable) -> Unit = { exception -> lazyPagingColumnError(exception) },
    emptyContent: LazyListScope.() -> Unit = { lazyPagingColumnEmpty() },
    prependLoadingContent: LazyListScope.() -> Unit = {},
    appendLoadingContent: LazyListScope.() -> Unit = {},
    prependErrorContent: LazyListScope.(Throwable) -> Unit = {
        lazyPagingConcatenateError(
            exception = it,
            onRetry = lazyPagingItems::retry,
        )
    },
    appendErrorContent: LazyListScope.(Throwable) -> Unit = {
        lazyPagingConcatenateError(
            exception = it,
            onRetry = lazyPagingItems::retry,
        )
    },
    itemContent: LazyListScope.() -> Unit,
) {
    val pullToRefreshState = rememberPullToRefreshState()
    // True only for a refresh the gambler pulled for; other refreshes use the inline indicator.
    var isRefreshing by remember { mutableStateOf(false) }
    val refreshLoadState = lazyPagingItems.loadState.refresh

    LaunchedEffect(refreshLoadState, lazyPagingColumnState) {
        if (isRefreshing && refreshLoadState !is LoadState.Loading) {
            isRefreshing = false
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            lazyPagingItems.refresh()
        },
        modifier = modifier,
        state = pullToRefreshState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = contentPadding.calculateTopPadding()),
            )
        },
    ) {
        LazyPagingColumn(
            modifier = Modifier.fillMaxSize(),
            lazyPagingItems = lazyPagingItems,
            lazyListState = lazyListState,
            lazyPagingColumnState = lazyPagingColumnState,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            loadingContent = loadingContent,
            refreshLoadingContent = {
                if (!isRefreshing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = contentPadding.calculateTopPadding()),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(all = 16.dp))
                    }
                }
            },
            errorContent = errorContent,
            emptyContent = emptyContent,
            prependLoadingContent = prependLoadingContent,
            appendLoadingContent = appendLoadingContent,
            prependErrorContent = prependErrorContent,
            appendErrorContent = appendErrorContent,
            itemContent = itemContent,
        )
    }
}
