package com.felipearpa.tyche.bet

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.compose.collectAsLazyPagingItems

@Composable
fun PoolGamblerBetListView(viewModel: PoolGamblerBetListViewModel) {
    val lazyItems = viewModel.poolGamblerBets.collectAsLazyPagingItems()
    val pageSize = viewModel.pageSize

    PoolGamblerBetList(
        lazyPoolGamblerBets = lazyItems,
        fakeItemCount = pageSize,
        modifier = Modifier.fillMaxSize()
    )
}