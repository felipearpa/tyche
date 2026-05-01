package com.felipearpa.tyche.bet.timeline

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.foundation.time.toShortDateString
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.pending.PendingBetPlaceholderItem
import com.felipearpa.tyche.bet.poolGamblerBetDummyModels
import com.felipearpa.tyche.ui.lazy.RefreshableStatefulLazyColumn
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BetTimelineList(
    lazyBets: LazyPagingItems<PoolGamblerBetModel>,
    modifier: Modifier = Modifier,
    placeholderCount: Int = 0,
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)? = null,
) {
    RefreshableStatefulLazyColumn(
        modifier = modifier,
        lazyPagingItems = lazyBets,
        loadingContent = { betTimelinePlaceholderList(count = placeholderCount) },
        loadingContentOnConcatenate = { betTimelinePlaceholderItem() },
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
                        modifier = Modifier.betTimelineHeaderItem(),
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
                val itemModifier = Modifier
                    .let { base ->
                        if (onMatchOpen != null) base.clickable { onMatchOpen(poolGamblerBet) } else base
                    }
                    .betTimelineItem()

                BetTimeLineItem(
                    bet = poolGamblerBet,
                    modifier = itemModifier,
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = LocalBoxSpacing.current.large))
            }
        }
    }
}

private fun LazyListScope.betTimelinePlaceholderList(count: Int) {
    repeat(count) {
        betTimelinePlaceholderItem()
    }
}

private fun LazyListScope.betTimelinePlaceholderItem() {
    item {
        PendingBetPlaceholderItem(modifier = Modifier.betTimelineItem())
        HorizontalDivider(modifier = Modifier.padding(horizontal = LocalBoxSpacing.current.large))
    }
}

@Composable
private fun Modifier.betTimelineItem() =
    fillMaxWidth()
        .padding(horizontal = LocalBoxSpacing.current.large)
        .padding(vertical = LocalBoxSpacing.current.medium)

@Composable
private fun Modifier.betTimelineHeaderItem() =
    fillMaxWidth()
        .padding(top = LocalBoxSpacing.current.large)

@PreviewLightDark
@Composable
private fun BetTimelineListPreview() {
    val items = MutableStateFlow(PagingData.from(poolGamblerBetDummyModels())).collectAsLazyPagingItems()
    TycheTheme {
        Surface {
            BetTimelineList(
                lazyBets = items,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
