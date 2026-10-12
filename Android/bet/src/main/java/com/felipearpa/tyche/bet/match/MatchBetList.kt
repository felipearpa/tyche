package com.felipearpa.tyche.bet.match

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
import com.felipearpa.tyche.bet.MATCH_PREVIEW_VIEWER_ID
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.finished.HistoryRowDivider
import com.felipearpa.tyche.bet.historyBetPreviewModels
import com.felipearpa.tyche.bet.matchGamblerBetPlaceholderModel
import com.felipearpa.tyche.bet.matchParticipantPreviewModels
import com.felipearpa.tyche.data.bet.domain.RankedMatchBetsException
import com.felipearpa.tyche.ui.lazy.RefreshableLazyPagingColumn
import com.felipearpa.tyche.ui.lazy.lazyPagingColumnEmpty
import com.felipearpa.tyche.ui.lazy.lazyPagingColumnError
import com.felipearpa.tyche.ui.lazy.lazyPagingConcatenateError
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The revealed match screen: the heading followed by one row per participant in the server's
 * pool-total order. The heading scrolls with the rows and starts every list state (initial
 * loading, loaded, empty, failed), so it stays usable whatever happens to the rows.
 *
 * Another participant's row opens their Timeline through [onGamblerOpen]. The signed-in gambler's
 * row and loading rows have no action.
 *
 * One pull restarts the rows from their first page and calls [onRefresh], which rechecks the
 * match; the pull indicator stays while that check runs ([isHeadingPullRefreshing]). Retrying
 * after a failed first page, or after the server rejected the continuation, calls
 * [onFeedRestart] and restarts from the first page; any other failed page is repeated.
 */
@Composable
fun MatchBetList(
    lazyPoolGamblerBets: LazyPagingItems<PoolGamblerBetModel>,
    viewerAccountId: String,
    isComputed: Boolean,
    onRefresh: () -> Unit,
    onFeedRestart: () -> Unit,
    header: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    isHeadingPullRefreshing: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    placeholderCount: Int = 0,
    onGamblerOpen: ((poolId: String, gamblerId: String, gamblerUsername: String) -> Unit)? = null,
) {
    val headerItem: LazyListScope.() -> Unit = {
        item(key = HEADING_KEY, contentType = HEADING_KEY) { header() }
    }
    val restart = {
        onFeedRestart()
        lazyPoolGamblerBets.refresh()
    }

    RefreshableLazyPagingColumn(
        modifier = modifier,
        lazyPagingItems = lazyPoolGamblerBets,
        contentPadding = contentPadding,
        onRefresh = onRefresh,
        isCompanionRefreshing = isHeadingPullRefreshing,
        loadingContent = {
            headerItem()
            matchBetPlaceholderList(count = placeholderCount, isComputed = isComputed)
        },
        emptyContent = {
            headerItem()
            lazyPagingColumnEmpty()
        },
        errorContent = { exception ->
            headerItem()
            // A hidden answer is not a failure: the screen replaces the list with the hidden state.
            if (exception !is RankedMatchBetsException.PredictionsHidden) {
                lazyPagingColumnError(exception = exception, onRetry = restart)
            }
        },
        appendLoadingContent = { item { MatchBetPlaceholderRow(isComputed = isComputed) } },
        appendErrorContent = { exception ->
            lazyPagingConcatenateError(
                exception = exception,
                onRetry = if (exception is MatchBetsLocalizedException.ListChanged) restart else lazyPoolGamblerBets::retry,
            )
        },
    ) {
        headerItem()
        items(
            count = lazyPoolGamblerBets.itemCount,
            key = { index ->
                val poolGamblerBet = lazyPoolGamblerBets.peek(index)
                if (poolGamblerBet == null) index else Pair(poolGamblerBet.poolId, poolGamblerBet.gamblerId)
            },
            contentType = { PARTICIPANT_ROW_CONTENT_TYPE },
        ) { index ->
            val poolGamblerBet = lazyPoolGamblerBets[index] ?: return@items
            val isCurrentUser = poolGamblerBet.gamblerId == viewerAccountId
            MatchGamblerBetItem(
                poolGamblerBet = poolGamblerBet,
                isCurrentUser = isCurrentUser,
                modifier = Modifier.fillMaxWidth(),
                onClick = onGamblerOpen?.takeUnless { isCurrentUser }?.let { open ->
                    { open(poolGamblerBet.poolId, poolGamblerBet.gamblerId, poolGamblerBet.gamblerUsername) }
                },
            )
            HistoryRowDivider()
        }
    }
}

/** [count] production placeholder rows for an initial load. */
internal fun LazyListScope.matchBetPlaceholderList(count: Int, isComputed: Boolean) {
    items(count = count, contentType = { PARTICIPANT_ROW_CONTENT_TYPE }) {
        MatchBetPlaceholderRow(isComputed = isComputed)
    }
}

/** One production row filled with placeholder data, with its divider. It has no action. */
@Composable
private fun MatchBetPlaceholderRow(isComputed: Boolean) {
    MatchGamblerBetItem(
        poolGamblerBet = matchGamblerBetPlaceholderModel(isComputed = isComputed),
        modifier = Modifier.fillMaxWidth(),
        isPlaceholder = true,
    )
    HistoryRowDivider()
}

private const val HEADING_KEY = "matchHeading"
private const val PARTICIPANT_ROW_CONTENT_TYPE = "MatchGamblerBet"

@PreviewLightDark
@Preview(locale = "es-rCO", fontScale = 2f)
@Composable
private fun MatchBetListComputedPreview() {
    val items = MutableStateFlow(PagingData.from(matchParticipantPreviewModels(isComputed = true)))
        .collectAsLazyPagingItems()
    TycheTheme {
        Surface {
            MatchBetList(
                lazyPoolGamblerBets = items,
                viewerAccountId = MATCH_PREVIEW_VIEWER_ID,
                isComputed = true,
                onRefresh = {},
                onFeedRestart = {},
                header = { MatchHeader(match = historyBetPreviewModels()[1], presentation = MatchBetsPresentation.COMPUTED) },
                onGamblerOpen = { _, _, _ -> },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun MatchBetListRankingUnavailablePreview() {
    val failed = LoadStates(
        refresh = LoadState.Error(MatchBetsLocalizedException.RankingUnavailable()),
        prepend = LoadState.NotLoading(endOfPaginationReached = false),
        append = LoadState.NotLoading(endOfPaginationReached = false),
    )
    val items = MutableStateFlow(PagingData.empty<PoolGamblerBetModel>(sourceLoadStates = failed))
        .collectAsLazyPagingItems()
    TycheTheme {
        Surface {
            MatchBetList(
                lazyPoolGamblerBets = items,
                viewerAccountId = MATCH_PREVIEW_VIEWER_ID,
                isComputed = false,
                onRefresh = {},
                onFeedRestart = {},
                header = {
                    MatchHeader(
                        match = historyBetPreviewModels()[1].copy(isComputed = false, matchScore = null),
                        presentation = MatchBetsPresentation.BETS_CLOSED,
                    )
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
