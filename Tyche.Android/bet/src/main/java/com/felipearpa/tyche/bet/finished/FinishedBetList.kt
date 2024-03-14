package com.felipearpa.tyche.bet.finished

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.bet.PoolGamblerBetFakeItem
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.poolGamblerBetDummyModels
import com.felipearpa.tyche.core.toLocalDateString
import com.felipearpa.tyche.ui.lazy.StatefulRefreshableLazyColumn
import com.felipearpa.tyche.ui.theme.boxSpacing
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FinishedBetList(
    lazyPoolGamblerBets: LazyPagingItems<PoolGamblerBetModel>,
    modifier: Modifier = Modifier,
    fakeItemCount: Int = 0
) {
    val poolGamblerBetsCount = lazyPoolGamblerBets.itemCount

    StatefulRefreshableLazyColumn(
        modifier = modifier,
        lazyItems = lazyPoolGamblerBets,
        loadingContent = { FinishedPoolGamblerBetFakeList(count = fakeItemCount) },
        loadingContentOnConcatenate = { finishedPoolGamblerBetFakeItem() }
    ) {
        var lastMatchDate: LocalDate? = null

        repeat(poolGamblerBetsCount) { index ->
            val poolGamblerBet = lazyPoolGamblerBets[index]!!
            if (lastMatchDate != poolGamblerBet.matchDateTime.toLocalDate()) {
                stickyHeader {
                    Text(
                        text = poolGamblerBet.matchDateTime.toLocalDateString(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MaterialTheme.boxSpacing.small)
                    )
                }
                lastMatchDate = poolGamblerBet.matchDateTime.toLocalDate()
            }

            item {
                FinishedBetItem(
                    poolGamblerBet = poolGamblerBet,
                    modifier = Modifier.finishedPoolGamblerBetItem()
                )
                HorizontalDivider()
            }
        }
    }
}

private fun LazyListScope.finishedPoolGamblerBetFakeItem() {
    item {
        PoolGamblerBetFakeItem(modifier = Modifier.fillMaxWidth())
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun FinishedPoolGamblerBetFakeList(count: Int) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        repeat(count) {
            item {
                PoolGamblerBetFakeItem(modifier = Modifier.finishedPoolGamblerBetItem())
                HorizontalDivider()
            }
        }
    }
}

private fun Modifier.finishedPoolGamblerBetItem() = composed {
    this
        .fillMaxWidth()
        .padding(horizontal = MaterialTheme.boxSpacing.medium)
        .padding(vertical = MaterialTheme.boxSpacing.small)
}

@Preview(showBackground = true)
@Composable
private fun FinishedBetListPreview() {
    val items = flowOf(PagingData.from(poolGamblerBetDummyModels())).collectAsLazyPagingItems()
    FinishedBetList(lazyPoolGamblerBets = items)
}

@Preview(showBackground = true)
@Composable
private fun FinishedBetFakeListPreview() {
    FinishedPoolGamblerBetFakeList(count = 10)
}