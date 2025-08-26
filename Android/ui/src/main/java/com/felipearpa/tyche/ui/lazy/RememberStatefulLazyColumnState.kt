package com.felipearpa.tyche.ui.lazy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems

@Composable
fun <Item : Any> rememberStatefulLazyColumnState(lazyPagingItems: LazyPagingItems<Item>): StatefulLazyColumnState {
    val statefulLazyColumnState by produceState<StatefulLazyColumnState>(
        initialValue = StatefulLazyColumnState.Content,
        lazyPagingItems.loadState.refresh,
    ) {
        val currentStatefulLazyColumnState = value

        value = when (val refresh = lazyPagingItems.loadState.refresh) {
            is LoadState.Error ->
                StatefulLazyColumnState.Error(refresh.error)

            is LoadState.NotLoading ->
                if (
                    lazyPagingItems.loadState.prepend.endOfPaginationReached
                    && lazyPagingItems.loadState.append.endOfPaginationReached
                    && lazyPagingItems.isEmpty
                ) {
                    StatefulLazyColumnState.Empty
                } else {
                    if (lazyPagingItems.isNotEmpty)
                        StatefulLazyColumnState.Content
                    else
                        StatefulLazyColumnState.Loading
                }

            is LoadState.Loading ->
                if (currentStatefulLazyColumnState !is StatefulLazyColumnState.Loading)
                    currentStatefulLazyColumnState
                else
                    StatefulLazyColumnState.Loading
        }
    }

    return statefulLazyColumnState
}
