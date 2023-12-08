package com.felipearpa.tyche.pool.gamblerscore

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
fun GamblerScoreListView(viewModel: GamblerScoreListViewModel) {
    var filterText by remember { mutableStateOf(emptyString()) }
    val lazyItems = viewModel.poolGamblerScores.collectAsLazyPagingItems()
    val onFilterChange = { newFilterText: String -> filterText = newFilterText }
    val pageSize = viewModel.pageSize

    filterText.useDebounce { newFilterText -> viewModel.search(newFilterText) }

    GamblerScoreList(
        lazyPoolGamblerScores = lazyItems,
        loggedInGamblerId = viewModel.gamblerId,
        filterText = filterText,
        onFilterValueChange = onFilterChange,
        fakeItemCount = pageSize,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
    )
}