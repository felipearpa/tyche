package com.felipearpa.tyche.bet.live

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.poolGamblerBetDummyModels
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun LiveBetListView(viewModel: LiveBetListViewModel) {
    val lazyItems = viewModel.poolGamblerBets.collectAsLazyPagingItems()
    val pageSize = viewModel.pageSize
    LiveBetListView(
        lazyBets = lazyItems,
        placeholderCount = pageSize,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun LiveBetListView(
    lazyBets: LazyPagingItems<PoolGamblerBetModel>,
    placeholderCount: Int,
    modifier: Modifier = Modifier,
) {
    LiveBetList(
        lazyBets = lazyBets,
        placeholderCount = placeholderCount,
        modifier = modifier,
    )
}


@Preview(showBackground = true)
@Composable
private fun LiveBetListViewPreview() {
    val lazyBets = MutableStateFlow(PagingData.from(poolGamblerBetDummyModels())).collectAsLazyPagingItems()
    LiveBetListView(
        lazyBets = lazyBets,
        placeholderCount = 50,
        modifier = Modifier.fillMaxSize(),
    )
}
