package com.felipearpa.tyche.poolhome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.R
import com.felipearpa.tyche.bet.pending.PendingBetListView
import com.felipearpa.tyche.bet.finished.FinishedBetListView
import com.felipearpa.tyche.bet.finished.finishedBetListViewModel
import com.felipearpa.tyche.bet.pending.pendingBetListViewModel
import com.felipearpa.tyche.core.emptyString
import com.felipearpa.tyche.pool.gamblerscore.GamblerScoreListView
import com.felipearpa.tyche.pool.gamblerscore.gamblerScoreListViewModel
import com.felipearpa.tyche.poolhome.drawer.DrawerView
import com.felipearpa.tyche.poolhome.drawer.drawerViewModel
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import kotlinx.coroutines.launch
import com.felipearpa.tyche.ui.R as SharedR

private val iconSize = 24.dp

private enum class Tab {
    GAMBLER_SCORE,
    BET_EDITOR,
    HISTORY_BET
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoolHomeView(
    poolId: String,
    gamblerId: String,
    changePool: () -> Unit,
    onLogout: () -> Unit = {}
) {
    var selectedTabIndex by remember { mutableStateOf(Tab.GAMBLER_SCORE) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerView(
                    viewModel = drawerViewModel(poolId = poolId, gamblerId = gamblerId),
                    changePool = changePool,
                    onLogout = onLogout
                )
            }
        },
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                AppTopBar(
                    title = selectedTabIndex.title,
                    onAccountRequested = {
                        coroutineScope.launch {
                            drawerState.apply {
                                if (isClosed) open() else close()
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            bottomBar = {
                TabRow(
                    selectedTabIndex = selectedTabIndex.ordinal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GamblerScoreTab(
                        selected = selectedTabIndex == Tab.GAMBLER_SCORE,
                        onClick = { selectedTabIndex = Tab.GAMBLER_SCORE }
                    )
                    BetEditorTab(
                        selected = selectedTabIndex == Tab.BET_EDITOR,
                        onClick = { selectedTabIndex = Tab.BET_EDITOR }
                    )
                    HistoryBetTab(
                        selected = selectedTabIndex == Tab.HISTORY_BET,
                        onClick = { selectedTabIndex = Tab.HISTORY_BET }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues = paddingValues)
                    .fillMaxSize()
            ) {
                when (selectedTabIndex) {
                    Tab.GAMBLER_SCORE -> GamblerScoreListView(
                        viewModel = gamblerScoreListViewModel(
                            poolId = poolId,
                            gamblerId = gamblerId
                        )
                    )

                    Tab.BET_EDITOR -> PendingBetListView(
                        viewModel = pendingBetListViewModel(
                            poolId = poolId,
                            gamblerId = gamblerId
                        )
                    )

                    Tab.HISTORY_BET -> FinishedBetListView(
                        viewModel = finishedBetListViewModel(
                            poolId = poolId,
                            gamblerId = gamblerId
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun GamblerScoreTab(selected: Boolean, onClick: () -> Unit) {
    Tab(
        selected = selected,
        onClick = onClick
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.padding(vertical = LocalBoxSpacing.current.medium)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_sport_score),
                contentDescription = emptyString(),
                modifier = Modifier.size(iconSize)
            )
            Text(
                text = stringResource(id = R.string.score_tab),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BetEditorTab(selected: Boolean, onClick: () -> Unit) {
    Tab(
        selected = selected,
        onClick = onClick
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.padding(vertical = LocalBoxSpacing.current.medium)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_money),
                contentDescription = emptyString(),
                modifier = Modifier.size(iconSize)
            )
            Text(
                text = stringResource(id = R.string.bet_tab),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HistoryBetTab(selected: Boolean, onClick: () -> Unit) {
    Tab(
        selected = selected,
        onClick = onClick
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.padding(vertical = LocalBoxSpacing.current.medium)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.history_bets),
                contentDescription = emptyString(),
                modifier = Modifier.size(iconSize)
            )
            Text(
                text = stringResource(id = R.string.history_bets_tab),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    title: String,
    onAccountRequested: () -> Unit,
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                modifier = shimmerModifier,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onAccountRequested) {
                Icon(
                    painter = painterResource(id = SharedR.drawable.menu),
                    contentDescription = emptyString()
                )
            }
        },
        modifier = modifier,
        scrollBehavior = scrollBehavior
    )
}

private val Tab.title: String
    @Composable
    get() = when (this) {
        Tab.GAMBLER_SCORE -> stringResource(id = R.string.score_tab)
        Tab.BET_EDITOR -> stringResource(id = R.string.bet_tab)
        Tab.HISTORY_BET -> stringResource(id = R.string.history_bets_tab)
    }