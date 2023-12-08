package com.felipearpa.tyche.bet

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.core.emptyString
import com.felipearpa.tyche.core.toLocalDateString
import com.felipearpa.tyche.ui.CollapsableContainer
import com.felipearpa.tyche.ui.SearchBar
import com.felipearpa.tyche.ui.lazy.RefreshableLazyColumn
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PoolGamblerBetList(
    lazyPoolGamblerBets: LazyPagingItems<PoolGamblerBetModel>,
    filterText: String,
    onFilterValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    fakeItemCount: Int = 0
) {
    val poolGamblerBetsCount = lazyPoolGamblerBets.itemCount
    var lastMatchDate: LocalDate? = null

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
            lazyItems = lazyPoolGamblerBets,
            loadingContent = { PoolGamblerBetFakeList(count = fakeItemCount) },
            loadingContentOnConcatenate = { poolGamblerBetFakeItem() }
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
                    PoolGamblerBetItemView(viewModel = poolGamblerBetViewModel(poolGamblerBet = poolGamblerBet))
                    Divider(modifier = Modifier.padding(top = 8.dp))
                }
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

@Preview
@Composable
fun PoolGamblerBetListPreview() {
    val items = flowOf(PagingData.from(poolGamblerBetDummyModels())).collectAsLazyPagingItems()
    Surface {
        PoolGamblerBetList(
            lazyPoolGamblerBets = items,
            filterText = emptyString(),
            onFilterValueChange = {}
        )
    }
}