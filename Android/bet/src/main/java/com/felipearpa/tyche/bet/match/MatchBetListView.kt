package com.felipearpa.tyche.bet.match

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
fun MatchBetListView(viewModel: MatchBetListViewModel) {
    val lazyItems = viewModel.poolGamblerBets.collectAsLazyPagingItems()
    val pageSize = viewModel.pageSize

    MatchBetListView(
        lazyGamblerBets = lazyItems,
        placeholderCount = pageSize,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun MatchBetListView(
    lazyGamblerBets: LazyPagingItems<PoolGamblerBetModel>,
    placeholderCount: Int,
    modifier: Modifier = Modifier,
) {
    MatchBetList(
        lazyPoolGamblerBets = lazyGamblerBets,
        placeholderCount = placeholderCount,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun MatchBetListViewPreview() {
    val items = MutableStateFlow(PagingData.from(poolGamblerBetDummyModels())).collectAsLazyPagingItems()
    MatchBetListView(
        lazyGamblerBets = items,
        placeholderCount = 50,
        modifier = Modifier.fillMaxSize(),
    )
}
