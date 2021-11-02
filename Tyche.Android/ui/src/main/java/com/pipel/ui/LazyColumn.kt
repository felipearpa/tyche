package com.pipel.ui

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.items
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
private fun <TModel : Any> LazyColumn(
    modifier: Modifier = Modifier,
    lazyItems: LazyPagingItems<TModel>,
    filterContent: @Composable (() -> Unit)? = null,
    onLoadingContent: @Composable () -> Unit,
    onLoadingAppendContent: @Composable () -> Unit,
    onErrorAppendContent: @Composable () -> Unit,
    onErrorContent: @Composable () -> Unit,
    onEmptyContent: @Composable () -> Unit,
    itemContent: @Composable (TModel) -> Unit
) {
    LazyColumn(
        modifier = modifier.gesturesDisabled(lazyItems.loadState.refresh is LoadState.Loading)
    ) {
        filterContent?.let { filterContent ->
            item { filterContent() }
        }

        with(lazyItems) {
            if (loadState.refresh !is LoadState.Loading) {
                items(lazyItems) { item ->
                    itemContent(item!!)
                }
                if (lazyItems.itemCount == 0
                    && loadState.prepend.endOfPaginationReached
                    && loadState.append.endOfPaginationReached
                ) {
                    item {
                        onEmptyContent()
                    }
                }
            }

            when {
                loadState.refresh is LoadState.Loading -> {
                    item {
                        onLoadingContent()
                    }
                }

                loadState.refresh is LoadState.Error -> {
                    item {
                        onErrorContent()
                    }
                }

                loadState.append is LoadState.Loading -> {
                    item {
                        onLoadingAppendContent()
                    }
                }

                loadState.append is LoadState.Error -> {
                    item {
                        onErrorAppendContent()
                    }
                }
            }
        }
    }
}

@Composable
fun <TModel : Any> RefreshableLazyColumn(
    modifier: Modifier = Modifier,
    lazyItems: LazyPagingItems<TModel>,
    filterContent: @Composable (() -> Unit)? = null,
    onLoadingContent: @Composable () -> Unit,
    onLoadingAppendContent: @Composable () -> Unit,
    onErrorAppendContent: @Composable () -> Unit,
    onErrorContent: @Composable () -> Unit,
    onEmptyContent: @Composable () -> Unit,
    itemContent: @Composable (TModel) -> Unit
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
            filterContent = filterContent,
            onLoadingContent = onLoadingContent,
            onLoadingAppendContent = onLoadingAppendContent,
            onErrorAppendContent = onErrorAppendContent,
            onErrorContent = onErrorContent,
            onEmptyContent = onEmptyContent,
            itemContent = itemContent
        )
    }
}