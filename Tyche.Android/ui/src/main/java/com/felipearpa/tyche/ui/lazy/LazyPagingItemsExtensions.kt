package com.felipearpa.tyche.ui.lazy

import androidx.paging.compose.LazyPagingItems

fun <Value : Any> LazyPagingItems<Value>.hasItems() = this.itemCount > 0

fun <Value : Any> LazyPagingItems<Value>.isPendingLoad() =
    !(this.loadState.prepend.endOfPaginationReached && this.loadState.append.endOfPaginationReached)