package com.felipearpa.tyche.ui.lazy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <Value : Any> RefreshableStatefulLazyColumn(
    modifier: Modifier = Modifier,
    lazyPagingItems: LazyPagingItems<Value>,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    loadingVisibilityDecider: LoadingVisibilityDecider<Value> = FirstTimeLoadingVisibilityDecider(),
    loadingContent: LazyListScope.() -> Unit = {},
    loadingContentOnConcatenate: LazyListScope.() -> Unit = {},
    errorContentOnConcatenate: LazyListScope.() -> Unit = {},
    errorContent: LazyListScope.(Throwable) -> Unit = { exception ->
        statefulLazyColumnContentOnError(exception)
    },
    emptyContent: LazyListScope.() -> Unit = { statefulLazyColumnContentOnEmpty() },
    itemContent: LazyListScope.() -> Unit,
) {
    if (LocalInspectionMode.current) {
        RefreshableStatefulLazyColumnForPreview(
            modifier = modifier,
            lazyPagingItems = lazyPagingItems,
            state = state,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            loadingVisibilityDecider = loadingVisibilityDecider,
            loadingContent = loadingContent,
            loadingContentOnConcatenate = loadingContentOnConcatenate,
            errorContentOnConcatenate = errorContentOnConcatenate,
            errorContent = errorContent,
            emptyContent = emptyContent,
            itemContent = itemContent,
        )
    } else {
        val pullToRefreshState = rememberPullToRefreshState()
        var isRefreshing by remember { mutableStateOf(false) }
        var hasShownLoading by remember { mutableStateOf(false) }

        val shouldLoadingContent = loadingVisibilityDecider.shouldShowLoader(lazyPagingItems)
        val onRefresh: () -> Unit = {
            lazyPagingItems.refresh()
            if (shouldLoadingContent) {
                isRefreshing = false
            }
        }

        LaunchedEffect(lazyPagingItems.loadState.refresh) {
            if (lazyPagingItems.loadState.refresh is LoadState.Loading) {
                hasShownLoading = true
                if (!shouldLoadingContent) {
                    isRefreshing = true
                }
            }

            if (hasShownLoading && (lazyPagingItems.loadState.refresh is LoadState.NotLoading || lazyPagingItems.loadState.refresh is LoadState.Error)) {
                hasShownLoading = false
                if (!shouldLoadingContent) {
                    isRefreshing = false
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            modifier = modifier,
            state = pullToRefreshState,
            onRefresh = onRefresh,
        ) {
            StatefulLazyColumn(
                modifier = Modifier.fillMaxSize(),
                lazyPagingItems = lazyPagingItems,
                state = state,
                contentPadding = contentPadding,
                reverseLayout = reverseLayout,
                verticalArrangement = verticalArrangement,
                loadingVisibilityDecider = loadingVisibilityDecider,
                loadingContent = loadingContent,
                loadingContentOnConcatenate = loadingContentOnConcatenate,
                errorContentOnConcatenate = errorContentOnConcatenate,
                errorContent = errorContent,
                emptyContent = emptyContent,
                itemContent = itemContent,
            )
        }
    }
}

@Composable
private fun <Value : Any> RefreshableStatefulLazyColumnForPreview(
    modifier: Modifier = Modifier,
    lazyPagingItems: LazyPagingItems<Value>,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    loadingVisibilityDecider: LoadingVisibilityDecider<Value> = FirstTimeLoadingVisibilityDecider(),
    loadingContent: LazyListScope.() -> Unit = {},
    loadingContentOnConcatenate: LazyListScope.() -> Unit = {},
    errorContentOnConcatenate: LazyListScope.() -> Unit = {},
    errorContent: LazyListScope.(Throwable) -> Unit = { exception ->
        statefulLazyColumnContentOnError(exception)
    },
    emptyContent: LazyListScope.() -> Unit = { statefulLazyColumnContentOnEmpty() },
    itemContent: LazyListScope.() -> Unit,
) {
    StatefulLazyColumn(
        modifier = modifier,
        lazyPagingItems = lazyPagingItems,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        loadingVisibilityDecider = loadingVisibilityDecider,
        loadingContent = loadingContent,
        loadingContentOnConcatenate = loadingContentOnConcatenate,
        errorContentOnConcatenate = errorContentOnConcatenate,
        errorContent = errorContent,
        emptyContent = emptyContent,
    ) {
        itemContent()
    }
}
