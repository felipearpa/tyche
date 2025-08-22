package com.felipearpa.tyche.ui.lazy

import androidx.paging.compose.LazyPagingItems

val <Value : Any> LazyPagingItems<Value>.isEmpty: Boolean
    get() = this.itemCount == 0

val <Value : Any> LazyPagingItems<Value>.isNotEmpty: Boolean
    get() = this.itemCount > 0
