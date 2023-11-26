package com.felipearpa.tyche.ui.lazy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.felipearpa.tyche.ui.disabledGestures

@Composable
fun <Value : Any> LazyColumn(
    modifier: Modifier = Modifier,
    lazyItems: LazyPagingItems<Value>,
    topContent: (LazyListScope.() -> Unit) = {},
    loadingContent: (LazyListScope.() -> Unit) = {},
    itemContent: LazyListScope.() -> Unit
) = LazyColumn(
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

fun LazyListScope.contentOnConcatenating() {
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

fun <Value : Any> LazyListScope.contentOnError(lazyItems: LazyPagingItems<Value>) {
    item {
        Failure(modifier = Modifier.fillMaxWidth()) {
            lazyItems.retry()
        }
    }
}

fun LazyListScope.contentOnEmpty() {
    item {
        Empty(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun <Value : Any> LazyColumn(
    modifier: Modifier = Modifier,
    lazyItems: LazyPagingItems<Value>,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Bottom,
    topContent: (LazyListScope.() -> Unit) = {},
    loadingContent: (LazyListScope.() -> Unit) = {},
    loadingContentOnConcatenate: (LazyListScope.() -> Unit) = {},
    errorContentOnConcatenate: (LazyListScope.() -> Unit) = {},
    errorContent: (LazyListScope.(Throwable) -> Unit) = {},
    emptyContent: (LazyListScope.() -> Unit) = {},
    itemContent: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier.disabledGestures(lazyItems.loadState.refresh is LoadState.Loading),
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement
    ) {
        topContent()

        prependContent(
            lazyItems = lazyItems,
            loadingContentOnConcatenate = loadingContentOnConcatenate,
            errorContentOnConcatenate = errorContentOnConcatenate
        )

        mainContent(
            lazyItems = lazyItems,
            loadingContent = loadingContent,
            errorContent = errorContent,
            emptyContent = emptyContent,
            itemContent = itemContent
        )

        appendContent(
            lazyItems = lazyItems,
            loadingContentOnConcatenate = loadingContentOnConcatenate,
            errorContentOnConcatenate = errorContentOnConcatenate
        )
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

private fun <Value : Any> LazyListScope.mainContent(
    lazyItems: LazyPagingItems<Value>,
    loadingContent: (LazyListScope.() -> Unit) = {},
    errorContent: (LazyListScope.(Throwable) -> Unit) = {},
    emptyContent: (LazyListScope.() -> Unit) = {},
    itemContent: LazyListScope.() -> Unit
) {
    when (lazyItems.loadState.refresh) {
        is LoadState.NotLoading -> if (!lazyItems.hasItems()) {
            emptyContent()
        } else {
            itemContent()
        }

        LoadState.Loading -> loadingContent()

        else -> {
            val errorLoadState = lazyItems.loadState.refresh as LoadState.Error
            errorContent(errorLoadState.error)
        }
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