package com.felipearpa.tyche.bet.finished

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
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
import com.felipearpa.tyche.bet.poolGamblerBetFinishedDummyModels
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun FinishedBetListView(
    viewModel: FinishedBetListViewModel,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)? = null,
) {
    val lazyItems = viewModel.poolGamblerBets.collectAsLazyPagingItems()
    val pageSize = viewModel.pageSize

    FinishedBetListView(
        lazyGamblerBets = lazyItems,
        placeholderCount = pageSize,
        onMatchOpen = onMatchOpen,
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun FinishedBetListView(
    lazyGamblerBets: LazyPagingItems<PoolGamblerBetModel>,
    placeholderCount: Int,
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)?,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    // The list scrolls under the screen's app bars; its content padding keeps the first and
    // last rows clear of them.
    FinishedBetList(
        lazyPoolGamblerBets = lazyGamblerBets,
        placeholderCount = placeholderCount,
        contentPadding = contentPadding,
        modifier = modifier.consumeWindowInsets(contentPadding),
        onMatchOpen = onMatchOpen,
    )
}

@PreviewLightDark
@Composable
fun FinishedBetListViewPreview(contentPadding: PaddingValues = PaddingValues(0.dp)) {
    val lazyItems = MutableStateFlow(PagingData.from(poolGamblerBetFinishedDummyModels())).collectAsLazyPagingItems()

    TycheTheme {
        Surface {
            FinishedBetListView(
                lazyGamblerBets = lazyItems,
                placeholderCount = 50,
                onMatchOpen = {},
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
