package com.felipearpa.tyche.pool.poolscore

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModels
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import com.felipearpa.tyche.ui.R as SharedR

@Composable
fun PoolScoreListView(
    viewModel: PoolScoreListViewModel,
    drawerView: @Composable () -> Unit,
    onPoolClick: (String, String) -> Unit,
    onCreatePoolClick: () -> Unit,
) {
    val lazyItems = viewModel.poolGamblerScores.collectAsLazyPagingItems()
    val pageSize = viewModel.pageSize

    PoolScoreListView(
        lazyItems = lazyItems,
        pageSize = pageSize,
        drawerView = drawerView,
        onPoolClick = onPoolClick,
        onCreatePoolClick = onCreatePoolClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PoolScoreListView(
    lazyItems: LazyPagingItems<PoolGamblerScoreModel>,
    pageSize: Int = 50,
    drawerView: @Composable () -> Unit,
    onPoolClick: (String, String) -> Unit,
    onCreatePoolClick: () -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    ModalNavigationDrawer(
        modifier = Modifier.fillMaxSize(),
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet { drawerView() }
        },
    ) {
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    title = { Text(text = stringResource(id = R.string.gambler_pool_list_title)) },
                    scrollBehavior = scrollBehavior,
                    onAccountClick = {
                        coroutineScope.launch {
                            drawerState.apply {
                                if (isClosed) open() else close()
                            }
                        }
                    },
                    onCreatePoolClick = onCreatePoolClick,
                )
            },
        ) { paddingValues ->
            PoolScoreList(
                lazyPoolGamblerScores = lazyItems,
                fakeItemCount = pageSize,
                onPoolClick = onPoolClick,
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
    onAccountClick: () -> Unit,
    onCreatePoolClick: () -> Unit,
) {
    TopAppBar(
        title = title,
        navigationIcon = {
            IconButton(onClick = onAccountClick) {
                Icon(
                    painter = painterResource(id = SharedR.drawable.menu),
                    contentDescription = emptyString(),
                )
            }
        },
        actions = {
            IconButton(onClick = onCreatePoolClick) {
                Icon(
                    painter = painterResource(id = SharedR.drawable.filled_add),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "Localized description",
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@PreviewLightDark
@Composable
private fun PoolScoreListViewPreview() {
    val items = flowOf(PagingData.from(poolGamblerScoreDummyModels())).collectAsLazyPagingItems()
    TycheTheme {
        Surface {
            PoolScoreListView(
                lazyItems = items,
                drawerView = {},
                onPoolClick = { _, _ -> },
                onCreatePoolClick = {},
            )
        }
    }
}
