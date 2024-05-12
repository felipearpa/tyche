package com.felipearpa.tyche.ui.lazy

import androidx.compose.runtime.Composable
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems

class AlwaysLoadingVisibilityDecider<Value : Any> : LoadingVisibilityDecider<Value> {
    @Composable
    override fun shouldShowLoader(lazyPagingItems: LazyPagingItems<Value>): Boolean {
        return lazyPagingItems.loadState.refresh is LoadState.Loading
    }
}