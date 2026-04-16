package com.felipearpa.tyche.pool.gamblerscore

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModels
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun GamblerScoreListView(viewModel: GamblerScoreListViewModel) {
    val lazyItems = viewModel.poolGamblerScores.collectAsLazyPagingItems()
    val pageSize = viewModel.pageSize

    GamblerScoreList(
        lazyPoolGamblerScores = lazyItems,
        loggedInGamblerId = viewModel.gamblerId,
        fakeItemCount = pageSize,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun GamblerScoreListView(
    lazyPoolGamblerScores: LazyPagingItems<PoolGamblerScoreModel>,
    gamblerId: String,
    placeholderItemCount: Int,
) {
    GamblerScoreList(
        lazyPoolGamblerScores = lazyPoolGamblerScores,
        loggedInGamblerId = gamblerId,
        fakeItemCount = placeholderItemCount,
        modifier = Modifier.fillMaxSize(),
    )
}

@Preview(showBackground = true)
@Composable
private fun GamblerScoreListViewPreview() {
    val items = MutableStateFlow(PagingData.from(poolGamblerScoreDummyModels())).collectAsLazyPagingItems()
    GamblerScoreListView(
        lazyPoolGamblerScores = items,
        gamblerId = "gambler001",
        placeholderItemCount = 50,
    )
}
