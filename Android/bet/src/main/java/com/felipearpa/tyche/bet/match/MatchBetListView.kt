package com.felipearpa.tyche.bet.match

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.compose.collectAsLazyPagingItems

@Composable
fun MatchBetListView(viewModel: MatchBetListViewModel) {
    val lazyItems = viewModel.poolGamblerBets.collectAsLazyPagingItems()
    val pageSize = viewModel.pageSize

    MatchBetList(
        lazyPoolGamblerBets = lazyItems,
        fakeItemCount = pageSize,
        modifier = Modifier.fillMaxSize(),
    )
}
