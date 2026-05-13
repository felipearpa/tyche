package com.felipearpa.tyche.bet.pending

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.foundation.time.toShortDateString
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.isPending
import com.felipearpa.tyche.bet.poolGamblerBetDummyModels
import com.felipearpa.tyche.ui.exception.localizedOrDefault
import com.felipearpa.tyche.ui.lazy.Failure
import com.felipearpa.tyche.ui.lazy.RefreshableLazyPagingColumn
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate
import com.felipearpa.tyche.ui.R as SharedR

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PendingBetList(
    lazyPoolGamblerBets: LazyPagingItems<PoolGamblerBetModel>,
    modifier: Modifier = Modifier,
    fakeItemCount: Int = 0,
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)? = null,
) {
    RefreshableLazyPagingColumn(
        modifier = modifier,
        lazyPagingItems = lazyPoolGamblerBets,
        loadingContent = { pendingBetPlaceholderList(count = fakeItemCount) },
        emptyContent = { emptyContent() },
        errorContent = { error(it) },
        appendLoadingContent = { item { pendingBetPlaceholderItemRow() } },
    ) {
        val poolGamblerBetsCount = lazyPoolGamblerBets.itemCount
        var lastMatchDate: LocalDate? = null

        repeat(poolGamblerBetsCount) { index ->
            val poolGamblerBet = lazyPoolGamblerBets[index] ?: return@repeat

            if (lastMatchDate != poolGamblerBet.matchDateTime.date) {
                val localDateString = poolGamblerBet.matchDateTime.toShortDateString()
                val isFirstHeader = lastMatchDate == null
                stickyHeader(
                    key = localDateString,
                    contentType = "Header",
                ) {
                    Text(
                        text = localDateString,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.pendingHeaderBetItemView(isFirst = isFirstHeader),
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
                        if (onMatchOpen != null && !poolGamblerBet.isPending) base.clickable {
                            onMatchOpen(
                                poolGamblerBet,
                            )
                        } else base
                    }
                    .pendingBetItem()

                if (LocalInspectionMode.current) {
                    PendingBetItem(
                        poolGamblerBet = poolGamblerBet,
                        viewState = PendingBetItemViewState.dummyVisualization(),
                        modifier = itemModifier,
                    )
                } else {
                    PendingBetItemView(
                        viewModel = pendingBetViewModel(poolGamblerBet = poolGamblerBet),
                        poolGamblerBet = poolGamblerBet,
                        modifier = itemModifier,
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = LocalBoxSpacing.current.large))
            }
        }
    }
}

private fun LazyListScope.pendingBetPlaceholderList(count: Int) {
    repeat(count) {
        item { pendingBetPlaceholderItemRow() }
    }
}

private fun LazyListScope.emptyContent() {
    item {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillParentMaxSize(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    painter = painterResource(id = SharedR.drawable.ic_sentiment_sad),
                    contentDescription = "",
                    modifier = Modifier.size(iconSize),
                )

                Text(
                    text = stringResource(id = SharedR.string.empty_list_message),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun pendingBetPlaceholderItemRow() {
    PendingBetPlaceholderItem(modifier = Modifier.pendingBetItem())
    HorizontalDivider(modifier = Modifier.padding(horizontal = LocalBoxSpacing.current.large))
}

private fun LazyListScope.error(exception: Throwable) {
    item {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillParentMaxSize()
                .padding(all = LocalBoxSpacing.current.medium),
        ) {
            Failure(
                localizedException = exception.localizedOrDefault(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Modifier.pendingBetItem() =
    fillMaxWidth()
        .padding(horizontal = LocalBoxSpacing.current.large)
        .padding(vertical = LocalBoxSpacing.current.medium)

@Composable
private fun Modifier.pendingHeaderBetItemView(isFirst: Boolean) =
    fillMaxWidth()
        .padding(horizontal = LocalBoxSpacing.current.medium)
        .padding(
            top = if (isFirst) LocalBoxSpacing.current.medium
            else LocalBoxSpacing.current.medium + LocalBoxSpacing.current.medium,
        )
        .padding(bottom = LocalBoxSpacing.current.medium)

private val iconSize = 64.dp

@PreviewLightDark
@Composable
private fun PendingBetListPreview() {
    val items =
        MutableStateFlow(PagingData.from(poolGamblerBetDummyModels())).collectAsLazyPagingItems()
    TycheTheme {
        Surface {
            PendingBetList(lazyPoolGamblerBets = items, modifier = Modifier.fillMaxSize())
        }
    }
}

@PreviewLightDark
@Composable
private fun PendingBetPlaceholderListPreview() {
    TycheTheme {
        Surface {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                pendingBetPlaceholderList(count = 50)
            }
        }
    }
}
