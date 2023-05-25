package com.felipearpa.bet.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.core.emptyString

@Composable
fun PoolGamblerBetListView(viewModel: PoolGamblerBetListViewModel) {
    var filterText by remember { mutableStateOf(emptyString()) }
    val lazyItems = viewModel.poolGamblerBets.collectAsLazyPagingItems()
    val onFilterChange = { newFilterText: String -> filterText = newFilterText }
    val onAsyncFilterChange = viewModel::search
    val pageSize = viewModel.pageSize

    PoolGamblerBetList(
        lazyPoolGamblerBets = lazyItems,
        filterText = filterText,
        onFilterChange = onFilterChange,
        onFilterDelayedChange = onAsyncFilterChange,
        fakeItemCount = pageSize,
        modifier = Modifier
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            .fillMaxWidth()
    )
}