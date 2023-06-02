package com.felipearpa.tyche.pool.ui.gamblerScore

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.felipearpa.tyche.core.emptyString
import com.felipearpa.tyche.core.type.Ulid
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.pool.ui.PoolGamblerScoreModel
import com.felipearpa.tyche.ui.DelayedTextField
import com.felipearpa.tyche.ui.lazy.RefreshableLazyColumn
import kotlinx.coroutines.flow.flowOf

@Composable
fun GamblerScoreList(
    lazyPoolGamblerScores: LazyPagingItems<PoolGamblerScoreModel>,
    loggedInGamblerId: String,
    filterText: String,
    onFilterChange: (String) -> Unit,
    onFilterDelayedChange: (String) -> Unit,
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
            GamblerScoreItem(
                poolGamblerScore = poolGamblerScore!!,
                isLoggedIn = poolGamblerScore.gamblerId == loggedInGamblerId,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            Divider()
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
            GamblerScoreFakeItem(
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
fun GamblerScoreListPreview() {
    val items = flowOf(
        PagingData.from(
            listOf(
                PoolGamblerScoreModel(
                    poolId = Ulid.randomUlid().value,
                    poolLayoutId = Ulid.randomUlid().value,
                    poolName = "Tyche American Cup YYYY",
                    gamblerId = Ulid.randomUlid().toString(),
                    gamblerUsername = "user-tyche",
                    currentPosition = 1,
                    beforePosition = 2,
                    score = 10
                )
            )
        )
    ).collectAsLazyPagingItems()
    GamblerScoreList(
        lazyPoolGamblerScores = items,
        loggedInGamblerId = Ulid.randomUlid().toString(),
        filterText = emptyString(),
        onFilterChange = {},
        onFilterDelayedChange = {}
    )
}