package com.felipearpa.tyche.pool.poolscore

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.core.emptyString
import com.felipearpa.tyche.pool.R

@Composable
fun PoolScoreListView(
    viewModel: PoolScoreListViewModel,
    onDetailRequested: (String, String) -> Unit
) {
    var filterText by remember { mutableStateOf(emptyString()) }
    val lazyItems = viewModel.poolGamblerScores.collectAsLazyPagingItems()
    val onFilterChange = { newFilterText: String -> filterText = newFilterText }
    val onAsyncFilterChange = viewModel::search
    val pageSize = viewModel.pageSize

    Scaffold(
        topBar = { AppTopBar(title = { Text(text = stringResource(id = R.string.gambler_pool_list_title)) }) }
    ) { paddingValues ->
        PoolScoreList(
            lazyPoolGamblerScores = lazyItems,
            filterText = filterText,
            onFilterChange = onFilterChange,
            onFilterDelayedChange = onAsyncFilterChange,
            fakeItemCount = pageSize,
            detailRequested = onDetailRequested,
            modifier = Modifier
                .padding(paddingValues = paddingValues)
                .padding(all = 8.dp)
                .fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(title: @Composable () -> Unit) {
    TopAppBar(title = title)
}