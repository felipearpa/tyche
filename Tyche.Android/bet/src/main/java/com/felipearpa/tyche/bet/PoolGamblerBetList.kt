package com.felipearpa.tyche.bet

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.core.toLocalDateString
import com.felipearpa.tyche.ui.lazy.RefreshableLazyColumn
import com.felipearpa.tyche.ui.theme.boxSpacing
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PoolGamblerBetList(
    lazyPoolGamblerBets: LazyPagingItems<PoolGamblerBetModel>,
    modifier: Modifier = Modifier,
    fakeItemCount: Int = 0
) {
    val poolGamblerBetsCount = lazyPoolGamblerBets.itemCount
    var lastMatchDate: LocalDate? = null

    RefreshableLazyColumn(
        modifier = modifier,
        lazyItems = lazyPoolGamblerBets,
        loadingContent = { PoolGamblerBetFakeList(count = fakeItemCount) },
        loadingContentOnConcatenate = { poolGamblerBetFakeItem() }
    ) {
        repeat(poolGamblerBetsCount) { index ->
            val poolGamblerBet = lazyPoolGamblerBets.peek(index)!!
            if (lastMatchDate != poolGamblerBet.matchDateTime.toLocalDate()) {
                stickyHeader {
                    Text(
                        text = poolGamblerBet.matchDateTime.toLocalDateString(),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MaterialTheme.boxSpacing.small)
                    )
                }
                lastMatchDate = poolGamblerBet.matchDateTime.toLocalDate()
            }

            item {
                PoolGamblerBetItemView(
                    viewModel = poolGamblerBetViewModel(poolGamblerBet = poolGamblerBet),
                    modifier = Modifier.poolGamblerBetItem()
                )
                Divider()
            }
        }
    }
}

private fun LazyListScope.poolGamblerBetFakeItem() {
    item {
        PoolGamblerBetFakeItem(modifier = Modifier.fillMaxWidth())
        Divider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun PoolGamblerBetFakeList(count: Int) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        repeat(count) {
            item {
                PoolGamblerBetFakeItem(modifier = Modifier.poolGamblerBetItem())
                Divider()
            }
        }
    }
}

private fun Modifier.poolGamblerBetItem() = composed {
    Modifier
        .fillMaxWidth()
        .padding(horizontal = MaterialTheme.boxSpacing.medium)
        .padding(vertical = MaterialTheme.boxSpacing.small)
}

@Preview(showBackground = true)
@Composable
private fun PoolGamblerBetListPreview() {
    val items = flowOf(PagingData.from(poolGamblerBetDummyModels())).collectAsLazyPagingItems()
    PoolGamblerBetList(lazyPoolGamblerBets = items)
}

@Preview(showBackground = true)
@Composable
private fun PoolGamblerBetFakeListPreview() {
    PoolGamblerBetFakeList(count = 10)
}
