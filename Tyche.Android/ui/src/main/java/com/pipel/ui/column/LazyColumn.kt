package com.pipel.ui.column

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.items
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.pipel.ui.gesturesDisabled

@Composable
private fun <TModel : Any> LazyColumn(
    modifier: Modifier = Modifier,
    lazyItems: LazyPagingItems<TModel>,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Bottom,
    filterContent: @Composable (() -> Unit)? = null,
    onLoadingContent: @Composable (() -> Unit)? = null,
    onLoadingAppendContent: @Composable () -> Unit,
    onErrorAppendContent: @Composable () -> Unit,
    onErrorContent: @Composable () -> Unit,
    onEmptyContent: @Composable () -> Unit,
    itemContent: @Composable (TModel) -> Unit
) {
    LazyColumn(
        modifier = modifier.gesturesDisabled(lazyItems.loadState.refresh is LoadState.Loading),
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement
    ) {
        filterContent?.let { filterContent ->
            item { filterContent() }
        }

        with(lazyItems) {

            val defaultContent = {
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

            if (loadState.refresh !is LoadState.Loading) {
                defaultContent()
            }

            when {
                loadState.refresh is LoadState.Loading -> {
                    item {
                        onLoadingContent?.let { loadingContent ->
                            loadingContent()
                        } ?: run {
                            defaultContent()
                        }
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
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Bottom,
    filterContent: @Composable (() -> Unit)? = null,
    onLoadingContent: @Composable (() -> Unit)? = null,
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
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            onLoadingContent = onLoadingContent,
            onLoadingAppendContent = onLoadingAppendContent,
            onErrorAppendContent = onErrorAppendContent,
            onErrorContent = onErrorContent,
            onEmptyContent = onEmptyContent,
            itemContent = itemContent
        )
    }
}