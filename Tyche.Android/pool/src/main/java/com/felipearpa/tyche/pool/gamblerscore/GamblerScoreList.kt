package com.felipearpa.tyche.pool.gamblerscore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModels
import com.felipearpa.tyche.ui.lazy.RefreshableStatefulLazyColumn
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.flow.flowOf

@Composable
fun GamblerScoreList(
    lazyPoolGamblerScores: LazyPagingItems<PoolGamblerScoreModel>,
    loggedInGamblerId: String,
    modifier: Modifier = Modifier,
    fakeItemCount: Int = 0,
) {
    RefreshableStatefulLazyColumn(
        modifier = modifier,
        lazyItems = lazyPoolGamblerScores,
        loadingContent = { GamblerScoreFakeList(count = fakeItemCount) },
        loadingContentOnConcatenate = { gamblerScoreFakeItem() },
    ) {
        items(
            count = lazyPoolGamblerScores.itemCount,
            key = lazyPoolGamblerScores.itemKey(
                key = { poolGamblerScore ->
                    Pair(
                        poolGamblerScore.poolId,
                        poolGamblerScore.gamblerId,
                    )
                },
            ),
            contentType = lazyPoolGamblerScores.itemContentType { "PoolGamblerScore" },
        ) { index ->
            val item = lazyPoolGamblerScores[index]
            GamblerScoreItem(
                poolGamblerScore = item!!,
                isLoggedIn = item.gamblerId == loggedInGamblerId,
                modifier = Modifier.gamblerScoreItem(),
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun GamblerScoreFakeList(count: Int) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(count) {
            item {
                GamblerScoreFakeItem(modifier = Modifier.gamblerScoreItem())
                HorizontalDivider()
            }
        }
    }
}

private fun LazyListScope.gamblerScoreFakeItem() {
    item {
        GamblerScoreFakeItem(modifier = Modifier.gamblerScoreItem())
        HorizontalDivider()
    }
}

private fun Modifier.gamblerScoreItem() = composed {
    this
        .fillMaxWidth()
        .padding(all = LocalBoxSpacing.current.medium)
}

@PreviewLightDark
@Composable
private fun GamblerScoreListPreview() {
    val items = flowOf(PagingData.from(poolGamblerScoreDummyModels())).collectAsLazyPagingItems()

    TycheTheme {
        Surface {
            GamblerScoreList(
                lazyPoolGamblerScores = items,
                loggedInGamblerId = "gambler001",
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GamblerScoreFakeListPreview() {
    GamblerScoreFakeList(count = 50)
}
