package com.felipearpa.tyche.pool.poolscore

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModels
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import com.felipearpa.tyche.ui.R as SharedR

@Composable
fun PoolScoreListView(
    viewModel: PoolScoreListViewModel,
    drawerView: @Composable () -> Unit,
    onPoolOpen: (poolId: String, gamblerId: String) -> Unit,
    onPoolCreate: () -> Unit,
) {
    val lazyItems = viewModel.poolGamblerScores.collectAsLazyPagingItems()
    val pageSize = viewModel.pageSize
    var invitePoolUrl by remember { mutableStateOf(emptyString()) }
    val refreshEvent = viewModel.refreshEvents.collectAsState(Unit)

    PoolScoreListView(
        lazyItems = lazyItems,
        pageSize = pageSize,
        drawerView = drawerView,
        onPoolOpen = onPoolOpen,
        onPoolCreate = onPoolCreate,
        onPoolJoin = { poolId ->
            invitePoolUrl = viewModel.createUrlForJoining(poolId = poolId)
        },
    )

    if (invitePoolUrl.isNotEmpty()) {
        InvitePoolSharer(url = invitePoolUrl, onClose = { invitePoolUrl = emptyString() })
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PoolScoreListView(
    lazyItems: LazyPagingItems<PoolGamblerScoreModel>,
    pageSize: Int = 50,
    drawerView: @Composable () -> Unit,
    onPoolOpen: (poolId: String, gamblerId: String) -> Unit,
    onPoolJoin: (poolId: String) -> Unit,
    onPoolCreate: () -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    ModalNavigationDrawer(
        modifier = Modifier.fillMaxSize(),
        drawerState = drawerState,
        drawerContent = { ModalDrawerSheet { drawerView() } },
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    title = { Text(text = stringResource(id = R.string.gambler_pool_list_title)) },
                    scrollBehavior = scrollBehavior,
                    onAccountOpen = {
                        coroutineScope.launch {
                            drawerState.apply {
                                if (isClosed) open() else close()
                            }
                        }
                    },
                    onPoolCreate = onPoolCreate,
                )
            },
        ) { paddingValues ->
            PoolScoreList(
                lazyPoolGamblerScores = lazyItems,
                fakeItemCount = pageSize,
                onPoolOpen = onPoolOpen,
                onPoolJoin = onPoolJoin,
                onPoolCreate = onPoolCreate,
                modifier = Modifier
                    .padding(paddingValues = paddingValues)
                    .fillMaxSize(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppBar(
    title: @Composable () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    onAccountOpen: () -> Unit,
    onPoolCreate: () -> Unit,
) {
    TopAppBar(
        title = title,
        navigationIcon = {
            IconButton(onClick = onAccountOpen) {
                Icon(
                    painter = painterResource(id = SharedR.drawable.menu),
                    contentDescription = emptyString(),
                )
            }
        },
        actions = {
            IconButton(onClick = onPoolCreate) {
                Icon(
                    painter = painterResource(id = SharedR.drawable.filled_add),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(createIconSize),
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

private val createIconSize = 32.dp

@PreviewLightDark
@Composable
private fun PoolScoreListViewPreview() {
    val items =
        MutableStateFlow(PagingData.from(poolGamblerScoreDummyModels())).collectAsLazyPagingItems()
    TycheTheme {
        Surface {
            PoolScoreListView(
                lazyItems = items,
                drawerView = {},
                onPoolOpen = { _, _ -> },
                onPoolJoin = {},
                onPoolCreate = {},
            )
        }
    }
}
