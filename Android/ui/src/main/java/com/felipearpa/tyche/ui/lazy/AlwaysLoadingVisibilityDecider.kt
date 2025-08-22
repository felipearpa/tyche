package com.felipearpa.tyche.ui.lazy

import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems

class AlwaysLoadingVisibilityDecider<Value : Any> : LoadingVisibilityDecider<Value> {
    override fun shouldShowLoader(lazyPagingItems: LazyPagingItems<Value>): Boolean =
        lazyPagingItems.loadState.refresh is LoadState.Loading
}
