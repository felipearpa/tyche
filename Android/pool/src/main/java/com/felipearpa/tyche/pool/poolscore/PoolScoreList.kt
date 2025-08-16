package com.felipearpa.tyche.pool.poolscore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModels
import com.felipearpa.tyche.ui.lazy.RefreshableStatefulLazyColumn
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun PoolScoreList(
    modifier: Modifier = Modifier,
    lazyPoolGamblerScores: LazyPagingItems<PoolGamblerScoreModel>,
    lazyListState: LazyListState = rememberLazyListState(),
    onPoolOpen: (poolId: String, gamblerId: String) -> Unit,
    onPoolJoin: (poolId: String) -> Unit,
    fakeItemCount: Int = 50,
) {
    RefreshableStatefulLazyColumn(
        modifier = modifier,
        lazyItems = lazyPoolGamblerScores,
        state = lazyListState,
        loadingContent = { PoolScoreFakeList(count = fakeItemCount) },
        loadingContentOnConcatenate = { poolScoreFakeItem() },
    ) {
        items(
            count = lazyPoolGamblerScores.itemCount,
            key = lazyPoolGamblerScores.itemKey { poolGamblerScore ->
                Pair(
                    poolGamblerScore.poolId,
                    poolGamblerScore.gamblerId,
                )
            },
            contentType = lazyPoolGamblerScores.itemContentType { "PoolScore" },
        ) { index ->
            val poolGamblerScore = lazyPoolGamblerScores[index] ?: return@items

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onPoolOpen(poolGamblerScore.poolId, poolGamblerScore.gamblerId)
                    },
            ) {
                PoolScoreItem(
                    poolGamblerScore = poolGamblerScore,
                    onJoin = { onPoolJoin(poolGamblerScore.poolId) },
                    modifier = Modifier.fillMaxWidth(),
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun PoolScoreFakeList(count: Int) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        repeat(count) {
            item {
                PoolScoreFakeItem(modifier = Modifier.fillMaxWidth())
                HorizontalDivider()
            }
        }
    }
}

private fun LazyListScope.poolScoreFakeItem() {
    item {
        PoolScoreFakeItem(modifier = Modifier.fillMaxWidth())
        HorizontalDivider()
    }
}

@Preview(showBackground = true)
@Composable
private fun PoolScoreListPreview() {
    val items =
        MutableStateFlow(PagingData.from(poolGamblerScoreDummyModels())).collectAsLazyPagingItems()
    PoolScoreList(
        lazyPoolGamblerScores = items,
        onPoolOpen = { _, _ -> },
        onPoolJoin = {},
        modifier = Modifier.fillMaxSize(),
    )
}

@Preview(showBackground = true)
@Composable
private fun PoolScoreFakeListPreview() {
    PoolScoreFakeList(count = 50)
}
