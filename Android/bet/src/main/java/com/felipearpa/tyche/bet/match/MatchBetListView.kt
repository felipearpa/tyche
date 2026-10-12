package com.felipearpa.tyche.bet.match

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.R
import com.felipearpa.tyche.bet.finished.HistoryPointsRetryButton
import com.felipearpa.tyche.bet.finished.HistoryStyle
import com.felipearpa.tyche.bet.historyBetPreviewModels
import com.felipearpa.tyche.bet.matchHeaderPlaceholderModel
import com.felipearpa.tyche.core.type.TeamScore
import com.felipearpa.tyche.ui.CenteredScrollableColumn
import com.felipearpa.tyche.ui.exception.ExceptionView
import com.felipearpa.tyche.ui.network.NetworkLocalizedException
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.tyche.ui.R as SharedR

/**
 * The match screen: the match heading, then either the hidden-bets explanation with the viewer's
 * own prediction, or every participant's prediction in pool-total order. No participant data is
 * requested or shown until the heading confirms the gate; until then the heading and the rows show
 * their placeholders.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchBetListView(
    viewModel: MatchBetListViewModel,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onGamblerOpen: ((poolId: String, gamblerId: String, gamblerUsername: String) -> Unit)? = null,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val heading by viewModel.heading.collectAsState()

    // Entry and every return to the screen recheck the match.
    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { AppTopBar(onBack = onBack, onHome = onHome, scrollBehavior = scrollBehavior) },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .consumeWindowInsets(innerPadding)
        when (val presentation = heading.presentation) {
            MatchHeadingState.Presentation.Placeholder ->
                MatchHeadingPlaceholderContent(
                    placeholderCount = viewModel.pageSize,
                    contentPadding = innerPadding,
                    modifier = contentModifier,
                )

            MatchHeadingState.Presentation.Failed ->
                MatchHeadingFailureContent(onRetry = viewModel::load, contentPadding = innerPadding, modifier = contentModifier)

            is MatchHeadingState.Presentation.Loaded -> if (presentation.presentation == MatchBetsPresentation.HIDDEN) {
                val ownBet by viewModel.ownBet.collectAsState()
                MatchHiddenScrollContent(
                    isRefreshing = heading.isPullRefreshing,
                    onRefresh = viewModel::refresh,
                    contentPadding = innerPadding,
                    modifier = contentModifier,
                    header = { MatchHeading(presentation, onRetry = viewModel::refresh) },
                ) {
                    MatchHiddenBetsContent(
                        match = presentation.match,
                        ownBet = ownBet,
                        onOwnBetRetry = viewModel::retryOwnBet,
                    )
                }
            } else {
                MatchParticipantsContent(
                    viewModel = viewModel,
                    heading = presentation,
                    isHeadingPullRefreshing = heading.isPullRefreshing,
                    onGamblerOpen = onGamblerOpen,
                    contentPadding = innerPadding,
                    modifier = contentModifier,
                )
            }
        }
    }
}

/** Collects the participant feed only here, once the gate confirms bets are revealed. */
@Composable
private fun MatchParticipantsContent(
    viewModel: MatchBetListViewModel,
    heading: MatchHeadingState.Presentation.Loaded,
    isHeadingPullRefreshing: Boolean,
    onGamblerOpen: ((poolId: String, gamblerId: String, gamblerUsername: String) -> Unit)?,
    contentPadding: PaddingValues,
    modifier: Modifier,
) {
    val lazyPoolGamblerBets = viewModel.participantBets.collectAsLazyPagingItems()
    MatchBetList(
        lazyPoolGamblerBets = lazyPoolGamblerBets,
        viewerAccountId = viewModel.viewerAccountId,
        isComputed = heading.presentation == MatchBetsPresentation.COMPUTED,
        onRefresh = viewModel::refresh,
        onFeedRestart = viewModel::onFeedRestart,
        isHeadingPullRefreshing = isHeadingPullRefreshing,
        header = { MatchHeading(heading, onRetry = viewModel::refresh) },
        contentPadding = contentPadding,
        placeholderCount = viewModel.pageSize,
        onGamblerOpen = onGamblerOpen,
        modifier = modifier,
    )
}

/** The confirmed heading, with a failure line and retry when its latest refresh failed. */
@Composable
private fun MatchHeading(heading: MatchHeadingState.Presentation.Loaded, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
        modifier = Modifier.headingPadding(),
    ) {
        MatchHeader(match = heading.match, presentation = heading.presentation)
        if (heading.refreshStatus == MatchHeadingState.RefreshStatus.FAILED) {
            MatchHeadingRefreshFailure(onRetry = onRetry)
        }
    }
}

