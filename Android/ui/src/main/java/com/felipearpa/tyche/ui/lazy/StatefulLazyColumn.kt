package com.felipearpa.tyche.ui.lazy

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.felipearpa.tyche.ui.exception.localizedOrDefault
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing

@Composable
fun <Value : Any> StatefulLazyColumn(
    modifier: Modifier = Modifier,
    lazyPagingItems: LazyPagingItems<Value>,
    state: LazyListState = rememberLazyListState(),
    loadingVisibilityDecider: LoadingVisibilityDecider<Value> = AlwaysLoadingVisibilityDecider(),
    loadingContent: LazyListScope.() -> Unit = {},
    itemContent: LazyListScope.() -> Unit,
) = StatefulLazyColumn(
    modifier = modifier,
    lazyPagingItems = lazyPagingItems,
    state = state,
    contentPadding = PaddingValues(LocalBoxSpacing.current.medium),
    verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
    loadingVisibilityDecider = loadingVisibilityDecider,
    loadingContent = loadingContent,
    loadingContentOnConcatenate = { statefulLazyColumnContentOnConcatenate() },
    errorContentOnConcatenate = { statefulLazyColumnContentOnConcatenateError(lazyPagingItems = lazyPagingItems) },
    errorContent = { exception -> statefulLazyColumnContentOnError(exception) },
    emptyContent = { statefulLazyColumnContentOnEmpty() },
    itemContent = itemContent,
)

@Composable
fun <Value : Any> StatefulLazyColumn(
    modifier: Modifier = Modifier,
    lazyPagingItems: LazyPagingItems<Value>,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    loadingVisibilityDecider: LoadingVisibilityDecider<Value> = AlwaysLoadingVisibilityDecider(),
    loadingContent: LazyListScope.() -> Unit = {},
    loadingContentOnConcatenate: LazyListScope.() -> Unit = {},
    errorContentOnConcatenate: LazyListScope.() -> Unit = {},
    errorContent: LazyListScope.(Throwable) -> Unit = {},
    emptyContent: LazyListScope.() -> Unit = {},
    itemContent: LazyListScope.() -> Unit,
) {
    if (LocalInspectionMode.current) {
        StatefulLazyColumnForPreview(
            state = state,
            lazyPagingItems = lazyPagingItems,
            modifier = modifier,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            emptyContent = emptyContent,
            itemContent = itemContent,
        )
    } else {
        val shouldShowLoader =
            loadingVisibilityDecider.shouldShowLoader(lazyPagingItems = lazyPagingItems)

        fun computeStatefulLazyColumnState(): StatefulLazyColumnState =
            when (val refreshLoadState = lazyPagingItems.loadState.refresh) {
                is LoadState.Error -> StatefulLazyColumnState.Error(refreshLoadState.error)
                is LoadState.NotLoading -> {
                    if (lazyPagingItems.isEmpty) StatefulLazyColumnState.Empty
                    else StatefulLazyColumnState.Content
                }

                is LoadState.Loading -> {
                    if (shouldShowLoader) StatefulLazyColumnState.Loading
                    else {
                        if (lazyPagingItems.isEmpty) StatefulLazyColumnState.Empty else StatefulLazyColumnState.Content
                    }
                }
            }

        var statefulLazyColumnState by remember {
            mutableStateOf<StatefulLazyColumnState>(StatefulLazyColumnState.Content)
        }

        LaunchedEffect(
            shouldShowLoader,
            lazyPagingItems.loadState.refresh,
            lazyPagingItems.itemCount,
        ) {
            val refresh = lazyPagingItems.loadState.refresh
            statefulLazyColumnState = if (refresh is LoadState.Loading && !shouldShowLoader) {
                statefulLazyColumnState
            } else {
                computeStatefulLazyColumnState()
            }
        }

        AnimatedContent(
            targetState = statefulLazyColumnState,
            transitionSpec = skeletonToContentTransition(),
            label = "SkeletonToContentFadeThrough",
        ) { statefulLazyColumnState ->
            LazyColumn(
                state = state,
                modifier = modifier,
                contentPadding = contentPadding,
                verticalArrangement = verticalArrangement,
            ) {
                when (statefulLazyColumnState) {
                    StatefulLazyColumnState.Loading -> loadingContent()
                    StatefulLazyColumnState.Empty -> emptyContent()
                    is StatefulLazyColumnState.Error -> errorContent(statefulLazyColumnState.exception)
                    StatefulLazyColumnState.Content -> {
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
                }
            }
        }
    }
}

private sealed interface StatefulLazyColumnState {
    data object Loading : StatefulLazyColumnState
    data object Empty : StatefulLazyColumnState
    data class Error(val exception: Throwable) : StatefulLazyColumnState
    data object Content : StatefulLazyColumnState
}

private fun <Value> skeletonToContentTransition(): AnimatedContentTransitionScope<Value>.() -> ContentTransform {
    return {
        val out = fadeOut(animationSpec = tween(durationMillis = 90))
        val `in` = fadeIn(animationSpec = tween(delayMillis = 90, durationMillis = 210)) +
                scaleIn(
                    initialScale = 0.92f,
                    animationSpec = tween(delayMillis = 90, durationMillis = 210),
                )

        (`in`) togetherWith out using
                SizeTransform(
                    clip = false,
                    sizeAnimationSpec = { _, _ -> tween(durationMillis = 210) },
                )
    }
}

@Composable
private fun <Value : Any> StatefulLazyColumnForPreview(
    state: LazyListState,
    lazyPagingItems: LazyPagingItems<Value>,
    modifier: Modifier,
    contentPadding: PaddingValues,
    verticalArrangement: Arrangement.Vertical,
    emptyContent: LazyListScope.() -> Unit = {},
    itemContent: LazyListScope.() -> Unit,
) {
    LazyColumn(
        state = state,
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

private fun <Value : Any> LazyListScope.prependContent(
    lazyPagingItems: LazyPagingItems<Value>,
    loadingContentOnConcatenate: (LazyListScope.() -> Unit) = {},
    errorContentOnConcatenate: (LazyListScope.() -> Unit) = {},
) {
    if (lazyPagingItems.loadState.prepend is LoadState.Loading) {
        loadingContentOnConcatenate()
    } else if (lazyPagingItems.loadState.prepend is LoadState.Error) {
        errorContentOnConcatenate()
    }
}

private fun <Value : Any> LazyListScope.appendContent(
    lazyPagingItems: LazyPagingItems<Value>,
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

fun <Value : Any> LazyListScope.statefulLazyColumnContentOnConcatenateError(lazyPagingItems: LazyPagingItems<Value>) {
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
