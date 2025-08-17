package com.felipearpa.tyche.ui.lazy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
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
    lazyItems: LazyPagingItems<Value>,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    loadingVisibilityDecider: LoadingVisibilityDecider<Value> = FirstTimeLoadingVisibilityDecider(),
    loadingContent: @Composable () -> Unit = {},
    loadingContentOnConcatenate: (LazyListScope.() -> Unit) = {},
    errorContentOnConcatenate: (LazyListScope.() -> Unit) = {},
    errorContent: @Composable (Throwable) -> Unit = { ContentOnError(lazyItems = lazyItems) },
    emptyContent: @Composable () -> Unit = { ContentOnEmpty() },
    itemContent: LazyListScope.() -> Unit,
) {
    if (LocalInspectionMode.current) {
        RefreshableStatefulLazyColumnForPreview(
            modifier = modifier,
            lazyItems = lazyItems,
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
        var isRefreshing by remember { mutableStateOf(false) }
        val pullToRefreshState = rememberPullToRefreshState()
        val onRefresh: () -> Unit = { isRefreshing = true }

        if (isRefreshing) {
            LaunchedEffect(Unit) { lazyItems.refresh() }
        }

        if (lazyItems.loadState.refresh is LoadState.NotLoading) {
            LaunchedEffect(Unit) { isRefreshing = false }
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            modifier = modifier,
            state = pullToRefreshState,
            onRefresh = onRefresh,
        ) {
            StatefulLazyColumn(
                modifier = Modifier.fillMaxWidth(),
                lazyItems = lazyItems,
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
    lazyItems: LazyPagingItems<Value>,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    loadingVisibilityDecider: LoadingVisibilityDecider<Value> = FirstTimeLoadingVisibilityDecider(),
    loadingContent: @Composable () -> Unit = {},
    loadingContentOnConcatenate: (LazyListScope.() -> Unit) = {},
    errorContentOnConcatenate: (LazyListScope.() -> Unit) = {},
    errorContent: @Composable (Throwable) -> Unit = { ContentOnError(lazyItems = lazyItems) },
    emptyContent: @Composable () -> Unit = { ContentOnEmpty() },
    itemContent: LazyListScope.() -> Unit,
) {
    StatefulLazyColumn(
        modifier = modifier,
        lazyItems = lazyItems,
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
