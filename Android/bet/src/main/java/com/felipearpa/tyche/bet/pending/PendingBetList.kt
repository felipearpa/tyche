package com.felipearpa.tyche.bet.pending

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fitInside
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
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
import androidx.compose.ui.layout.WindowInsetsRulers
import androidx.compose.ui.platform.LocalDensity
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
import com.felipearpa.tyche.bet.poolGamblerBetFakeModel
import com.felipearpa.tyche.ui.bottomUncoveredBy
import com.felipearpa.tyche.ui.exception.localizedOrDefault
import com.felipearpa.tyche.ui.lazy.Failure
import com.felipearpa.tyche.ui.lazy.RefreshableLazyPagingColumn
import com.felipearpa.tyche.ui.lazy.ViewportFillingItem
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.ui.state.MutationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate
import com.felipearpa.tyche.ui.R as SharedR

/**
 * Pending bets with editable scores.
 *
 * [contentPadding] is the screen's inset padding, which may include a bottom bar that stays at
 * the window edge while the keyboard covers it. The list's viewport ends at the keyboard top,
 * and its bottom padding keeps only the part of that bar the keyboard leaves uncovered, so the
 * bar, navigation-bar, and keyboard heights are not stacked. Ending the viewport at the keyboard
 * rather than padding its content matters: a scrollable brings a focused score back into view
 * only when its viewport shrinks past it. [modifier] must give the list a fixed size, such as
 * `fillMaxSize()`, for the keyboard fitting to apply.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PendingBetList(
    lazyPoolGamblerBets: LazyPagingItems<PoolGamblerBetModel>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    fakeItemCount: Int = 0,
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)? = null,
) {
    val listPadding = contentPadding.bottomUncoveredBy(WindowInsets.ime, LocalDensity.current)
    RefreshableLazyPagingColumn(
        modifier = modifier
            .consumeWindowInsets(contentPadding)
            .fitInside(WindowInsetsRulers.Ime.current),
        contentPadding = listPadding + PaddingValues(vertical = LocalBoxSpacing.current.medium),
        lazyPagingItems = lazyPoolGamblerBets,
        loadingContent = { pendingBetPlaceholderList(count = fakeItemCount) },
        emptyContent = { emptyContent() },
        errorContent = { error(it) },
        appendLoadingContent = { pendingBetPlaceholderItemRow() },
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
                    PendingBetItemView(
                        viewModelState = MutationState.Idle(poolGamblerBet),
                        viewState = PendingBetItemViewState.Visualization(poolGamblerBet.toPartialPoolGamblerBetModel()),
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
        pendingBetPlaceholderItemRow()
    }
}

private fun LazyListScope.emptyContent() {
    item {
        ViewportFillingItem {
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

private fun LazyListScope.pendingBetPlaceholderItemRow() {
    item {
        PendingBetItem(
            poolGamblerBet = poolGamblerBetFakeModel(),
            viewState = PendingBetItemViewState.Visualization(partialPoolGamblerBetFakeModel()),
            modifier = Modifier.pendingBetItem(),
            isPlaceholder = true,
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = LocalBoxSpacing.current.large))
    }
}

private fun LazyListScope.error(exception: Throwable) {
    item {
        ViewportFillingItem {
            Failure(
                localizedException = exception.localizedOrDefault(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = LocalBoxSpacing.current.medium),
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
