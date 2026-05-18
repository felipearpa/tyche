package com.felipearpa.tyche.bet.pending

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.poolGamblerBetPendingDummyModels
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme
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
            .padding(vertical = LocalBoxSpacing.current.medium),
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

@PreviewLightDark
@Composable
fun PendingBetListViewPreview() {
    val lazyItems = MutableStateFlow(PagingData.from(poolGamblerBetPendingDummyModels())).collectAsLazyPagingItems()

    TycheTheme {
        Surface {
            PendingBetListView(
                lazyPoolGamblerBets = lazyItems,
                placeholderCount = 50,
                onMatchOpen = {},
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = LocalBoxSpacing.current.medium),
            )
        }
    }
}
