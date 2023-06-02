package com.felipearpa.tyche.pool.ui.gamblerScore

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

@Composable
fun GamblerScoreListView(viewModel: GamblerScoreListViewModel) {
    var filterText by remember { mutableStateOf(emptyString()) }
    val lazyItems = viewModel.poolGamblerScores.collectAsLazyPagingItems()
    val onFilterChange = { newFilterText: String -> filterText = newFilterText }
    val onAsyncFilterChange = viewModel::search
    val pageSize = viewModel.pageSize

    GamblerScoreList(
        lazyPoolGamblerScores = lazyItems,
        loggedInGamblerId = viewModel.gamblerId,
        filterText = filterText,
        onFilterChange = onFilterChange,
        onFilterDelayedChange = onAsyncFilterChange,
        fakeItemCount = pageSize,
        modifier = Modifier
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            .fillMaxWidth()
    )
}