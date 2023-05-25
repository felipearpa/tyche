package com.felipearpa.ui.lazy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.felipearpa.core.ifNotNull
import com.felipearpa.ui.disabledGestures

@Composable
fun <T : Any> LazyColumn(
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
    LazyColumn(
        modifier = modifier.disabledGestures(lazyItems.loadState.refresh is LoadState.Loading),
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement
    ) {
        topContent.ifNotNull { nonNullableTopContent ->
            nonNullableTopContent()
        }

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

private fun <T : Any> LazyPagingItems<T>.hasItems() =
    this.itemCount > 0
            || !this.loadState.prepend.endOfPaginationReached
            || !this.loadState.append.endOfPaginationReached

private fun <T : Any> LazyListScope.prependContent(
    lazyItems: LazyPagingItems<T>,
    loadingContentOnConcatenate: (LazyListScope.() -> Unit)? = null,
    errorContentOnConcatenate: (LazyListScope.() -> Unit)? = null,
) {
    if (lazyItems.loadState.prepend is LoadState.Loading) {
        loadingContentOnConcatenate.ifNotNull { nonNullableLoadingContentOnConcatenate ->
            nonNullableLoadingContentOnConcatenate()
        }
    } else if (lazyItems.loadState.prepend is LoadState.Error) {
        errorContentOnConcatenate.ifNotNull { nonNullableErrorContentOnConcatenate ->
            nonNullableErrorContentOnConcatenate()
        }
    }
}

private fun <T : Any> LazyListScope.mainContent(
    lazyItems: LazyPagingItems<T>,
    loadingContent: (LazyListScope.() -> Unit)? = null,
    errorContent: (LazyListScope.(Throwable) -> Unit)? = null,
    emptyContent: (LazyListScope.() -> Unit)? = null,
    itemContent: LazyListScope.() -> Unit
) {
    when (lazyItems.loadState.refresh) {
        is LoadState.NotLoading -> if (!lazyItems.hasItems()) {
            emptyContent.ifNotNull { nonNullableEmptyContent ->
                nonNullableEmptyContent()
            }
        } else {
            itemContent()
        }

        LoadState.Loading -> loadingContent.ifNotNull { nonNullableLoadingContent ->
            nonNullableLoadingContent()
        }

        else -> errorContent.ifNotNull { nonNullableErrorContent ->
            val exception = lazyItems.loadState.refresh as LoadState.Error
            nonNullableErrorContent(exception.error)
        }
    }
}

private fun <T : Any> LazyListScope.appendContent(
    lazyItems: LazyPagingItems<T>,
    loadingContentOnConcatenate: (LazyListScope.() -> Unit)? = null,
    errorContentOnConcatenate: (LazyListScope.() -> Unit)? = null,
) {
    if (lazyItems.loadState.append is LoadState.Loading) {
        loadingContentOnConcatenate.ifNotNull { nonNullableLoadingContentOnConcatenate ->
            nonNullableLoadingContentOnConcatenate()
        }
    } else if (lazyItems.loadState.append is LoadState.Error) {
        errorContentOnConcatenate.ifNotNull { nonNullableErrorContentOnConcatenate ->
            nonNullableErrorContentOnConcatenate()
        }
    }
}