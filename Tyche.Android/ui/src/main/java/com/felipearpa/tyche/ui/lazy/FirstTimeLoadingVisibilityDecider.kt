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
        var isContentLoaded by remember { mutableStateOf(false) }

        LaunchedEffect(lazyPagingItems.loadState.refresh) {
            if (lazyPagingItems.loadState.refresh is LoadState.NotLoading || lazyPagingItems.loadState.refresh is LoadState.Error) {
                isContentLoaded = true
            }
        }

        return lazyPagingItems.loadState.refresh is LoadState.Loading && !isContentLoaded
    }
}