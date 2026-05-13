package com.felipearpa.tyche.ui.lazy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.felipearpa.ui.lazy.LazyPagingColumnState
import com.felipearpa.ui.lazy.RefreshableLazyPagingColumn
import com.felipearpa.ui.lazy.rememberLazyPagingColumnState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <Item : Any> RefreshableLazyPagingColumn(
    modifier: Modifier = Modifier,
    lazyPagingItems: LazyPagingItems<Item>,
    lazyListState: LazyListState = rememberLazyListState(),
    lazyPagingColumnState: LazyPagingColumnState = rememberLazyPagingColumnState(lazyPagingItems),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    loadingContent: LazyListScope.() -> Unit = {},
    errorContent: LazyListScope.(Throwable) -> Unit = { exception -> lazyPagingColumnError(exception) },
    emptyContent: LazyListScope.() -> Unit = { lazyPagingColumnEmpty() },
    prependLoadingContent: LazyListScope.() -> Unit = {},
    appendLoadingContent: LazyListScope.() -> Unit = {},
    prependErrorContent: LazyListScope.(Throwable) -> Unit = {
        lazyPagingConcatenateError(
            exception = it,
            retry = lazyPagingItems::retry,
        )
    },
    appendErrorContent: LazyListScope.(Throwable) -> Unit = {
        lazyPagingConcatenateError(
            exception = it,
            retry = lazyPagingItems::retry,
        )
    },
    itemContent: LazyListScope.() -> Unit,
) {
    RefreshableLazyPagingColumn(
        modifier = modifier,
        lazyPagingItems = lazyPagingItems,
        lazyListState = lazyListState,
        lazyPagingColumnState = lazyPagingColumnState,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        loadingContent = loadingContent,
        errorContent = errorContent,
        emptyContent = emptyContent,
        prependLoadingContent = prependLoadingContent,
        appendLoadingContent = appendLoadingContent,
        prependErrorContent = prependErrorContent,
        appendErrorContent = appendErrorContent,
        itemContent = itemContent,
    )
}
