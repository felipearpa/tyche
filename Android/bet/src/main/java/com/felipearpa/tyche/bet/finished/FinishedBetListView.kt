package com.felipearpa.tyche.bet.finished

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.kickoffgroup.KickoffGroupModel
import com.felipearpa.tyche.bet.kickoffgroup.historyGroupPreviewModels
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun FinishedBetListView(
    viewModel: FinishedBetListViewModel,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)? = null,
) {
    val lazyItems = viewModel.kickoffGroups.collectAsLazyPagingItems()
    val pointsSummary by viewModel.pointsSummary.collectAsState()

    FinishedBetListView(
        lazyKickoffGroups = lazyItems,
        pointsSummary = pointsSummary,
        onPointsSummaryRetry = viewModel::loadPointsSummary,
        onRefresh = viewModel::refresh,
        onFeedRestart = viewModel::onFeedRestart,
        placeholderCount = viewModel.placeholderCount,
        onMatchOpen = onMatchOpen,
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun FinishedBetListView(
    lazyKickoffGroups: LazyPagingItems<KickoffGroupModel>,
    pointsSummary: HistoryPointsSummaryState,
    onPointsSummaryRetry: () -> Unit,
    onRefresh: () -> Unit,
    onFeedRestart: () -> Unit,
    placeholderCount: Int,
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)?,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    // The list scrolls under the screen's app bars; its content padding keeps the first and
    // last rows clear of them.
    FinishedBetList(
        lazyKickoffGroups = lazyKickoffGroups,
        pointsSummary = pointsSummary,
        onPointsSummaryRetry = onPointsSummaryRetry,
        onRefresh = onRefresh,
        onFeedRestart = onFeedRestart,
        placeholderCount = placeholderCount,
        contentPadding = contentPadding,
        modifier = modifier.consumeWindowInsets(contentPadding),
        onMatchOpen = onMatchOpen,
    )
}

@PreviewLightDark
@Composable
fun FinishedBetListViewPreview(contentPadding: PaddingValues = PaddingValues(0.dp)) {
    val lazyItems = MutableStateFlow(PagingData.from(historyGroupPreviewModels())).collectAsLazyPagingItems()

    TycheTheme {
        Surface {
            FinishedBetListView(
                lazyKickoffGroups = lazyItems,
                pointsSummary = HistoryPointsSummaryState.initial.loaded(HistoryPoints.Earned(4)),
                onPointsSummaryRetry = {},
                onRefresh = {},
                onFeedRestart = {},
                placeholderCount = 4,
                onMatchOpen = {},
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
