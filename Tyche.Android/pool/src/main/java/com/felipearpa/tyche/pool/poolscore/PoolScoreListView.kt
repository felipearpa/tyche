package com.felipearpa.tyche.pool.poolscore

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.core.emptyString
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.ui.useDebounce
import kotlinx.coroutines.launch
import com.felipearpa.tyche.ui.R as SharedR

@Composable
fun PoolScoreListView(
    viewModel: PoolScoreListViewModel,
    settingsView: @Composable () -> Unit,
    onDetailRequested: (String, String) -> Unit
) {
    var filterText by remember { mutableStateOf(emptyString()) }
    val lazyItems = viewModel.poolGamblerScores.collectAsLazyPagingItems()
    val onFilterChange = { newFilterText: String -> filterText = newFilterText }
    val pageSize = viewModel.pageSize
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    filterText.useDebounce { newFilterText -> viewModel.search(newFilterText) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet { settingsView() }
        },
    ) {
        Scaffold(
            topBar = {
                AppTopBar(
                    title = { Text(text = stringResource(id = R.string.gambler_pool_list_title)) },
                    onAccountRequested = {
                        coroutineScope.launch {
                            drawerState.apply {
                                if (isClosed) open() else close()
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            PoolScoreList(
                lazyPoolGamblerScores = lazyItems,
                filterText = filterText,
                onFilterValueChange = onFilterChange,
                fakeItemCount = pageSize,
                onDetailRequested = onDetailRequested,
                modifier = Modifier
                    .padding(paddingValues = paddingValues)
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(title: @Composable () -> Unit, onAccountRequested: () -> Unit) {
    TopAppBar(
        title = title,
        navigationIcon = {
            IconButton(onClick = onAccountRequested) {
                Icon(
                    painter = painterResource(id = SharedR.drawable.account_circle),
                    contentDescription = emptyString()
                )
            }
        },
    )
}