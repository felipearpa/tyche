package com.felipearpa.tyche.bet.pending

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.poolGamblerBetDummyModels
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun PendingBetListView(
    viewModel: PendingBetListViewModel,
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)? = null,
) {
    val lazyItems = viewModel.poolGamblerBets.collectAsLazyPagingItems()
    val pageSize = viewModel.pageSize
    PendingBetListView(
        lazyPoolGamblerBets = lazyItems,
        placeholderCount = pageSize,
        onMatchOpen = onMatchOpen,
        modifier = Modifier
            .fillMaxSize()
            .padding(all = LocalBoxSpacing.current.medium),
    )
}

@Composable
private fun PendingBetListView(
    lazyPoolGamblerBets: LazyPagingItems<PoolGamblerBetModel>,
    placeholderCount: Int,
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    PendingBetList(
        lazyPoolGamblerBets = lazyPoolGamblerBets,
        fakeItemCount = placeholderCount,
        onMatchOpen = onMatchOpen,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun PendingBetListViewPreview() {
    val lazyItems = MutableStateFlow(PagingData.from(poolGamblerBetDummyModels())).collectAsLazyPagingItems()
    PendingBetListView(
        lazyPoolGamblerBets = lazyItems,
        placeholderCount = 50,
        onMatchOpen = {},
        modifier = Modifier
            .fillMaxSize()
            .padding(all = LocalBoxSpacing.current.medium),
    )
}
