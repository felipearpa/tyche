package com.felipearpa.tyche.pool.gamblerscore

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.compose.collectAsLazyPagingItems

@Composable
fun GamblerScoreListView(viewModel: GamblerScoreListViewModel) {
    val lazyItems = viewModel.poolGamblerScores.collectAsLazyPagingItems()
    val pageSize = viewModel.pageSize

    GamblerScoreList(
        lazyPoolGamblerScores = lazyItems,
        loggedInGamblerId = viewModel.gamblerId,
        fakeItemCount = pageSize,
        modifier = Modifier.fillMaxSize()
    )
}