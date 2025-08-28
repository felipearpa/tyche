package com.felipearpa.tyche.ui.lazy

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.felipearpa.tyche.ui.exception.localizedOrDefault
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing

@Composable
fun <Item : Any> StatefulLazyColumn(
    modifier: Modifier = Modifier,
    lazyPagingItems: LazyPagingItems<Item>,
    lazyListState: LazyListState = rememberLazyListState(),
    statefulLazyColumnState: StatefulLazyColumnState = rememberStatefulLazyColumnState(
        lazyPagingItems,
    ),
    loadingContent: LazyListScope.() -> Unit = {},
    refreshLoadingContent: @Composable () -> Unit = {},
    itemContent: LazyListScope.() -> Unit,
) = StatefulLazyColumn(
    modifier = modifier,
    lazyPagingItems = lazyPagingItems,
    lazyListState = lazyListState,
    statefulLazyColumnState = statefulLazyColumnState,
    contentPadding = PaddingValues(LocalBoxSpacing.current.medium),
    verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
    loadingContent = loadingContent,
    refreshLoadingContent = refreshLoadingContent,
    loadingContentOnConcatenate = { statefulLazyColumnContentOnConcatenate() },
    errorContentOnConcatenate = { statefulLazyColumnContentOnConcatenateError(lazyPagingItems = lazyPagingItems) },
    errorContent = { exception -> statefulLazyColumnContentOnError(exception) },
    emptyContent = { statefulLazyColumnContentOnEmpty() },
    itemContent = itemContent,
)

@Composable
fun <Item : Any> StatefulLazyColumn(
    modifier: Modifier = Modifier,
    lazyPagingItems: LazyPagingItems<Item>,
    lazyListState: LazyListState = rememberLazyListState(),
    statefulLazyColumnState: StatefulLazyColumnState = rememberStatefulLazyColumnState(
        lazyPagingItems,
    ),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    loadingContent: LazyListScope.() -> Unit = {},
    refreshLoadingContent: @Composable () -> Unit = {},
    loadingContentOnConcatenate: LazyListScope.() -> Unit = {},
    errorContentOnConcatenate: LazyListScope.() -> Unit = {},
    errorContent: LazyListScope.(Throwable) -> Unit = {},
    emptyContent: LazyListScope.() -> Unit = {},
    itemContent: LazyListScope.() -> Unit,
) {
    if (LocalInspectionMode.current) {
        StatefulLazyColumnForPreview(
            lazyListState = lazyListState,
            lazyPagingItems = lazyPagingItems,
            modifier = modifier,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            emptyContent = emptyContent,
            itemContent = itemContent,
        )
    } else {
        val shouldShowRefreshIndicator =
            lazyPagingItems.loadState.refresh is LoadState.Loading && statefulLazyColumnState !is StatefulLazyColumnState.Loading

        Box(modifier = Modifier.fillMaxWidth()) {
            AnimatedVisibility(
                visible = shouldShowRefreshIndicator,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(LocalBoxSpacing.current.medium)
                        .animateContentSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    refreshLoadingContent()
                }
            }
        }

        LazyColumn(
            state = lazyListState,
            modifier = modifier,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
        ) {
            when (statefulLazyColumnState) {
                StatefulLazyColumnState.Loading -> loadingContent()
                StatefulLazyColumnState.Empty -> emptyContent()
                is StatefulLazyColumnState.Error -> errorContent(statefulLazyColumnState.exception)
                StatefulLazyColumnState.Content ->
                    statefulLazyColumnContent(
                        lazyPagingItems = lazyPagingItems,
                        loadingContentOnConcatenate = loadingContentOnConcatenate,
                        errorContentOnConcatenate = errorContentOnConcatenate,
                        itemContent = itemContent,
                    )
            }
        }
    }
}

private fun <Item : Any> LazyListScope.statefulLazyColumnContent(
    lazyPagingItems: LazyPagingItems<Item>,
    loadingContentOnConcatenate: LazyListScope.() -> Unit = {},
    errorContentOnConcatenate: LazyListScope.() -> Unit = {},
    itemContent: LazyListScope.() -> Unit,
) {
    prependContent(
        lazyPagingItems = lazyPagingItems,
        loadingContentOnConcatenate = loadingContentOnConcatenate,
        errorContentOnConcatenate = errorContentOnConcatenate,
    )
    itemContent()
    appendContent(
        lazyPagingItems = lazyPagingItems,
        loadingContentOnConcatenate = loadingContentOnConcatenate,
        errorContentOnConcatenate = errorContentOnConcatenate,
    )
}

private fun <Item : Any> LazyListScope.prependContent(
    lazyPagingItems: LazyPagingItems<Item>,
    loadingContentOnConcatenate: (LazyListScope.() -> Unit) = {},
    errorContentOnConcatenate: (LazyListScope.() -> Unit) = {},
) {
    if (lazyPagingItems.loadState.prepend is LoadState.Loading) {
        loadingContentOnConcatenate()
    } else if (lazyPagingItems.loadState.prepend is LoadState.Error) {
        errorContentOnConcatenate()
    }
}

private fun <Item : Any> LazyListScope.appendContent(
    lazyPagingItems: LazyPagingItems<Item>,
    loadingContentOnConcatenate: (LazyListScope.() -> Unit) = {},
    errorContentOnConcatenate: (LazyListScope.() -> Unit) = {},
) {
    if (lazyPagingItems.loadState.append is LoadState.Loading) {
        loadingContentOnConcatenate()
    } else if (lazyPagingItems.loadState.append is LoadState.Error) {
        errorContentOnConcatenate()
    }
}

fun LazyListScope.statefulLazyColumnContentOnConcatenate() {
    item {
        ProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LocalBoxSpacing.current.medium),
        )
    }
}

fun <Item : Any> LazyListScope.statefulLazyColumnContentOnConcatenateError(lazyPagingItems: LazyPagingItems<Item>) {
    item {
        Retry(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = LocalBoxSpacing.current.medium),
        ) { lazyPagingItems.retry() }
    }
}

fun LazyListScope.statefulLazyColumnContentOnError(exception: Throwable) {
    item {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillParentMaxSize(),
        ) {
            Failure(
                localizedException = exception.localizedOrDefault(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

fun LazyListScope.statefulLazyColumnContentOnEmpty() {
    item {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillParentMaxSize(),
        ) {
            Empty(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun <Item : Any> StatefulLazyColumnForPreview(
    lazyListState: LazyListState,
    lazyPagingItems: LazyPagingItems<Item>,
    modifier: Modifier,
    contentPadding: PaddingValues,
    verticalArrangement: Arrangement.Vertical,
    emptyContent: LazyListScope.() -> Unit = {},
    itemContent: LazyListScope.() -> Unit,
) {
    LazyColumn(
        state = lazyListState,
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
    ) {
        if (lazyPagingItems.isEmpty)
            itemContent()
        else
            emptyContent()
    }
}
