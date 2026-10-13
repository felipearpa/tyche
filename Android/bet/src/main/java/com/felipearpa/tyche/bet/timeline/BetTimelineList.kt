package com.felipearpa.tyche.bet.timeline

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
import com.felipearpa.tyche.bet.finished.HistoryOwner
import com.felipearpa.tyche.bet.finished.HistoryPoints
import com.felipearpa.tyche.bet.finished.HistoryPointsHeader
import com.felipearpa.tyche.bet.finished.HistoryPointsSummaryState
import com.felipearpa.tyche.bet.finished.rememberHistoryMatchDateFormat
import com.felipearpa.tyche.bet.kickoffgroup.KickoffGroupModel
import com.felipearpa.tyche.bet.kickoffgroup.KickoffGroupPlaceholderSection
import com.felipearpa.tyche.bet.kickoffgroup.kickoffGroupAppendError
import com.felipearpa.tyche.bet.kickoffgroup.kickoffGroupItems
import com.felipearpa.tyche.bet.kickoffgroup.kickoffGroupPlaceholderList
import com.felipearpa.tyche.bet.kickoffgroup.timelineGroupPreviewModels
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.lazy.RefreshableLazyPagingColumn
import com.felipearpa.tyche.ui.lazy.lazyPagingColumnEmpty
import com.felipearpa.tyche.ui.lazy.lazyPagingColumnError
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Another gambler's Timeline: their identity and earned-points total, then one shared History
 * kickoff group per scheduled kickoff in the feed's order, unsettled groups first. Identity, total,
 * and group headers scroll with the rows, and every list state (initial loading, loaded, empty,
 * failed) starts with identity and total, so they stay usable whatever happens to the groups.
 *
 * One pull restarts the groups from their first page and calls [onRefresh] for the total; the pull
 * indicator stays while the total's pull request is pending. The list error's Retry, and the Retry
 * shown after the server rejected the continuation twice, call [onFeedRestart] and reload only the
 * groups; any other failed page is repeated.
 */
@Composable
fun BetTimelineList(
    gamblerId: String,
    gamblerUsername: String,
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
    val owner = HistoryOwner.SelectedGambler(name = gamblerUsername)
    val header: LazyListScope.() -> Unit = {
        item(key = IDENTITY_KEY, contentType = IDENTITY_KEY) {
            TimelineGamblerHeading(gamblerId = gamblerId, gamblerUsername = gamblerUsername)
        }
        item(key = POINTS_SUMMARY_KEY, contentType = POINTS_SUMMARY_KEY) {
            HistoryPointsHeader(state = pointsSummary, onRetry = onPointsSummaryRetry, owner = owner)
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
            kickoffGroupPlaceholderList(count = placeholderCount, dateFormat = dateFormat, owner = owner)
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
        appendLoadingContent = { item { KickoffGroupPlaceholderSection(dateFormat = dateFormat, owner = owner) } },
        appendErrorContent = { exception ->
            kickoffGroupAppendError(exception = exception, onRetry = lazyKickoffGroups::retry, onRestart = restart)
        },
    ) {
        header()
        kickoffGroupItems(
            lazyKickoffGroups = lazyKickoffGroups,
            dateFormat = dateFormat,
            owner = owner,
            onMatchOpen = onMatchOpen,
        )
    }
}

private const val IDENTITY_KEY = "timelineGamblerIdentity"
private const val POINTS_SUMMARY_KEY = "timelinePointsSummary"

@PreviewLightDark
@Preview(locale = "es-rCO", fontScale = 2f)
@Composable
private fun BetTimelineListPreview() {
    val items = MutableStateFlow(PagingData.from(timelineGroupPreviewModels())).collectAsLazyPagingItems()
    TycheTheme {
        Surface {
            BetTimelineList(
                gamblerId = "",
                gamblerUsername = "El mono",
                lazyKickoffGroups = items,
                pointsSummary = HistoryPointsSummaryState.initial.loaded(HistoryPoints.Earned(676)),
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
private fun BetTimelineListRowsFailedPreview() {
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
            BetTimelineList(
                gamblerId = "",
                gamblerUsername = "El mono",
                lazyKickoffGroups = items,
                pointsSummary = HistoryPointsSummaryState.initial.loaded(HistoryPoints.Earned(676)),
                onPointsSummaryRetry = {},
                onRefresh = {},
                onFeedRestart = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
