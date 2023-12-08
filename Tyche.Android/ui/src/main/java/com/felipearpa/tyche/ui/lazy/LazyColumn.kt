package com.felipearpa.tyche.ui.lazy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.felipearpa.tyche.ui.disabledGestures

@Composable
fun <Value : Any> LazyColumn(
    modifier: Modifier = Modifier,
    lazyItems: LazyPagingItems<Value>,
    state: LazyListState = rememberLazyListState(),
    loadingContent: @Composable () -> Unit = {},
    itemContent: LazyListScope.() -> Unit
) = LazyColumn(
    modifier = modifier,
    lazyItems = lazyItems,
    state = state,
    contentPadding = PaddingValues(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    loadingContent = loadingContent,
    loadingContentOnConcatenate = { contentOnConcatenate() },
    errorContentOnConcatenate = { contentOnConcatenateError(lazyItems = lazyItems) },
    errorContent = { ContentOnError(lazyItems = lazyItems) },
    emptyContent = { ContentOnEmpty() },
    itemContent = itemContent
)

fun LazyListScope.contentOnConcatenate() {
    item {
        ProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
    }
}

fun <Value : Any> LazyListScope.contentOnConcatenateError(lazyItems: LazyPagingItems<Value>) {
    item {
        Retry(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) { lazyItems.retry() }
    }
}

@Composable
fun <Value : Any> ContentOnError(lazyItems: LazyPagingItems<Value>) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Failure(modifier = Modifier.fillMaxWidth()) {
            lazyItems.retry()
        }
    }
}

@Composable
fun ContentOnEmpty() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Empty(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun <Value : Any> LazyColumn(
    modifier: Modifier = Modifier,
    lazyItems: LazyPagingItems<Value>,
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
    when (lazyItems.loadState.refresh) {
        is LoadState.Error -> {
            val errorLoadState = lazyItems.loadState.refresh as LoadState.Error
            errorContent(errorLoadState.error)
        }

        is LoadState.Loading -> loadingContent()

        is LoadState.NotLoading -> {
            if (!lazyItems.hasItems()) {
                emptyContent()
            } else {
                LazyColumn(
                    state = state,
                    modifier = modifier.disabledGestures(lazyItems.loadState.refresh is LoadState.Loading),
                    contentPadding = contentPadding,
                    verticalArrangement = verticalArrangement
                ) {
                    prependContent(
                        lazyItems = lazyItems,
                        loadingContentOnConcatenate = loadingContentOnConcatenate,
                        errorContentOnConcatenate = errorContentOnConcatenate
                    )

                    itemContent()

                    appendContent(
                        lazyItems = lazyItems,
                        loadingContentOnConcatenate = loadingContentOnConcatenate,
                        errorContentOnConcatenate = errorContentOnConcatenate
                    )
                }
            }
        }
    }
}

private fun <Value : Any> LazyPagingItems<Value>.hasItems() =
    this.itemCount > 0
            || !this.loadState.prepend.endOfPaginationReached
            || !this.loadState.append.endOfPaginationReached

private fun <Value : Any> LazyListScope.prependContent(
    lazyItems: LazyPagingItems<Value>,
    loadingContentOnConcatenate: (LazyListScope.() -> Unit) = {},
    errorContentOnConcatenate: (LazyListScope.() -> Unit) = {},
) {
    if (lazyItems.loadState.prepend is LoadState.Loading) {
        loadingContentOnConcatenate()
    } else if (lazyItems.loadState.prepend is LoadState.Error) {
        errorContentOnConcatenate()
    }
}

private fun <Value : Any> LazyListScope.appendContent(
    lazyItems: LazyPagingItems<Value>,
    loadingContentOnConcatenate: (LazyListScope.() -> Unit) = {},
    errorContentOnConcatenate: (LazyListScope.() -> Unit) = {},
) {
    if (lazyItems.loadState.append is LoadState.Loading) {
        loadingContentOnConcatenate()
    } else if (lazyItems.loadState.append is LoadState.Error) {
        errorContentOnConcatenate()
    }
}