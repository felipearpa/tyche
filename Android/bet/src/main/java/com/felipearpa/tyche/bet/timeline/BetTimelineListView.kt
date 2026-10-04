package com.felipearpa.tyche.bet.timeline

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.R
import com.felipearpa.tyche.bet.finished.HistoryPoints
import com.felipearpa.tyche.bet.finished.HistoryPointsSummaryState
import com.felipearpa.tyche.bet.timelineBetPreviewModels
import kotlinx.coroutines.flow.MutableStateFlow
import com.felipearpa.tyche.ui.R as SharedR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BetTimelineListView(
    poolId: String,
    gamblerId: String,
    gamblerUsername: String,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)? = null,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AppTopBar(
                title = stringResource(id = R.string.bet_timeline_view_title),
                onBack = onBack,
                onHome = onHome,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        if (LocalInspectionMode.current) {
            val lazyItems =
                MutableStateFlow(PagingData.from(timelineBetPreviewModels())).collectAsLazyPagingItems()
            BetTimelineList(
                gamblerId = gamblerId,
                gamblerUsername = gamblerUsername,
                lazyBets = lazyItems,
                pointsSummary = HistoryPointsSummaryState.initial.loaded(HistoryPoints.Earned(676)),
                onPointsSummaryRetry = {},
                onRefresh = {},
                contentPadding = innerPadding,
                modifier = Modifier.fillMaxSize(),
            )
            return@Scaffold
        }

        BetTimelineListView(
            viewModel = betTimelineListViewModel(
                poolId = poolId,
                gamblerId = gamblerId,
            ),
            gamblerUsername = gamblerUsername,
            onMatchOpen = onMatchOpen,
            contentPadding = innerPadding,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    title: String,
    onBack: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    TopAppBar(
        title = { Text(text = title) },
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

@Composable
private fun BetTimelineListView(
    viewModel: BetTimelineListViewModel,
    gamblerUsername: String,
    contentPadding: PaddingValues,
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)?,
) {
    val lazyItems = viewModel.poolGamblerBets.collectAsLazyPagingItems()
    val pointsSummary by viewModel.pointsSummary.collectAsState()

    // The list scrolls under the top app bar and the system bars; its content padding keeps the
    // first and last content clear of them.
    BetTimelineList(
        gamblerId = viewModel.gamblerId,
        gamblerUsername = gamblerUsername,
        lazyBets = lazyItems,
        pointsSummary = pointsSummary,
        onPointsSummaryRetry = viewModel::loadPointsSummary,
        onRefresh = viewModel::refreshPointsSummary,
        placeholderCount = viewModel.pageSize,
        contentPadding = contentPadding,
        modifier = Modifier
            .fillMaxSize()
            .consumeWindowInsets(contentPadding),
        onMatchOpen = onMatchOpen,
    )
}

@Preview(showBackground = true)
@Composable
private fun BetTimelineListViewPreview() {
    BetTimelineListView(
        poolId = "poolId",
        gamblerId = "",
        gamblerUsername = "felipearcila@gmail.com",
        onBack = {},
        onHome = {},
        onMatchOpen = {},
    )
}
