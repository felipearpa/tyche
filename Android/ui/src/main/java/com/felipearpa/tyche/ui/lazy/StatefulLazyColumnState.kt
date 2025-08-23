package com.felipearpa.tyche.ui.lazy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems

sealed interface StatefulLazyColumnState {
    data object Initial : StatefulLazyColumnState
    data object Loading : StatefulLazyColumnState
    data object Empty : StatefulLazyColumnState
    data class Error(val exception: Throwable) : StatefulLazyColumnState
    data object Content : StatefulLazyColumnState
}

@Composable
fun <T : Any> rememberStatefulLazyColumnState(
    lazyPagingItems: LazyPagingItems<T>,
): StatefulLazyColumnState {
    val refresh = lazyPagingItems.loadState.refresh
    val isEmpty = lazyPagingItems.isEmpty

    val state by produceState<StatefulLazyColumnState>(
        initialValue = StatefulLazyColumnState.Initial,
        refresh,
    ) {
        val previousState = value
        value = when (refresh) {
            is LoadState.Error -> StatefulLazyColumnState.Error(refresh.error)
            is LoadState.NotLoading ->
                if (previousState is StatefulLazyColumnState.Initial) StatefulLazyColumnState.Loading
                else {
                    if (isEmpty) StatefulLazyColumnState.Empty
                    else StatefulLazyColumnState.Content
                }

            is LoadState.Loading ->
                if (previousState is StatefulLazyColumnState.Initial) StatefulLazyColumnState.Loading
                else previousState
        }
    }
    return state
}