/** A failed refresh keeps the last confirmed heading and says so, with a retry. */
@Composable
private fun MatchHeadingRefreshFailure(onRetry: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
    ) {
        Text(
            text = stringResource(R.string.match_bets_heading_refresh_failure),
            style = MaterialTheme.typography.bodyMedium,
            color = HistoryStyle.secondaryText,
            modifier = Modifier.weight(1f, fill = false),
        )
        HistoryPointsRetryButton(onClick = onRetry)
    }
}

@Composable
private fun Modifier.headingPadding() =
    fillMaxWidth()
        .padding(horizontal = HistoryStyle.horizontalPadding)
        .padding(top = LocalBoxSpacing.current.medium, bottom = LocalBoxSpacing.current.large)

/**
 * The loading screen while the gate is unknown: the heading's placeholder above placeholder rows,
 * laid out as [MatchBetList] lays out its first load, so confirming a revealed match only fills
 * in the heading. The rows are filler, never participant data, and nothing is requested for them.
 */
@Composable
private fun MatchHeadingPlaceholderContent(placeholderCount: Int, contentPadding: PaddingValues, modifier: Modifier) {
    LazyColumn(modifier = modifier, contentPadding = contentPadding, userScrollEnabled = false) {
        item {
            MatchHeader(
                match = matchHeaderPlaceholderModel(),
                presentation = MatchBetsPresentation.HIDDEN,
                isPlaceholder = true,
                modifier = Modifier.headingPadding(),
            )
        }
        matchBetPlaceholderList(count = placeholderCount, isComputed = false)
    }
}

/** The first heading request failed: nothing about the match is known, so only a retry is offered. */
@Composable
private fun MatchHeadingFailureContent(onRetry: () -> Unit, contentPadding: PaddingValues, modifier: Modifier) {
    CenteredScrollableColumn(
        modifier = modifier,
        contentModifier = Modifier
            .padding(contentPadding)
            .padding(all = LocalBoxSpacing.current.large),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.large),
        ) {
            ExceptionView(localizedException = NetworkLocalizedException.RemoteCommunication)
            Button(onClick = onRetry) {
                Text(text = stringResource(SharedR.string.retry_action))
            }
        }
    }
}

/**
 * The hidden state's scrolling composition: the heading at the top, then [content] centered in the
 * visible space left below it. When text is too large to fit, the whole content grows and scrolls.
 * Pull to refresh rechecks the match.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatchHiddenScrollContent(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    contentPadding: PaddingValues,
    header: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        modifier = modifier,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = contentPadding.calculateTopPadding()),
            )
        },
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(contentPadding),
            ) {
                header()
                Spacer(modifier = Modifier.height(LocalBoxSpacing.current.large))
                Spacer(modifier = Modifier.weight(1f))
                Box(modifier = Modifier.padding(horizontal = HistoryStyle.horizontalPadding)) { content() }
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(LocalBoxSpacing.current.large * 2))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    onBack: () -> Unit,
    onHome: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.match_bets_view_title)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(id = SharedR.drawable.arrow_back),
                    contentDescription = stringResource(id = SharedR.string.back_action),
                )
            }
        },
        actions = {
            IconButton(onClick = onHome) {
                Icon(
                    painter = painterResource(id = SharedR.drawable.home),
                    contentDescription = stringResource(id = SharedR.string.go_home_action),
                )
            }
        },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
    )
}

@PreviewLightDark
@Preview(locale = "es-rCO", fontScale = 2f)
@Composable
private fun MatchHiddenScrollContentPreview() {
    val match: PoolGamblerBetModel = historyBetPreviewModels()[1].copy(isLocked = false, isComputed = false)
    val loaded = MatchHeadingState.initial.loaded(match).presentation as MatchHeadingState.Presentation.Loaded
    TycheTheme {
        Surface {
            MatchHiddenScrollContent(
                isRefreshing = false,
                onRefresh = {},
                contentPadding = PaddingValues(0.dp),
                header = { MatchHeading(loaded, onRetry = {}) },
                modifier = Modifier.fillMaxSize(),
            ) {
                MatchHiddenBetsContent(match = match, ownBet = OwnBetState.Bet(TeamScore(3, 2)), onOwnBetRetry = {})
            }
        }
    }
}
