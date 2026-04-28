package com.felipearpa.tyche.bet.pending

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.bet.PoolGamblerBetModel

@Composable
fun PendingBetListView(
    viewModel: PendingBetListViewModel,
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)? = null,
) {
    val lazyItems = viewModel.poolGamblerBets.collectAsLazyPagingItems()
    val pageSize = viewModel.pageSize

    PendingBetList(
        lazyPoolGamblerBets = lazyItems,
        fakeItemCount = pageSize,
        modifier = Modifier.fillMaxSize(),
        onMatchOpen = onMatchOpen,
    )
}
