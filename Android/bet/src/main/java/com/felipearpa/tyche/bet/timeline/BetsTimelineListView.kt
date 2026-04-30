package com.felipearpa.tyche.bet.timeline

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
fun BetsTimelineListView(
    viewModel: BetsTimelineViewModel,
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)? = null,
) {
    val lazyItems = viewModel.poolGamblerBets.collectAsLazyPagingItems()
    val pageSize = viewModel.pageSize

    BetsTimelineList(
        lazyBets = lazyItems,
        placeholderCount = pageSize,
        modifier = Modifier.fillMaxSize(),
        onMatchOpen = onMatchOpen,
    )
}

@Composable
private fun BetsTimelineListView(
    lazyBets: LazyPagingItems<PoolGamblerBetModel>,
    placeholderCount: Int,
    modifier: Modifier = Modifier,
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)?,
) {
    BetsTimelineList(
        lazyBets = lazyBets,
        placeholderCount = placeholderCount,
        modifier = modifier,
        onMatchOpen = onMatchOpen,
    )
}

@Preview(showBackground = true)
@Composable
private fun BetsTimelineListViewPreview() {
    val items = MutableStateFlow(PagingData.from(poolGamblerBetDummyModels())).collectAsLazyPagingItems()
    BetsTimelineListView(
        lazyBets = items,
        placeholderCount = 50,
        modifier = Modifier.fillMaxSize(),
        onMatchOpen = {},
    )
}
