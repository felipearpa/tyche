package com.felipearpa.tyche.bet.pending

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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.foundation.time.toShortDateString
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.poolGamblerBetDummyModels
import com.felipearpa.tyche.ui.lazy.RefreshableStatefulLazyColumn
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PendingBetList(
    lazyPoolGamblerBets: LazyPagingItems<PoolGamblerBetModel>,
    modifier: Modifier = Modifier,
    fakeItemCount: Int = 0,
) {
    RefreshableStatefulLazyColumn(
        modifier = modifier,
        lazyItems = lazyPoolGamblerBets,
        loadingContent = { PendingBetFakeList(count = fakeItemCount) },
        loadingContentOnConcatenate = { pendingBetFakeItem() },
    ) {
        val poolGamblerBetsCount = lazyPoolGamblerBets.itemCount
        var lastMatchDate: LocalDate? = null

        repeat(poolGamblerBetsCount) { index ->
            val poolGamblerBet = lazyPoolGamblerBets[index]!!

            if (lastMatchDate != poolGamblerBet.matchDateTime.date) {
                val localDateString = poolGamblerBet.matchDateTime.toShortDateString()
                stickyHeader(
                    key = localDateString,
                    contentType = "Header",
                ) {
                    Text(
                        text = localDateString,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.pendingHeaderBetItemView(),
                    )
                }
                lastMatchDate = poolGamblerBet.matchDateTime.date
            }

            item(
                key = Triple(
                    poolGamblerBet.poolId,
                    poolGamblerBet.gamblerId,
                    poolGamblerBet.matchId,
                ),
                contentType = "PoolGamblerBet",
            ) {
                if (LocalInspectionMode.current) {
                    PendingBetItem(
                        poolGamblerBet = poolGamblerBet,
                        viewState = PendingBetItemViewState.dummyVisualization(),
                        modifier = Modifier.pendingBetItem(),
                    )
                } else {
                    PendingBetItemView(
                        viewModel = pendingBetViewModel(poolGamblerBet = poolGamblerBet),
                        modifier = Modifier.pendingBetItem(),
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = LocalBoxSpacing.current.large))
            }
        }
    }
}

private fun LazyListScope.pendingBetFakeItem() {
    item {
        PendingBetFakeItem(modifier = Modifier.fillMaxWidth())
        HorizontalDivider(modifier = Modifier.padding(horizontal = LocalBoxSpacing.current.large))
    }
}

@Composable
private fun PendingBetFakeList(count: Int) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        repeat(count) {
            item {
                PendingBetFakeItem(modifier = Modifier.pendingBetItem())
                HorizontalDivider(modifier = Modifier.padding(horizontal = LocalBoxSpacing.current.large))
            }
        }
    }
}

private fun Modifier.pendingBetItem() = composed {
    this
        .fillMaxWidth()
        .padding(horizontal = LocalBoxSpacing.current.large)
        .padding(vertical = LocalBoxSpacing.current.medium)
}

private fun Modifier.pendingHeaderBetItemView() = composed {
    this
        .fillMaxWidth()
        .padding(horizontal = LocalBoxSpacing.current.medium)
        .padding(top = LocalBoxSpacing.current.medium)
}

@Preview(showBackground = true)
@Composable
private fun PendingBetListPreview() {
    val items = flowOf(PagingData.from(poolGamblerBetDummyModels())).collectAsLazyPagingItems()
    TycheTheme {
        PendingBetList(lazyPoolGamblerBets = items, modifier = Modifier.fillMaxSize())
    }
}

@Preview(showBackground = true)
@Composable
private fun PendingBetFakeListPreview() {
    PendingBetFakeList(count = 50)
}
