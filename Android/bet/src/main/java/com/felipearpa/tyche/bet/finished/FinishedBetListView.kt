package com.felipearpa.tyche.bet.finished

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.poolGamblerBetFinishedDummyModels
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun FinishedBetListView(
    viewModel: FinishedBetListViewModel,
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)? = null,
) {
    val lazyItems = viewModel.poolGamblerBets.collectAsLazyPagingItems()
    val pageSize = viewModel.pageSize

    FinishedBetListView(
        lazyGamblerBets = lazyItems,
        placeholderCount = pageSize,
        onMatchOpen = onMatchOpen,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun FinishedBetListView(
    lazyGamblerBets: LazyPagingItems<PoolGamblerBetModel>,
    placeholderCount: Int,
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    FinishedBetList(
        lazyPoolGamblerBets = lazyGamblerBets,
        placeholderCount = placeholderCount,
        modifier = modifier,
        onMatchOpen = onMatchOpen,
    )
}

@PreviewLightDark
@Composable
fun FinishedBetListViewPreview() {
    val lazyItems = MutableStateFlow(PagingData.from(poolGamblerBetFinishedDummyModels())).collectAsLazyPagingItems()

    TycheTheme {
        Surface {
            FinishedBetListView(
                lazyGamblerBets = lazyItems,
                placeholderCount = 50,
                onMatchOpen = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
