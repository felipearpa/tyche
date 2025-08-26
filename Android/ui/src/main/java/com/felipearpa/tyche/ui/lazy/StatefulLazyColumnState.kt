package com.felipearpa.tyche.ui.lazy

sealed interface StatefulLazyColumnState {
    data object Loading : StatefulLazyColumnState

    data object Empty : StatefulLazyColumnState

    data class Error(val exception: Throwable) : StatefulLazyColumnState

    data object Content : StatefulLazyColumnState
}
