package com.felipearpa.tyche.bet.match

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.poolGamblerBetDummyModels
import com.felipearpa.tyche.ui.lazy.RefreshableStatefulLazyColumn
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun MatchBetList(
    lazyPoolGamblerBets: LazyPagingItems<PoolGamblerBetModel>,
    modifier: Modifier = Modifier,
    fakeItemCount: Int = 0,
) {
    RefreshableStatefulLazyColumn(
        modifier = modifier,
        lazyPagingItems = lazyPoolGamblerBets,
        loadingContent = { matchGamblerBetPlaceholderList(count = fakeItemCount) },
        loadingContentOnConcatenate = { matchGamblerBetPlaceholderItem() },
    ) {
        items(
            count = lazyPoolGamblerBets.itemCount,
            key = lazyPoolGamblerBets.itemKey { Pair(it.poolId, it.gamblerId) },
            contentType = lazyPoolGamblerBets.itemContentType { "MatchGamblerBet" },
        ) { index ->
            val item = lazyPoolGamblerBets[index]!!
            MatchGamblerBetItem(
                poolGamblerBet = item,
                modifier = Modifier.matchGamblerBetItem(),
            )
            HorizontalDivider()
        }
    }
}

private fun LazyListScope.matchGamblerBetPlaceholderList(count: Int) {
    repeat(count) {
        matchGamblerBetPlaceholderItem()
    }
}

private fun LazyListScope.matchGamblerBetPlaceholderItem() {
    item {
        MatchGamblerBetPlaceholderItem(modifier = Modifier.matchGamblerBetItem())
        HorizontalDivider()
    }
}

@Composable
private fun Modifier.matchGamblerBetItem() =
    fillMaxWidth().padding(all = LocalBoxSpacing.current.medium)

@PreviewLightDark
@Composable
private fun MatchBetListPreview() {
    val items =
        MutableStateFlow(PagingData.from(poolGamblerBetDummyModels())).collectAsLazyPagingItems()
    TycheTheme {
        Surface {
            MatchBetList(
                lazyPoolGamblerBets = items,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
