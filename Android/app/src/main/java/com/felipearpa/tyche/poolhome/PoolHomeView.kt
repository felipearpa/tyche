package com.felipearpa.tyche.poolhome

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.R
import com.felipearpa.tyche.account.AutoEmailAvatar
import com.felipearpa.tyche.account.navigationEmailAvatar
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.finished.FinishedBetListView
import com.felipearpa.tyche.bet.finished.finishedBetListViewModel
import com.felipearpa.tyche.bet.pending.PendingBetListView
import com.felipearpa.tyche.bet.pending.pendingBetListViewModel
import com.felipearpa.tyche.core.JoinPoolUrlTemplateProvider
import com.felipearpa.tyche.pool.gamblerscore.GamblerScoreListView
import com.felipearpa.tyche.pool.gamblerscore.gamblerScoreListViewModel
import com.felipearpa.tyche.poolhome.drawer.DrawerView
import com.felipearpa.tyche.poolhome.drawer.DrawerViewModel
import com.felipearpa.tyche.poolhome.drawer.drawerViewModel
import com.felipearpa.tyche.ui.PushDrawer
import com.felipearpa.tyche.ui.exception.ExceptionAlertDialog
import com.felipearpa.tyche.ui.exception.LocalizedException
import com.felipearpa.tyche.ui.exception.localizedOrDefault
import com.felipearpa.tyche.ui.loading.LoadingContainerView
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.ui.state.SaveState
import com.felipearpa.ui.state.isFailure
import com.felipearpa.ui.state.isSaving
import org.koin.compose.koinInject
import com.felipearpa.tyche.ui.R as SharedR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoolHomeView(
    poolId: String,
    gamblerId: String,
    onPoolChange: () -> Unit,
    onSignOut: () -> Unit = {},
    onGamblerOpen: ((poolId: String, gamblerId: String, gamblerUsername: String) -> Unit)? = null,
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)? = null,
) {
    var selectedTabIndex by rememberSaveable { mutableStateOf(Tab.GAMBLER_SCORE) }
    var isDrawerOpen by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val drawerViewModel: DrawerViewModel = drawerViewModel(poolId = poolId, gamblerId = gamblerId)
    val deleteState by drawerViewModel.deleteState.collectAsState()
    val joinPoolUrlTemplate = koinInject<JoinPoolUrlTemplateProvider>()
    var invitePoolUrl by remember { mutableStateOf(emptyString()) }

    PoolHomeContent(
        selectedTabIndex = selectedTabIndex,
        onTabChange = { selectedTabIndex = it },
        isDrawerOpen = isDrawerOpen,
        onDrawerOpenChange = { isDrawerOpen = it },
        scrollBehavior = scrollBehavior,
        onPoolChange = onPoolChange,
        drawerContent = {
            DrawerView(
                viewModel = drawerViewModel,
                onCloseDrawer = { isDrawerOpen = false },
                onSignOut = onSignOut,
                onInvite = {
                    isDrawerOpen = false
                    invitePoolUrl = String.format(joinPoolUrlTemplate(), poolId)
                },
                onPoolDeleting = {
                    isDrawerOpen = false
                },
                onPoolDeleted = onPoolChange,
            )
        },
        isSaving = deleteState.isSaving(),
        content = {
            when (selectedTabIndex) {
                Tab.GAMBLER_SCORE -> GamblerScoreListView(
                    viewModel = gamblerScoreListViewModel(
                        poolId = poolId,
                        gamblerId = gamblerId,
                    ),
                    onGamblerOpen = onGamblerOpen,
                )

                Tab.BET_EDITOR -> PendingBetListView(
                    viewModel = pendingBetListViewModel(
                        poolId = poolId,
                        gamblerId = gamblerId,
                    ),
                    onMatchOpen = onMatchOpen,
                )

                Tab.HISTORY_BET -> FinishedBetListView(
                    viewModel = finishedBetListViewModel(
                        poolId = poolId,
                        gamblerId = gamblerId,
                    ),
                    onMatchOpen = onMatchOpen,
                )
            }
        },
    )

    if (deleteState.isFailure()) {
        val exception = (deleteState as SaveState.Failure).exception
        ExceptionAlertDialog(
            exception = exception as? LocalizedException ?: exception.localizedOrDefault(),
            onDismiss = { drawerViewModel.resetDeleteState() },
        )
    }

    if (invitePoolUrl.isNotEmpty()) {
        InvitePoolSharer(url = invitePoolUrl, onClose = { invitePoolUrl = emptyString() })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PoolHomeContent(
    selectedTabIndex: Tab,
    onTabChange: (Tab) -> Unit,
    isDrawerOpen: Boolean,
    onDrawerOpenChange: (Boolean) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    onPoolChange: () -> Unit,
    drawerContent: @Composable () -> Unit,
    isSaving: Boolean,
    content: @Composable () -> Unit,
) {
    PushDrawer(
        isOpen = isDrawerOpen,
        onOpenChange = onDrawerOpenChange,
        drawerContent = drawerContent,
    ) {
        val scaffoldContent = @Composable {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    AppTopBar(
                        title = selectedTabIndex.title,
                        onAccountShow = { onDrawerOpenChange(!isDrawerOpen) },
                        onPoolChange = onPoolChange,
                        scrollBehavior = scrollBehavior,
                    )
                },
                bottomBar = {
                    PrimaryTabRow(
                        selectedTabIndex = selectedTabIndex.ordinal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                    ) {
                        GamblerScoreTab(
                            selected = selectedTabIndex == Tab.GAMBLER_SCORE,
                            onClick = { onTabChange(Tab.GAMBLER_SCORE) },
                        )
                        BetEditorTab(
                            selected = selectedTabIndex == Tab.BET_EDITOR,
                            onClick = { onTabChange(Tab.BET_EDITOR) },
                        )
                        HistoryBetTab(
                            selected = selectedTabIndex == Tab.HISTORY_BET,
                            onClick = { onTabChange(Tab.HISTORY_BET) },
                        )
                    }
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .padding(paddingValues = innerPadding)
                        .fillMaxSize(),
                ) {
                    content()
                }
            }
        }

        if (isSaving) {
            LoadingContainerView { scaffoldContent() }
        } else {
            scaffoldContent()
        }
    }
}

