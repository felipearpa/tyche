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
    lazyListState: LazyListState = rememberLazyListState(),
    statefulLazyColumnState: StatefulLazyColumnState = rememberStatefulLazyColumnState(
        lazyPagingItems,
    ),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
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
            lazyListState = lazyListState,
            statefulLazyColumnState = statefulLazyColumnState,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
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
        val refreshLoadState = lazyPagingItems.loadState.refresh

        LaunchedEffect(refreshLoadState, statefulLazyColumnState) {
            isRefreshing =
                refreshLoadState is LoadState.Loading && statefulLazyColumnState !is StatefulLazyColumnState.Loading
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            modifier = modifier,
            state = pullToRefreshState,
            onRefresh = { lazyPagingItems.refresh() },
        ) {
            StatefulLazyColumn(
                modifier = Modifier.fillMaxSize(),
                lazyPagingItems = lazyPagingItems,
                lazyListState = lazyListState,
                contentPadding = contentPadding,
                reverseLayout = reverseLayout,
                verticalArrangement = verticalArrangement,
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
    lazyListState: LazyListState = rememberLazyListState(),
    statefulLazyColumnState: StatefulLazyColumnState = rememberStatefulLazyColumnState(
        lazyPagingItems,
    ),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
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
        lazyListState = lazyListState,
        statefulLazyColumnState = statefulLazyColumnState,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        loadingContent = loadingContent,
        loadingContentOnConcatenate = loadingContentOnConcatenate,
        errorContentOnConcatenate = errorContentOnConcatenate,
        errorContent = errorContent,
        emptyContent = emptyContent,
    ) {
        itemContent()
    }
}
