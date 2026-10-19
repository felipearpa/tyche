package com.felipearpa.tyche.bet.pending

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.poolGamblerBetPendingDummyModels
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * The Bets tab backed by [viewModel]. The minute clock that drives the lock notes and the day
 * headers runs while this screen is composed and stops with it.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun PendingBetListView(
    viewModel: PendingBetListViewModel,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)? = null,
) {
    val lazyItems = viewModel.poolGamblerBets.collectAsLazyPagingItems()
    val listState by viewModel.listState.collectAsState()
    val now by remember { minuteTicks(Clock.System) }.collectAsState(initial = Clock.System.now())
    PendingBetList(
        lazyPoolGamblerBets = lazyItems,
        listState = listState,
        now = now,
        onSelectFilter = viewModel::selectFilter,
        fakeItemCount = viewModel.pageSize,
        rowHolder = viewModel::rowHolder,
        onMatchOpen = onMatchOpen,
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize(),
    )
}

@OptIn(ExperimentalTime::class)
@PreviewLightDark
@Composable
fun PendingBetListViewPreview(contentPadding: PaddingValues = PaddingValues(0.dp)) {
    val lazyItems = MutableStateFlow(PagingData.from(poolGamblerBetPendingDummyModels())).collectAsLazyPagingItems()

    TycheTheme {
        Surface {
            PendingBetList(
                lazyPoolGamblerBets = lazyItems,
                listState = PendingBetListState.INITIAL,
                now = Clock.System.now(),
                onSelectFilter = {},
                fakeItemCount = 50,
                onMatchOpen = {},
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