@Composable
private fun InvitePoolSharer(url: String, onClose: () -> Unit) {
    val activityResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { _ -> onClose() }

    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, url)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)

    LaunchedEffect(Unit) {
        activityResultLauncher.launch(shareIntent)
    }
}

@Composable
private fun GamblerScoreTab(selected: Boolean, onClick: () -> Unit) {
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
                painter = painterResource(id = R.drawable.ic_sport_score),
                contentDescription = emptyString(),
                modifier = Modifier.size(iconSize),
            )
            Text(
                text = stringResource(id = R.string.score_tab),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BetEditorTab(selected: Boolean, onClick: () -> Unit) {
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
    onAccountShow: () -> Unit,
    onPoolChange: () -> Unit,
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                modifier = shimmerModifier,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onAccountShow) {
                AutoEmailAvatar(modifier = Modifier.navigationEmailAvatar())
            }
        },
        actions = {
            IconButton(onClick = onPoolChange) {
                Icon(
                    painter = painterResource(id = SharedR.drawable.swap_horizontal),
                    contentDescription = "",
                )
            }
        },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
    )
}

private val iconSize = 24.dp

private enum class Tab {
    GAMBLER_SCORE,
    BET_EDITOR,
    HISTORY_BET
}

private val Tab.title: String
    @Composable
    get() = when (this) {
        Tab.GAMBLER_SCORE -> stringResource(id = R.string.score_tab)
        Tab.BET_EDITOR -> stringResource(id = R.string.bet_tab)
        Tab.HISTORY_BET -> stringResource(id = R.string.history_bets_tab)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun PoolHomePreview() {
    TycheTheme {
        PoolHomeContent(
            selectedTabIndex = Tab.GAMBLER_SCORE,
            onTabChange = {},
            isDrawerOpen = false,
            onDrawerOpenChange = {},
            scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
            onPoolChange = {},
            drawerContent = { Text("Drawer Content") },
            isSaving = false,
            content = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Main Content")
                }
            },
        )
    }
}
