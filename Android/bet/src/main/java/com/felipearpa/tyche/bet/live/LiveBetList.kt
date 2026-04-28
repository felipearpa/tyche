package com.felipearpa.tyche.bet.live

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.foundation.time.toShortDateString
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.pending.NonEditablePendingBetItem
import com.felipearpa.tyche.bet.pending.PartialPoolGamblerBetModel
import com.felipearpa.tyche.bet.pending.PendingBetPlaceholderItem
import com.felipearpa.tyche.bet.poolGamblerBetDummyModels
import com.felipearpa.tyche.ui.lazy.RefreshableStatefulLazyColumn
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiveBetList(
    lazyBets: LazyPagingItems<PoolGamblerBetModel>,
    modifier: Modifier = Modifier,
    placeholderCount: Int = 0,
) {
    RefreshableStatefulLazyColumn(
        modifier = modifier,
        lazyPagingItems = lazyBets,
        loadingContent = { liveBetPlaceholderList(count = placeholderCount) },
        loadingContentOnConcatenate = { liveBetPlaceholderItem() },
    ) {
        val poolGamblerBetsCount = lazyBets.itemCount
        var lastMatchDate: LocalDate? = null

        repeat(poolGamblerBetsCount) { index ->
            val poolGamblerBet = lazyBets[index]!!

            if (lastMatchDate != poolGamblerBet.matchDateTime.date) {
                val localDateString = poolGamblerBet.matchDateTime.toShortDateString()
                stickyHeader(
                    key = localDateString,
                    contentType = "Header",
                ) {
                    Text(
                        text = localDateString,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.liveHeaderBetItemView(),
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
                NonEditablePendingBetItem(
                    poolGamblerBet = poolGamblerBet,
                    partialPoolGamblerBet = poolGamblerBet.toPartial(),
                    modifier = Modifier.liveBetItem(),
                )
                HorizontalDivider()
            }
        }
    }
}

private fun PoolGamblerBetModel.toPartial(): PartialPoolGamblerBetModel =
    PartialPoolGamblerBetModel(
        homeTeamBet = betScore?.homeTeamValue?.toString().orEmpty(),
        awayTeamBet = betScore?.awayTeamValue?.toString().orEmpty(),
    )

private fun LazyListScope.liveBetPlaceholderList(count: Int) {
    repeat(count) {
        liveBetPlaceholderItem()
    }
}

private fun LazyListScope.liveBetPlaceholderItem() {
    item {
        PendingBetPlaceholderItem(modifier = Modifier.liveBetItem())
        HorizontalDivider()
    }
}

@Composable
private fun Modifier.liveBetItem() =
    fillMaxWidth().padding(all = LocalBoxSpacing.current.medium)

@Composable
private fun Modifier.liveHeaderBetItemView() =
    fillMaxWidth().padding(all = LocalBoxSpacing.current.medium)

@Preview(showBackground = true)
@Composable
private fun LiveBetListPreview() {
    val items =
        MutableStateFlow(PagingData.from(poolGamblerBetDummyModels())).collectAsLazyPagingItems()
    LiveBetList(lazyBets = items, modifier = Modifier.fillMaxSize())
}
