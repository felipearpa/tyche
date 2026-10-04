package com.felipearpa.tyche.bet.timeline

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.finished.HISTORY_ROW_CONTENT_TYPE
import com.felipearpa.tyche.bet.finished.HistoryBetItem
import com.felipearpa.tyche.bet.finished.HistoryBetPlaceholderRow
import com.felipearpa.tyche.bet.finished.HistoryOwner
import com.felipearpa.tyche.bet.finished.HistoryPoints
import com.felipearpa.tyche.bet.finished.HistoryPointsHeader
import com.felipearpa.tyche.bet.finished.HistoryPointsSummaryState
import com.felipearpa.tyche.bet.finished.HistoryRowDivider
import com.felipearpa.tyche.bet.finished.historyBetPlaceholderList
import com.felipearpa.tyche.bet.finished.historyRowKey
import com.felipearpa.tyche.bet.finished.rememberHistoryMatchDateFormat
import com.felipearpa.tyche.bet.timelineBetPreviewModels
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.lazy.RefreshableLazyPagingColumn
import com.felipearpa.tyche.ui.lazy.lazyPagingColumnEmpty
import com.felipearpa.tyche.ui.lazy.lazyPagingColumnError
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Another gambler's Timeline: their identity and earned-points total, then one shared History row
 * per entry in the feed's order, pending entries included. Identity and total scroll with the
 * rows, and every list state (initial loading, loaded, empty, failed) starts with them, so they
 * stay usable whatever happens to the rows. Rows carry their own date, so there are no sticky date
 * headers.
 *
 * One pull refreshes the rows and calls [onRefresh] for the total; the pull indicator stays while
 * the total's pull request is pending. The list error's Retry reloads only the rows.
 */
@Composable
fun BetTimelineList(
    gamblerId: String,
    gamblerUsername: String,
    lazyBets: LazyPagingItems<PoolGamblerBetModel>,
    pointsSummary: HistoryPointsSummaryState,
    onPointsSummaryRetry: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    placeholderCount: Int = 0,
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)? = null,
) {
    val dateFormat = rememberHistoryMatchDateFormat()
    val owner = HistoryOwner.SelectedGambler(name = gamblerUsername)
    val header: LazyListScope.() -> Unit = {
        item(key = IDENTITY_KEY, contentType = IDENTITY_KEY) {
            TimelineGamblerHeading(gamblerId = gamblerId, gamblerUsername = gamblerUsername)
        }
        item(key = POINTS_SUMMARY_KEY, contentType = POINTS_SUMMARY_KEY) {
            HistoryPointsHeader(state = pointsSummary, onRetry = onPointsSummaryRetry, owner = owner)
        }
    }

    RefreshableLazyPagingColumn(
        modifier = modifier,
        lazyPagingItems = lazyBets,
        contentPadding = contentPadding,
        onRefresh = onRefresh,
        isCompanionRefreshing = pointsSummary.isPullRefreshing,
        loadingContent = {
            header()
            historyBetPlaceholderList(count = placeholderCount, dateFormat = dateFormat, owner = owner)
        },
        emptyContent = {
            header()
            lazyPagingColumnEmpty()
        },
        errorContent = { exception ->
            header()
            // Reloads only the rows; the total keeps its own retry.
            lazyPagingColumnError(exception = exception, onRetry = lazyBets::refresh)
        },
        appendLoadingContent = { item { HistoryBetPlaceholderRow(dateFormat = dateFormat, owner = owner) } },
    ) {
        header()
        items(
            count = lazyBets.itemCount,
            key = { index ->
                val poolGamblerBet = lazyBets.peek(index)
                if (poolGamblerBet == null) index else historyRowKey(poolGamblerBet)
            },
            contentType = { HISTORY_ROW_CONTENT_TYPE },
        ) { index ->
            val poolGamblerBet = lazyBets[index] ?: return@items
            HistoryBetItem(
                poolGamblerBet = poolGamblerBet,
                dateFormat = dateFormat,
                modifier = Modifier.fillMaxWidth(),
                owner = owner,
                onClick = onMatchOpen?.let { open -> { open(poolGamblerBet) } },
            )
            HistoryRowDivider()
        }
    }
}

private const val IDENTITY_KEY = "timelineGamblerIdentity"
private const val POINTS_SUMMARY_KEY = "timelinePointsSummary"

@PreviewLightDark
@Preview(locale = "es-rCO", fontScale = 2f)
@Composable
private fun BetTimelineListPreview() {
    val items = MutableStateFlow(PagingData.from(timelineBetPreviewModels())).collectAsLazyPagingItems()
    TycheTheme {
        Surface {
            BetTimelineList(
                gamblerId = "",
                gamblerUsername = "El mono",
                lazyBets = items,
                pointsSummary = HistoryPointsSummaryState.initial.loaded(HistoryPoints.Earned(676)),
                onPointsSummaryRetry = {},
                onRefresh = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun BetTimelineListRowsFailedPreview() {
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
            BetTimelineList(
                gamblerId = "",
                gamblerUsername = "El mono",
                lazyBets = items,
                pointsSummary = HistoryPointsSummaryState.initial.loaded(HistoryPoints.Earned(676)),
                onPointsSummaryRetry = {},
                onRefresh = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
