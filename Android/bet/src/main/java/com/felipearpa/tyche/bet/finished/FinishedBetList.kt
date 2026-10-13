package com.felipearpa.tyche.bet.finished

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import com.felipearpa.tyche.bet.kickoffgroup.KickoffGroupModel
import com.felipearpa.tyche.bet.kickoffgroup.historyGroupPreviewModels
import com.felipearpa.tyche.bet.kickoffgroup.kickoffGroupItems
import com.felipearpa.tyche.bet.kickoffgroup.kickoffGroupPlaceholderList
import com.felipearpa.tyche.bet.kickoffgroup.KickoffGroupPlaceholderSection
import com.felipearpa.tyche.bet.kickoffgroup.kickoffGroupAppendError
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.lazy.RefreshableLazyPagingColumn
import com.felipearpa.tyche.ui.lazy.lazyPagingColumnEmpty
import com.felipearpa.tyche.ui.lazy.lazyPagingColumnError
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The signed-in gambler's History: the earned-points summary followed by one kickoff group per
 * scheduled kickoff, newest first, in the server's order. Every list state (initial loading,
 * loaded, empty, failed) starts with the same summary header, so a known total survives list
 * changes. Group headers scroll with the rows; none is sticky.
 *
 * One pull restarts the groups from their first page and calls [onRefresh] for the total; the
 * pull indicator stays while the total's pull request is pending. The list error's Retry, and the
 * Retry shown after the server rejected the continuation twice, call [onFeedRestart] and reload
 * only the groups; any other failed page is repeated.
 */
@Composable
fun FinishedBetList(
    lazyKickoffGroups: LazyPagingItems<KickoffGroupModel>,
    pointsSummary: HistoryPointsSummaryState,
    onPointsSummaryRetry: () -> Unit,
    onRefresh: () -> Unit,
    onFeedRestart: () -> Unit,
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
    val restart = {
        onFeedRestart()
        lazyKickoffGroups.refresh()
    }

    RefreshableLazyPagingColumn(
        modifier = modifier,
        lazyPagingItems = lazyKickoffGroups,
        contentPadding = contentPadding,
        onRefresh = onRefresh,
        isCompanionRefreshing = pointsSummary.isPullRefreshing,
        loadingContent = {
            header()
            kickoffGroupPlaceholderList(count = placeholderCount, dateFormat = dateFormat)
        },
        emptyContent = {
            header()
            lazyPagingColumnEmpty()
        },
        errorContent = { exception ->
            header()
            // Reloads only the groups; the total keeps its own retry.
            lazyPagingColumnError(exception = exception, onRetry = restart)
        },
        appendLoadingContent = { item { KickoffGroupPlaceholderSection(dateFormat = dateFormat) } },
        appendErrorContent = { exception ->
            kickoffGroupAppendError(exception = exception, onRetry = lazyKickoffGroups::retry, onRestart = restart)
        },
    ) {
        header()
        kickoffGroupItems(
            lazyKickoffGroups = lazyKickoffGroups,
            dateFormat = dateFormat,
            onMatchOpen = onMatchOpen,
        )
    }
}

private const val POINTS_SUMMARY_KEY = "historyPointsSummary"

@PreviewLightDark
@Preview(name = "Large text, Spanish", locale = "es-rCO", fontScale = 2f)
@Composable
private fun FinishedBetListPreview() {
    val items = MutableStateFlow(PagingData.from(historyGroupPreviewModels())).collectAsLazyPagingItems()
    TycheTheme {
        Surface {
            FinishedBetList(
                lazyKickoffGroups = items,
                pointsSummary = HistoryPointsSummaryState.initial.loaded(HistoryPoints.Earned(120)),
                onPointsSummaryRetry = {},
                onRefresh = {},
                onFeedRestart = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun FinishedBetListLoadingPreview() {
    val loading = LoadStates(
        refresh = LoadState.Loading,
        prepend = LoadState.NotLoading(endOfPaginationReached = false),
        append = LoadState.NotLoading(endOfPaginationReached = false),
    )
    val items = MutableStateFlow(PagingData.empty<KickoffGroupModel>(sourceLoadStates = loading))
        .collectAsLazyPagingItems()
    TycheTheme {
        Surface {
            FinishedBetList(
                lazyKickoffGroups = items,
                pointsSummary = HistoryPointsSummaryState.initial,
                onPointsSummaryRetry = {},
                onRefresh = {},
                onFeedRestart = {},
                placeholderCount = 3,
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
        PagingData.empty<KickoffGroupModel>(sourceLoadStates = failed),
    ).collectAsLazyPagingItems()
    TycheTheme {
        Surface {
            FinishedBetList(
                lazyKickoffGroups = items,
                pointsSummary = HistoryPointsSummaryState.initial.loaded(HistoryPoints.Earned(4)),
                onPointsSummaryRetry = {},
                onRefresh = {},
                onFeedRestart = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
