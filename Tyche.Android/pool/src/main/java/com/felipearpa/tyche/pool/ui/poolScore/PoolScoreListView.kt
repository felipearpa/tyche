package com.felipearpa.tyche.pool.ui.poolScore

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.core.emptyString
import com.felipearpa.tyche.pool.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: @Composable () -> Unit) {
    TopAppBar(title = title)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoolScoreListView(viewModel: PoolScoreListViewModel, onPoolDetailRequested: (String) -> Unit) {
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
            onPoolScoreClick = onPoolDetailRequested,
            modifier = Modifier
                .padding(paddingValues = paddingValues)
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                .fillMaxWidth()
        )
    }
}

@Preview
@Composable
fun AppTopBarPreview() {
    AppTopBar { Text(text = stringResource(id = R.string.gambler_pool_list_title)) }
}