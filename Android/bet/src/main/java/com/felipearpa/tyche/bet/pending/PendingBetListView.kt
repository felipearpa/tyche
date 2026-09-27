package com.felipearpa.tyche.bet.pending

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.poolGamblerBetPendingDummyModels
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun PendingBetListView(
    viewModel: PendingBetListViewModel,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)? = null,
) {
    val lazyItems = viewModel.poolGamblerBets.collectAsLazyPagingItems()
    val pageSize = viewModel.pageSize
    PendingBetListView(
        lazyPoolGamblerBets = lazyItems,
        placeholderCount = pageSize,
        onMatchOpen = onMatchOpen,
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun PendingBetListView(
    lazyPoolGamblerBets: LazyPagingItems<PoolGamblerBetModel>,
    placeholderCount: Int,
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)?,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    PendingBetList(
        lazyPoolGamblerBets = lazyPoolGamblerBets,
        fakeItemCount = placeholderCount,
        onMatchOpen = onMatchOpen,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@PreviewLightDark
@Composable
fun PendingBetListViewPreview(contentPadding: PaddingValues = PaddingValues(0.dp)) {
    val lazyItems = MutableStateFlow(PagingData.from(poolGamblerBetPendingDummyModels())).collectAsLazyPagingItems()

    TycheTheme {
        Surface {
            PendingBetListView(
                lazyPoolGamblerBets = lazyItems,
                placeholderCount = 50,
                onMatchOpen = {},
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
