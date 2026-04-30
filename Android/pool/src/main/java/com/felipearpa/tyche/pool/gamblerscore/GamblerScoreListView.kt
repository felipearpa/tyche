package com.felipearpa.tyche.pool.gamblerscore

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModels
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun GamblerScoreListView(
    viewModel: GamblerScoreListViewModel,
    signedInGamblerId: String = viewModel.gamblerId,
    onGamblerOpen: ((poolId: String, gamblerId: String, gamblerUsername: String) -> Unit)? = null,
) {
    val lazyItems = viewModel.poolGamblerScores.collectAsLazyPagingItems()
    val pageSize = viewModel.pageSize
    GamblerScoreListView(
        lazyPoolGamblerScores = lazyItems,
        gamblerId = signedInGamblerId,
        placeholderItemCount = pageSize,
        onGamblerOpen = onGamblerOpen,
        modifier = Modifier
            .fillMaxSize()
            .padding(all = LocalBoxSpacing.current.medium),
    )
}

@Composable
private fun GamblerScoreListView(
    lazyPoolGamblerScores: LazyPagingItems<PoolGamblerScoreModel>,
    gamblerId: String,
    placeholderItemCount: Int,
    onGamblerOpen: ((poolId: String, gamblerId: String, gamblerUsername: String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    GamblerScoreList(
        lazyPoolGamblerScores = lazyPoolGamblerScores,
        loggedInGamblerId = gamblerId,
        fakeItemCount = placeholderItemCount,
        onGamblerOpen = onGamblerOpen,
        modifier = modifier,
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
        onGamblerOpen = { _, _, _ -> },
        modifier = Modifier
            .fillMaxSize()
            .padding(all = LocalBoxSpacing.current.medium),
    )
}
