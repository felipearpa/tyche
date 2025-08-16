package com.felipearpa.tyche.bet.finished

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.compose.collectAsLazyPagingItems

@Composable
fun FinishedBetListView(viewModel: FinishedBetListViewModel) {
    val lazyItems = viewModel.poolGamblerBets.collectAsLazyPagingItems()
    val pageSize = viewModel.pageSize

    FinishedBetList(
        lazyPoolGamblerBets = lazyItems,
        fakeItemCount = pageSize,
        modifier = Modifier.fillMaxSize()
    )
}