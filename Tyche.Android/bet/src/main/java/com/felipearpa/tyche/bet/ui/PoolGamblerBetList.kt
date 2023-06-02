package com.felipearpa.tyche.bet.ui

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.bet.poolGamblerBetViewModel
import com.felipearpa.tyche.core.emptyString
import com.felipearpa.tyche.core.toLocalDateString
import com.felipearpa.tyche.core.type.TeamScore
import com.felipearpa.tyche.core.type.Ulid
import com.felipearpa.tyche.ui.DelayedTextField
import com.felipearpa.tyche.ui.R
import com.felipearpa.tyche.ui.lazy.RefreshableLazyColumn
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PoolGamblerBetList(
    lazyPoolGamblerBets: LazyPagingItems<PoolGamblerBetModel>,
    filterText: String,
    onFilterChange: (String) -> Unit,
    onFilterDelayedChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    fakeItemCount: Int? = null
) {
    val poolGamblerBetsCount = lazyPoolGamblerBets.itemCount
    var lastMatchDate: LocalDate? = null

    RefreshableLazyColumn(
        modifier = modifier,
        lazyItems = lazyPoolGamblerBets,
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
        for (index in 0 until poolGamblerBetsCount) {
            val poolGamblerBet = lazyPoolGamblerBets.peek(index)!!
            if (lastMatchDate != poolGamblerBet.matchDateTime.toLocalDate()) {
                stickyHeader {
                    Text(
                        text = poolGamblerBet.matchDateTime.toLocalDateString(),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                lastMatchDate = poolGamblerBet.matchDateTime.toLocalDate()
            }

            item {
                PoolGamblerBetItemView(
                    viewModel = poolGamblerBetViewModel(poolGamblerBet = poolGamblerBet),
                    modifier = Modifier.fillMaxWidth()
                )
                Divider(modifier = Modifier.padding(top = 8.dp))
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
            PoolGamblerBetFakeItem(modifier = Modifier.fillMaxWidth())
            Divider(modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PoolGamblerBetListPreview() {
    val items = flowOf(
        PagingData.from(
            listOf(
                PoolGamblerBetModel(
                    poolId = Ulid.randomUlid().value,
                    gamblerId = Ulid.randomUlid().value,
                    matchId = Ulid.randomUlid().value,
                    homeTeamId = Ulid.randomUlid().value,
                    homeTeamName = "Colombia",
                    matchScore = TeamScore(2, 1),
                    betScore = TeamScore(2, 1),
                    awayTeamId = Ulid.randomUlid().value,
                    awayTeamName = "Argentina",
                    score = 10,
                    matchDateTime = LocalDateTime.now().minusDays(1)
                )
            )
        )
    ).collectAsLazyPagingItems()
    PoolGamblerBetList(
        lazyPoolGamblerBets = items,
        filterText = emptyString(),
        onFilterChange = {},
        onFilterDelayedChange = {}
    )
}