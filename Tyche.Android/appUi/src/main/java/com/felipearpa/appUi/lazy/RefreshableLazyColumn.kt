package com.felipearpa.appUi.lazy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.felipearpa.ui.lazy.RefreshableLazyColumn

@Composable
fun <T : Any> RefreshableLazyColumn(
    modifier: Modifier = Modifier,
    lazyItems: LazyPagingItems<T>,
    topContent: (LazyListScope.() -> Unit)? = null,
    loadingContent: (LazyListScope.() -> Unit)? = null,
    itemContent: LazyListScope.() -> Unit
) = RefreshableLazyColumn(
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