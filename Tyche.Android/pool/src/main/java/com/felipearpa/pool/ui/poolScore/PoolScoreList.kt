package com.felipearpa.pool.ui.poolScore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.felipearpa.appUi.R
import com.felipearpa.appUi.lazy.RefreshableLazyColumn
import com.felipearpa.core.emptyString
import com.felipearpa.core.type.Ulid
import com.felipearpa.pool.ui.PoolGamblerScoreModel
import com.felipearpa.ui.DelayedTextField
import kotlinx.coroutines.flow.flowOf

@Composable
fun PoolScoreList(
    lazyPoolGamblerScores: LazyPagingItems<PoolGamblerScoreModel>,
    filterText: String,
    onFilterChange: (String) -> Unit,
    onFilterDelayedChange: (String) -> Unit,
    onPoolScoreClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    fakeItemCount: Int? = null
) {
    RefreshableLazyColumn(
        modifier = modifier,
        lazyItems = lazyPoolGamblerScores,
        topContent = {
            topContent(
                filterValue = filterText,
                onFilterValueChange = onFilterChange,
                onFilterValueDelayedChange = onFilterDelayedChange
            )
        },
        loadingContent = fakeItemCount?.let { count ->
            { loadingContent(count = count) }
        }
    ) {
        items(lazyPoolGamblerScores) { poolGamblerScore ->
            poolGamblerScore?.let { nonNullablePoolScore ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPoolScoreClick(nonNullablePoolScore.poolId) }) {
                    PoolScoreItem(
                        poolGamblerScore = nonNullablePoolScore,
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

private fun LazyListScope.topContent(
    filterValue: String,
    onFilterValueChange: (String) -> Unit,
    onFilterValueDelayedChange: (String) -> Unit
) {
    item {
        DelayedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = filterValue,
            onValueChange = onFilterValueChange,
            onDelayedValueChange = onFilterValueDelayedChange
        ) {
            Text(text = stringResource(id = R.string.searching_label))
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun LazyListScope.loadingContent(count: Int) {
    for (i in 1..count) {
        item {
            PoolScoreFakeItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            Divider()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PoolScoreListPreview() {
    val items = flowOf(
        PagingData.from(
            listOf(
                PoolGamblerScoreModel(
                    poolId = Ulid.randomUlid().value,
                    poolLayoutId = Ulid.randomUlid().value,
                    poolName = "Tyche American Cup YYYY",
                    gamblerId = Ulid.randomUlid().value,
                    gamblerUsername = "user-tyche",
                    currentPosition = 1,
                    beforePosition = 2,
                    score = 10
                )
            )
        )
    ).collectAsLazyPagingItems()
    PoolScoreList(
        lazyPoolGamblerScores = items,
        filterText = emptyString(),
        onFilterChange = {},
        onFilterDelayedChange = {},
        onPoolScoreClick = {}
    )
}