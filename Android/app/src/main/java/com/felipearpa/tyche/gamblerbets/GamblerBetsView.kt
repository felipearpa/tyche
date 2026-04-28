package com.felipearpa.tyche.gamblerbets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.R
import com.felipearpa.tyche.bet.finished.FinishedBetListView
import com.felipearpa.tyche.bet.finished.finishedBetListViewModel
import com.felipearpa.tyche.bet.live.LiveBetListView
import com.felipearpa.tyche.bet.live.liveBetListViewModel
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.R as SharedR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamblerBetsView(
    poolId: String,
    gamblerId: String,
    gamblerUsername: String,
    onBack: () -> Unit,
) {
    var selectedTabIndex by remember { mutableStateOf(Tab.LIVE_BET) }
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
        bottomBar = {
            SecondaryTabRow(
                selectedTabIndex = selectedTabIndex.ordinal,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
            ) {
                LiveBetTab(
                    selected = selectedTabIndex == Tab.LIVE_BET,
                    onClick = { selectedTabIndex = Tab.LIVE_BET },
                )
                HistoryBetTab(
                    selected = selectedTabIndex == Tab.HISTORY_BET,
                    onClick = { selectedTabIndex = Tab.HISTORY_BET },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(paddingValues = innerPadding)
                .fillMaxSize(),
        ) {
            when (selectedTabIndex) {
                Tab.LIVE_BET -> LiveBetListView(
                    viewModel = liveBetListViewModel(
                        poolId = poolId,
                        gamblerId = gamblerId,
                    ),
                )

                Tab.HISTORY_BET -> FinishedBetListView(
                    viewModel = finishedBetListViewModel(
                        poolId = poolId,
                        gamblerId = gamblerId,
                    ),
                )
            }
        }
    }
}

@Composable
private fun LiveBetTab(selected: Boolean, onClick: () -> Unit) {
    Tab(
        selected = selected,
        onClick = onClick,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.padding(vertical = LocalBoxSpacing.current.medium),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_money),
                contentDescription = emptyString(),
                modifier = Modifier.size(iconSize),
            )
            Text(
                text = stringResource(id = R.string.bet_tab),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HistoryBetTab(selected: Boolean, onClick: () -> Unit) {
    Tab(
        selected = selected,
        onClick = onClick,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.padding(vertical = LocalBoxSpacing.current.medium),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.history_bets),
                contentDescription = emptyString(),
                modifier = Modifier.size(iconSize),
            )
            Text(
                text = stringResource(id = R.string.history_bets_tab),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
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

private val iconSize = 24.dp

private enum class Tab {
    LIVE_BET,
    HISTORY_BET
}
