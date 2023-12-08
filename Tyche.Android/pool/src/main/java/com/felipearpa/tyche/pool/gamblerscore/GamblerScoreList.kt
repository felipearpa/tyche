package com.felipearpa.tyche.pool.gamblerscore

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
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
fun GamblerScoreList(
    lazyPoolGamblerScores: LazyPagingItems<PoolGamblerScoreModel>,
    loggedInGamblerId: String,
    filterText: String,
    onFilterValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
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
            loadingContent = { GamblerScoreFakeList(count = fakeItemCount) },
            loadingContentOnConcatenate = { gamblerScoreFakeItem() }
        ) {
            items(
                count = lazyPoolGamblerScores.itemCount,
                key = lazyPoolGamblerScores.itemKey(key = { poolGamblerScore ->
                    Pair(
                        poolGamblerScore.poolId,
                        poolGamblerScore.gamblerId
                    )
                }),
                contentType = lazyPoolGamblerScores.itemContentType { "PoolGamblerScore" }
            ) { index ->
                val item = lazyPoolGamblerScores[index]
                GamblerScoreItem(
                    poolGamblerScore = item!!,
                    isLoggedIn = item.gamblerId == loggedInGamblerId,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                Divider()
            }
        }
    }
}

private fun LazyListScope.gamblerScoreFakeItem() {
    item {
        GamblerScoreFakeItem(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        Divider()
    }
}

@Preview(showBackground = true)
@Composable
fun GamblerScoreListPreview() {
    val items = flowOf(PagingData.from(poolGamblerScoreDummyModels())).collectAsLazyPagingItems()
    GamblerScoreList(
        lazyPoolGamblerScores = items,
        loggedInGamblerId = "X".repeat(15),
        filterText = emptyString(),
        onFilterValueChange = {}
    )
}