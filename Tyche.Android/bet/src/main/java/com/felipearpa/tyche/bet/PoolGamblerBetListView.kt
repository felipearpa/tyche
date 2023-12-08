package com.felipearpa.tyche.bet

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
import com.felipearpa.tyche.core.emptyString
import com.felipearpa.tyche.ui.useDebounce

@Composable
fun PoolGamblerBetListView(viewModel: PoolGamblerBetListViewModel) {
    var filterText by remember { mutableStateOf(emptyString()) }
    val lazyItems = viewModel.poolGamblerBets.collectAsLazyPagingItems()
    val onFilterChange = { newFilterText: String -> filterText = newFilterText }
    val pageSize = viewModel.pageSize

    filterText.useDebounce { newFilterText -> viewModel.search(newFilterText) }

    PoolGamblerBetList(
        lazyPoolGamblerBets = lazyItems,
        filterText = filterText,
        onFilterValueChange = onFilterChange,
        fakeItemCount = pageSize,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
    )
}