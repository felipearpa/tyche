package com.felipearpa.tyche.bet.timeline

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.poolGamblerBetDummyModels
import kotlinx.coroutines.flow.MutableStateFlow
import com.felipearpa.tyche.ui.R as SharedR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BetTimelineListView(
    poolId: String,
    gamblerId: String,
    gamblerUsername: String,
    onBack: () -> Unit,
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)? = null,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AppTopBar(
                title = gamblerUsername,
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        if (LocalInspectionMode.current) {
            val lazyItems =
                MutableStateFlow(PagingData.from(poolGamblerBetDummyModels())).collectAsLazyPagingItems()
            BetTimelineListView(
                lazyBets = lazyItems,
                placeholderCount = 50,
                onMatchOpen = {},
                modifier = Modifier
                    .padding(paddingValues = innerPadding)
                    .fillMaxSize(),
            )

            return@Scaffold
        }

        BetTimelineListView(
            viewModel = betsTimelineViewModel(
                poolId = poolId,
                gamblerId = gamblerId,
            ),
            onMatchOpen = onMatchOpen,
            modifier = Modifier
                .padding(paddingValues = innerPadding)
                .fillMaxSize(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(id = SharedR.drawable.arrow_back),
                    contentDescription = emptyString(),
                )
            }
        },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun BetTimelineListView(
    viewModel: BetsTimelineViewModel,
    modifier: Modifier = Modifier,
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)? = null,
) {
    val lazyItems = viewModel.poolGamblerBets.collectAsLazyPagingItems()
    val pageSize = viewModel.pageSize

    BetsTimelineList(
        lazyBets = lazyItems,
        placeholderCount = pageSize,
        modifier = modifier,
        onMatchOpen = onMatchOpen,
    )
}

@Composable
private fun BetTimelineListView(
    lazyBets: LazyPagingItems<PoolGamblerBetModel>,
    placeholderCount: Int,
    modifier: Modifier = Modifier,
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)?,
) {
    BetsTimelineList(
        lazyBets = lazyBets,
        placeholderCount = placeholderCount,
        modifier = modifier,
        onMatchOpen = onMatchOpen,
    )
}

@Preview(showBackground = true)
@Composable
private fun BetTimelineListViewPreview() {
    BetTimelineListView(
        poolId = "poolId",
        gamblerId = "gamblerId",
        gamblerUsername = "felipearcila@gmail.com",
        onBack = {},
        onMatchOpen = {},
    )
}
