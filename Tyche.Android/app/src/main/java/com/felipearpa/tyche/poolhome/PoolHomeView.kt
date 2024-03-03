package com.felipearpa.tyche.poolhome

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.R
import com.felipearpa.tyche.bet.PoolGamblerBetListView
import com.felipearpa.tyche.bet.poolGamblerBetListViewModel
import com.felipearpa.tyche.core.emptyString
import com.felipearpa.tyche.pool.PoolModel
import com.felipearpa.tyche.pool.gamblerscore.GamblerScoreListView
import com.felipearpa.tyche.pool.gamblerscore.gamblerScoreListViewModel
import com.felipearpa.tyche.settings.SettingsView
import com.felipearpa.tyche.settings.settingsViewModel
import com.felipearpa.tyche.ui.exception.ExceptionView
import com.felipearpa.tyche.ui.exception.localizedExceptionOrNull
import com.felipearpa.tyche.ui.shimmer
import com.felipearpa.tyche.ui.state.LoadableViewState
import com.felipearpa.tyche.ui.theme.boxSpacing
import kotlinx.coroutines.launch
import com.felipearpa.tyche.ui.R as SharedR

private val iconSize = 24.dp

private enum class Tab {
    GAMBLER_SCORE,
    BET_EDITOR
}

@Composable
fun PoolHomeView(
    viewModel: PoolHomeViewModel,
    onPoolScoreListRequested: () -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    PoolHomeView(
        state = state,
        poolId = viewModel.poolId,
        gamblerId = viewModel.gamblerId,
        onPoolScoreListRequested = onPoolScoreListRequested,
        onLogout = onLogout
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PoolHomeView(
    state: LoadableViewState<PoolModel>,
    poolId: String,
    gamblerId: String,
    onPoolScoreListRequested: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var selectedTabIndex by remember { mutableStateOf(Tab.GAMBLER_SCORE) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    when (state) {
        is LoadableViewState.Failure -> ExceptionView(
            exception = state().localizedExceptionOrNull()!!
        )

        else -> ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    SettingsView(
                        viewModel = settingsViewModel(),
                        onLogout = onLogout
                    )
                }
            },
        ) {
            Scaffold(
                topBar = {
                    if (state is LoadableViewState.Success) {
                        val pool = state()
                        AppTopBar(
                            title = pool.poolName,
                            onAccountRequested = {
                                coroutineScope.launch {
                                    drawerState.apply {
                                        if (isClosed) open() else close()
                                    }
                                }
                            },
                            poolScoreListRequested = onPoolScoreListRequested,
                            scrollBehavior = scrollBehavior
                        )
                    } else if (state is LoadableViewState.Loading) {
                        AppFakeTopBar()
                    }
                },
                bottomBar = {
                    TabRow(
                        selectedTabIndex = selectedTabIndex.ordinal,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = selectedTabIndex == Tab.GAMBLER_SCORE,
                            onClick = { selectedTabIndex = Tab.GAMBLER_SCORE }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.padding(vertical = MaterialTheme.boxSpacing.medium)
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

                        Tab(
                            selected = selectedTabIndex == Tab.BET_EDITOR,
                            onClick = { selectedTabIndex = Tab.BET_EDITOR }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.padding(vertical = MaterialTheme.boxSpacing.medium)
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
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues = paddingValues)) {
                    when (selectedTabIndex) {
                        Tab.GAMBLER_SCORE -> GamblerScoreListView(
                            viewModel = gamblerScoreListViewModel(
                                poolId = poolId,
                                gamblerId = gamblerId
                            )
                        )

                        Tab.BET_EDITOR -> PoolGamblerBetListView(
                            viewModel = poolGamblerBetListViewModel(
                                poolId = poolId,
                                gamblerId = gamblerId
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppFakeTopBar() {
    AppTopBar(
        title = "X".repeat(15),
        onAccountRequested = {},
        poolScoreListRequested = {},
        modifier = Modifier.fillMaxWidth(),
        shimmerModifier = Modifier.shimmer(),
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    title: String,
    onAccountRequested: () -> Unit,
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
    poolScoreListRequested: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable { poolScoreListRequested() }
                    .then(shimmerModifier)
            )
        },
        navigationIcon = {
            IconButton(onClick = onAccountRequested) {
                Icon(
                    painter = painterResource(id = SharedR.drawable.account_circle),
                    contentDescription = emptyString()
                )
            }
        },
        modifier = modifier,
        scrollBehavior = scrollBehavior
    )
}