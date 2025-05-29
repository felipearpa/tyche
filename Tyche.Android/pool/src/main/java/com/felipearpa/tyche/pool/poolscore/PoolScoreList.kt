package com.felipearpa.tyche.pool.poolscore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.tooling.preview.Preview
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
import kotlinx.coroutines.flow.flowOf

@Composable
fun PoolScoreList(
    modifier: Modifier = Modifier,
    lazyPoolGamblerScores: LazyPagingItems<PoolGamblerScoreModel>,
    lazyListState: LazyListState = rememberLazyListState(),
    onPoolClick: (String, String) -> Unit,
    fakeItemCount: Int = 50
) {
    RefreshableStatefulLazyColumn(
        modifier = modifier,
        lazyItems = lazyPoolGamblerScores,
        state = lazyListState,
        loadingContent = { PoolScoreFakeList(count = fakeItemCount) },
        loadingContentOnConcatenate = { poolScoreFakeItem() }
    ) {
        items(
            count = lazyPoolGamblerScores.itemCount,
            key = lazyPoolGamblerScores.itemKey { poolGamblerScore ->
                Pair(
                    poolGamblerScore.poolId,
                    poolGamblerScore.gamblerId
                )
            },
            contentType = lazyPoolGamblerScores.itemContentType { "PoolScore" }
        ) { index ->
            val poolGamblerScore = lazyPoolGamblerScores[index]!!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onPoolClick(poolGamblerScore.poolId, poolGamblerScore.gamblerId)
                    }
            ) {
                PoolScoreItem(
                    poolGamblerScore = poolGamblerScore,
                    modifier = Modifier.poolScoreItem()
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun PoolScoreFakeList(count: Int) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(count) {
            item {
                PoolScoreFakeItem(modifier = Modifier.poolScoreItem())
                HorizontalDivider()
            }
        }
    }
}

private fun LazyListScope.poolScoreFakeItem() {
    item {
        PoolScoreFakeItem(modifier = Modifier.poolScoreItem())
        HorizontalDivider()
    }
}

private fun Modifier.poolScoreItem() = composed {
    this
        .fillMaxWidth()
        .padding(all = LocalBoxSpacing.current.medium)
}

@Preview(showBackground = true)
@Composable
private fun PoolScoreListPreview() {
    val items = flowOf(PagingData.from(poolGamblerScoreDummyModels())).collectAsLazyPagingItems()
    PoolScoreList(
        lazyPoolGamblerScores = items,
        onPoolClick = { _, _ -> },
        modifier = Modifier.fillMaxSize()
    )
}

@Preview(showBackground = true)
@Composable
private fun PoolScoreFakeListPreview() {
    PoolScoreFakeList(count = 50)
}