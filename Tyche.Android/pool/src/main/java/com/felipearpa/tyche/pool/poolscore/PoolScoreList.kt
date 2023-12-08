package com.felipearpa.tyche.pool.poolscore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.felipearpa.tyche.core.emptyString
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModels
import com.felipearpa.tyche.ui.CollapsableContainer
import com.felipearpa.tyche.ui.SearchBar
import com.felipearpa.tyche.ui.lazy.RefreshableLazyColumn
import kotlinx.coroutines.flow.flowOf

@Composable
fun PoolScoreList(
    modifier: Modifier = Modifier,
    lazyPoolGamblerScores: LazyPagingItems<PoolGamblerScoreModel>,
    lazyListState: LazyListState = rememberLazyListState(),
    filterText: String,
    onFilterValueChange: (String) -> Unit,
    onDetailRequested: (String, String) -> Unit,
    fakeItemCount: Int = 0
) {
    CollapsableContainer(
        modifier = modifier,
        collapsableTop = {
            SearchBar(
                filterValue = filterText,
                onFilterValueChange = onFilterValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }
    ) {
        RefreshableLazyColumn(
            modifier = Modifier.fillMaxWidth(),
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
                contentType = lazyPoolGamblerScores.itemContentType { "PoolGamblerScore" }
            ) { index ->
                val poolGamblerScore = lazyPoolGamblerScores[index]!!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDetailRequested(
                                poolGamblerScore.poolId,
                                poolGamblerScore.gamblerId
                            )
                        }
                ) {
                    PoolScoreItem(
                        poolGamblerScore = poolGamblerScore,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    Divider(modifier = Modifier.align(Alignment.BottomCenter))
                }
            }
        }
    }
}

private fun LazyListScope.poolScoreFakeItem() {
    item {
        PoolScoreFakeItem(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        Divider()
    }
}

@Preview(showBackground = true)
@Composable
fun PoolScoreListPreview() {
    val items = flowOf(PagingData.from(poolGamblerScoreDummyModels())).collectAsLazyPagingItems()
    PoolScoreList(
        lazyPoolGamblerScores = items,
        filterText = emptyString(),
        onFilterValueChange = {},
        onDetailRequested = { _, _ -> }
    )
}