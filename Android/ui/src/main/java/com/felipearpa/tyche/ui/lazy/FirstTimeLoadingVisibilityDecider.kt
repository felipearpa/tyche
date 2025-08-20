package com.felipearpa.tyche.ui.lazy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems

class FirstTimeLoadingVisibilityDecider<Value : Any> : LoadingVisibilityDecider<Value> {
    @Composable
    override fun shouldShowLoader(lazyPagingItems: LazyPagingItems<Value>): Boolean {
        var hasShownLoading by remember { mutableStateOf(false) }
        var isContentLoaded by remember { mutableStateOf(false) }

        if (lazyPagingItems.hasItems()) {
            return false
        }

        LaunchedEffect(lazyPagingItems.loadState.refresh) {
            if (lazyPagingItems.loadState.refresh is LoadState.Loading) {
                hasShownLoading = true
            }

            if (hasShownLoading && (lazyPagingItems.loadState.refresh is LoadState.NotLoading || lazyPagingItems.loadState.refresh is LoadState.Error)) {
                isContentLoaded = true
            }
        }

        return lazyPagingItems.loadState.refresh is LoadState.Loading && !isContentLoaded
    }
}
