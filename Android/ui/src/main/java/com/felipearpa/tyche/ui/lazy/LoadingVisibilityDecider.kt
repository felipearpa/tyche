package com.felipearpa.tyche.ui.lazy

import androidx.paging.compose.LazyPagingItems

fun interface LoadingVisibilityDecider<Value : Any> {
    fun shouldShowLoader(lazyPagingItems: LazyPagingItems<Value>): Boolean
}
