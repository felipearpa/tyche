package com.felipearpa.tyche.ui.lazy

import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems

class FirstTimeLoadingVisibilityDecider<Value : Any> : LoadingVisibilityDecider<Value> {
    var hasShownLoading = false
    var isContentLoaded = false

    override fun shouldShowLoader(lazyPagingItems: LazyPagingItems<Value>): Boolean {
        if (lazyPagingItems.isNotEmpty) {
            return false
        }

        if (lazyPagingItems.loadState.refresh is LoadState.Loading) {
            hasShownLoading = true
        }

        when (lazyPagingItems.loadState.refresh) {
            is LoadState.NotLoading, is LoadState.Error ->
                if (hasShownLoading) {
                    isContentLoaded = true
                }

            else -> {}
        }

        return lazyPagingItems.loadState.refresh is LoadState.Loading && !isContentLoaded
    }
}
