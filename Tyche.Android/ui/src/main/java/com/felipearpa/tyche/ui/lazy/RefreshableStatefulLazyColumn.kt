package com.felipearpa.tyche.ui.lazy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.felipearpa.tyche.ui.loading.BallSpinner

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
    itemContent: LazyListScope.() -> Unit
) {
    val pullToRefreshState = rememberPullToRefreshState()

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(Unit) {
            lazyItems.refresh()
        }
    }

    if (lazyItems.loadState.refresh is LoadState.NotLoading) {
        LaunchedEffect(Unit) {
            if (pullToRefreshState.isRefreshing) pullToRefreshState.endRefresh()
        }
    }

    Box(modifier = modifier.nestedScroll(pullToRefreshState.nestedScrollConnection)) {
        StatefulLazyColumn(
            modifier = Modifier.fillMaxSize(),
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
            itemContent = itemContent
        )

        PullToRefreshContainer(
            modifier = Modifier.align(Alignment.TopCenter),
            state = pullToRefreshState,
            indicator = {
                BallSpinner()
            }
        )
    }
}