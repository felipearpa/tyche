package com.felipearpa.appUi.lazy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.felipearpa.ui.lazy.LazyColumn

@Composable
fun <T : Any> LazyColumn(
    modifier: Modifier = Modifier,
    lazyItems: LazyPagingItems<T>,
    topContent: (LazyListScope.() -> Unit)? = null,
    loadingContent: (LazyListScope.() -> Unit)? = null,
    itemContent: LazyListScope.() -> Unit
) = LazyColumn(
    modifier = Modifier,
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
        com.felipearpa.appUi.lazy.ProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
    }
}

fun <T : Any> LazyListScope.contentOnConcatenateError(lazyItems: LazyPagingItems<T>) {
    item {
        com.felipearpa.appUi.lazy.Retry(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) { lazyItems.retry() }
    }
}

fun <T : Any> LazyListScope.contentOnError(lazyItems: LazyPagingItems<T>) {
    item {
        com.felipearpa.appUi.lazy.Failure(modifier = Modifier.fillMaxWidth()) {
            lazyItems.retry()
        }
    }
}

fun LazyListScope.contentOnEmpty() {
    item {
        com.felipearpa.appUi.lazy.Empty(modifier = Modifier.fillMaxWidth())
    }
}