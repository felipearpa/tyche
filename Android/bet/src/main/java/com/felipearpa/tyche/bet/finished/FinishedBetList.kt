package com.felipearpa.tyche.bet.finished

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.historyBetPlaceholderModel
import com.felipearpa.tyche.bet.historyBetPreviewModels
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.lazy.RefreshableLazyPagingColumn
import com.felipearpa.tyche.ui.lazy.lazyPagingColumnEmpty
import com.felipearpa.tyche.ui.lazy.lazyPagingColumnError
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The signed-in gambler's History: the earned-points summary followed by one row per finished
 * match, newest first. Every list state (initial loading, loaded, empty, failed) starts with the
 * same summary header, so a known total survives list changes. Rows carry their own date, so
 * there are no sticky date headers.
 *
 * One pull refreshes the rows and calls [onRefresh] for the total; the pull indicator stays
 * while the total's pull request is pending.
 */
@Composable
fun FinishedBetList(
    lazyPoolGamblerBets: LazyPagingItems<PoolGamblerBetModel>,
    pointsSummary: HistoryPointsSummaryState,
    onPointsSummaryRetry: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    placeholderCount: Int = 0,
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)? = null,
) {
    val dateFormat = rememberHistoryMatchDateFormat()
    val header: LazyListScope.() -> Unit = {
        item(key = POINTS_SUMMARY_KEY, contentType = POINTS_SUMMARY_KEY) {
            HistoryPointsHeader(state = pointsSummary, onRetry = onPointsSummaryRetry)
        }
    }

    RefreshableLazyPagingColumn(
        modifier = modifier,
        lazyPagingItems = lazyPoolGamblerBets,
        contentPadding = contentPadding,
        onRefresh = onRefresh,
        isCompanionRefreshing = pointsSummary.isPullRefreshing,
        loadingContent = {
            header()
            historyBetPlaceholderList(count = placeholderCount, dateFormat = dateFormat)
        },
        emptyContent = {
            header()
            lazyPagingColumnEmpty()
        },
        errorContent = { exception ->
            header()
            lazyPagingColumnError(exception)
        },
        appendLoadingContent = { item { HistoryBetPlaceholderRow(dateFormat = dateFormat) } },
    ) {
        header()
        items(
            count = lazyPoolGamblerBets.itemCount,
            key = { index ->
                val poolGamblerBet = lazyPoolGamblerBets.peek(index)
                if (poolGamblerBet == null) index else historyRowKey(poolGamblerBet)
            },
            contentType = { HISTORY_ROW_CONTENT_TYPE },
        ) { index ->
            val poolGamblerBet = lazyPoolGamblerBets[index] ?: return@items
            HistoryBetItem(
                poolGamblerBet = poolGamblerBet,
                dateFormat = dateFormat,
                modifier = Modifier.fillMaxWidth(),
                onClick = onMatchOpen?.let { open -> { open(poolGamblerBet) } },
            )
            HistoryRowDivider()
        }
    }
}

private fun historyRowKey(poolGamblerBet: PoolGamblerBetModel) =
    Triple(poolGamblerBet.poolId, poolGamblerBet.gamblerId, poolGamblerBet.matchId)

private fun LazyListScope.historyBetPlaceholderList(count: Int, dateFormat: HistoryMatchDateFormat) {
    items(count = count, contentType = { HISTORY_ROW_CONTENT_TYPE }) {
        HistoryBetPlaceholderRow(dateFormat = dateFormat)
    }
}

@Composable
private fun HistoryBetPlaceholderRow(dateFormat: HistoryMatchDateFormat) {
    HistoryBetItem(
        poolGamblerBet = historyBetPlaceholderModel(),
        dateFormat = dateFormat,
        modifier = Modifier.fillMaxWidth(),
        isPlaceholder = true,
    )
    HistoryRowDivider()
}

private const val POINTS_SUMMARY_KEY = "historyPointsSummary"
private const val HISTORY_ROW_CONTENT_TYPE = "HistoryBet"

@PreviewLightDark
@Composable
private fun FinishedBetListPreview() {
    val items = MutableStateFlow(PagingData.from(historyBetPreviewModels())).collectAsLazyPagingItems()
    TycheTheme {
        Surface {
            FinishedBetList(
                lazyPoolGamblerBets = items,
                pointsSummary = HistoryPointsSummaryState.initial.loaded(HistoryPoints.Earned(120)),
                onPointsSummaryRetry = {},
                onRefresh = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun FinishedBetListSummaryFailedPreview() {
    val items = MutableStateFlow(PagingData.from(historyBetPreviewModels())).collectAsLazyPagingItems()
    TycheTheme {
        Surface {
            FinishedBetList(
                lazyPoolGamblerBets = items,
                pointsSummary = HistoryPointsSummaryState.initial.failed(),
                onPointsSummaryRetry = {},
                onRefresh = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun FinishedBetListRowsFailedPreview() {
    val failed = LoadStates(
        refresh = LoadState.Error(UnknownLocalizedException()),
        prepend = LoadState.NotLoading(endOfPaginationReached = false),
        append = LoadState.NotLoading(endOfPaginationReached = false),
    )
    val items = MutableStateFlow(
        PagingData.empty<PoolGamblerBetModel>(sourceLoadStates = failed),
    ).collectAsLazyPagingItems()
    TycheTheme {
        Surface {
            FinishedBetList(
                lazyPoolGamblerBets = items,
                pointsSummary = HistoryPointsSummaryState.initial.loaded(HistoryPoints.Earned(4)),
                onPointsSummaryRetry = {},
                onRefresh = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
