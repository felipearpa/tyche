package com.felipearpa.tyche.ui.lazy

import androidx.compose.runtime.Composable
import androidx.paging.compose.LazyPagingItems

fun interface LoadingVisibilityDecider<Value : Any> {
    @Composable
    fun shouldShowLoader(lazyPagingItems: LazyPagingItems<Value>): Boolean
}