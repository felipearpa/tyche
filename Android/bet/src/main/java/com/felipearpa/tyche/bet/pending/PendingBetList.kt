package com.felipearpa.tyche.bet.pending

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fitInside
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.WindowInsetsRulers
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.isPending
import com.felipearpa.tyche.bet.poolGamblerBetPendingDummyModels
import com.felipearpa.tyche.ui.bottomUncoveredBy
import com.felipearpa.tyche.ui.excludingBottom
import com.felipearpa.tyche.ui.onlyBottom
import com.felipearpa.tyche.ui.lazy.RefreshableLazyPagingColumn
import com.felipearpa.tyche.ui.lazy.viewportFillingItem
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.ui.state.MutationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import com.felipearpa.tyche.ui.R as SharedR

/**
 * The Bets tab's list: the filter row, then the loaded matches the filter keeps as cards under
 * sticky kickoff-day headers named relative to [now].
 *
 * Cards the filter hides keep their row holders, so drafts and in-flight saves survive, but emit
 * no list item, so a long list holds items only for the cards it shows. A one-pixel trigger after
 * the last shown item reads the last loaded index when it comes on screen, so the pager keeps
 * loading pages behind hidden cards. A filter that keeps no loaded match shows its message only
 * once no page remains; until then the append placeholder shows while the next page loads.
 *
 * [rowHolder] supplies each match's saving row; without it, as in previews, cards show the
 * feed's predictions read-only.
 *
 * [contentPadding] is the screen's inset padding. Its top and sides inset the list itself, so a
 * pinned day header sits below the top app bar rather than behind it; its bottom may include a
 * bottom bar that stays at the window edge while the keyboard covers it. The list's viewport ends at the keyboard top,
 * and its bottom padding keeps only the part of that bar the keyboard leaves uncovered, so the
 * bar, navigation-bar, and keyboard heights are not stacked. Ending the viewport at the keyboard
 * rather than padding its content matters: a scrollable brings a focused card back into view
 * only when its viewport shrinks past it. [modifier] must give the list a fixed size, such as
 * `fillMaxSize()`, for the keyboard fitting to apply.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun PendingBetList(
    lazyPoolGamblerBets: LazyPagingItems<PoolGamblerBetModel>,
    listState: PendingBetListState,
    now: Instant,
    onSelectFilter: (PendingBetFilter) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    fakeItemCount: Int = 0,
    rowHolder: ((PoolGamblerBetModel) -> PendingBetRowHolder)? = null,
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)? = null,
) {
    val listPadding = contentPadding.onlyBottom().bottomUncoveredBy(WindowInsets.ime, LocalDensity.current)
    val items = lazyPoolGamblerBets.itemSnapshotList
    val rows = remember(listState, items) { listState.rowsFor(items.items.filterNotNull()) }
    val isLastPageLoaded = lazyPoolGamblerBets.loadState.append.endOfPaginationReached
    val timeZone = TimeZone.currentSystemDefault()
    val today = now.toLocalDateTime(timeZone).date
    val dayLabelFormat = rememberPendingBetDayLabelFormat()
    val reduceMotion = reduceMotion()
    val placement = rememberCardPlacementSpec(filter = listState.filter, reduceMotion = reduceMotion)

    val lazyListState = rememberLazyListState()
    val stillness = rememberEndOfListStillness(lazyListState)
    val lastVisibleKey = rows.lastOrNull { row -> row.isVisible }?.match?.itemKey()
    RefreshableLazyPagingColumn(
        lazyListState = lazyListState,
        modifier = modifier
            .consumeWindowInsets(contentPadding)
            .fitInside(WindowInsetsRulers.Ime.current)
            .padding(contentPadding.excludingBottom()),
        contentPadding = listPadding + PaddingValues(vertical = LocalBoxSpacing.current.medium),
        lazyPagingItems = lazyPoolGamblerBets,
        loadingContent = { pendingBetPlaceholderList(count = fakeItemCount) },
        emptyContent = { emptyContent() },
        appendLoadingContent = { pendingBetPlaceholderItemRow() },
    ) {
        item(key = FILTER_ROW_KEY, contentType = FILTER_ROW_KEY) {
            PendingBetFilterRow(
                selected = listState.filter,
                counts = listState.counts,
                isPartial = !isLastPageLoaded,
                onSelect = onSelectFilter,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LocalBoxSpacing.current.large)
                    .padding(bottom = LocalBoxSpacing.current.small),
            )
        }

        val emptyMessage = listState.filter.emptyMessage
        if (listState.isFilteredEmpty && isLastPageLoaded && emptyMessage != null) {
            viewportFillingItem { PendingBetFilterEmptyMessage(filter = listState.filter, message = emptyMessage) }
        }

        rows.forEachIndexed { index, row ->
            if (!row.isVisible) return@forEachIndexed
            row.dayHeader?.let { day ->
                stickyHeader(key = "day-$day", contentType = DAY_HEADER_KEY) {
                    PendingBetDayHeader(
                        text = dayLabelFormat.text(PendingBetDay.of(day = day, today = today)),
                        modifier = Modifier.animateItem(fadeInSpec = PendingBetStyle.spec(), placementSpec = placement, fadeOutSpec = PendingBetStyle.spec()),
                    )
                }
            }
            item(key = row.match.itemKey(), contentType = CARD_KEY) {
                // Reading the slot tells the pager the gambler reached it, so it prefetches.
                val match = lazyPoolGamblerBets[index] ?: row.match
                val key = row.match.itemKey()
                val tracking = if (key == lastVisibleKey) with(stillness) { Modifier.trackLastItem(key) } else Modifier
                PendingBetCard(
                    match = match,
                    lockCountdown = lockCountdownMinutes(kickoff = match.matchDateTime, now = now, timeZone = timeZone),
                    rowHolder = rowHolder,
                    onMatchOpen = onMatchOpen,
                    modifier = tracking.animateItem(fadeInSpec = PendingBetStyle.spec(), placementSpec = placement, fadeOutSpec = PendingBetStyle.spec())
                        .padding(horizontal = LocalBoxSpacing.current.large)
                        .padding(bottom = PendingBetStyle.cardSpacing),
                )
            }
        }

        if (rows.isNotEmpty()) {
            item(key = PAGE_TRIGGER_KEY, contentType = PAGE_TRIGGER_KEY) {
                PendingBetPageTrigger(lazyPoolGamblerBets = lazyPoolGamblerBets, lastIndex = rows.lastIndex)
            }
        }
    }
}

/**
 * How cards move to new places. While the filter changes, the cards that stay glide to their new
 * places on the card curve as the others fade, as on iOS. At any other time a card moves because
 * the one above it grows or shrinks frame by frame; it then follows that card without its own
 * animation, because the list restarts an interrupted placement animation on a spring of its own,
 * which made the next card trail the resizing one. The filter change lasts one card animation,
 * kept on an [Animatable] so it follows the animator scale.
 */
@Composable
private fun rememberCardPlacementSpec(filter: PendingBetFilter, reduceMotion: Boolean): FiniteAnimationSpec<IntOffset>? {
    var settledFilter by remember { mutableStateOf(filter) }
    val filterTransition = remember { Animatable(0f) }
    LaunchedEffect(filter) {
        if (filter == settledFilter) return@LaunchedEffect
        filterTransition.snapTo(0f)
        filterTransition.animateTo(1f, PendingBetStyle.spec())
        settledFilter = filter
    }
    return if (filter != settledFilter && !reduceMotion) PendingBetStyle.spec() else null
}

@Composable
private fun PendingBetCard(
    match: PoolGamblerBetModel,
    lockCountdown: Int?,
    rowHolder: ((PoolGamblerBetModel) -> PendingBetRowHolder)?,
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)?,
    modifier: Modifier,
) {
    // Open cards hold the score boxes and steppers, so only locked cards open the match on tap.
    val openModifier = if (onMatchOpen != null && !match.isPending) Modifier.clickable { onMatchOpen(match) } else Modifier
    val cardModifier = modifier.then(openModifier)
    if (rowHolder == null) {
        val savedBet = SavedBet.of(match.betScore)
        val draft = PendingBetRowHolder.Draft.of(savedBet)
        PendingBetItem(
            match = match,
            draft = draft,
            rowState = PendingBetRowHolder.rowStateOf(match = match, draft = draft, saveState = MutationState.Idle(savedBet)),
            lockCountdown = lockCountdown,
            modifier = cardModifier,
        )
    } else {
        PendingBetItemView(rowHolder = rowHolder(match), lockCountdown = lockCountdown, modifier = cardModifier)
    }
}

/**
 * Sits after the last shown item and reads the last loaded slot when it comes on screen, so the
 * pager loads the next page even when the filter hides every card near the end of the list.
 * Keyed by that index, so each new page re-arms it.
 */
@Composable
private fun PendingBetPageTrigger(lazyPoolGamblerBets: LazyPagingItems<PoolGamblerBetModel>, lastIndex: Int) {
    LaunchedEffect(lastIndex) { lazyPoolGamblerBets[lastIndex] }
    Spacer(modifier = Modifier.height(1.dp).clearAndSetSemantics {})
}

/** A kickoff day above that day's cards. It pins while its cards scroll, on the screen's own background. */
@Composable
private fun PendingBetDayHeader(text: String, modifier: Modifier = Modifier) {
    val colors = pendingBetColors()
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = colors.secondaryText,
        modifier = modifier
            .fillMaxWidth()
            .background(colors.headerBackground)
            .padding(horizontal = LocalBoxSpacing.current.large + LocalBoxSpacing.current.small)
            .padding(vertical = LocalBoxSpacing.current.medium)
            .semantics { heading() },
    )
}

/** The message of a filter that keeps no loaded match, in the empty-list style. */
@Composable
private fun PendingBetFilterEmptyMessage(filter: PendingBetFilter, message: Int) {
    Column(
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
    ) {
        Icon(
            painter = painterResource(if (filter == PendingBetFilter.NO_BET) SharedR.drawable.done else SharedR.drawable.pending),
            contentDescription = null,
            tint = pendingBetColors().secondaryText,
            modifier = Modifier.size(iconSize),
        )
        Text(
            text = stringResource(message),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            color = pendingBetColors().secondaryText,
        )
    }
}

private fun PoolGamblerBetModel.itemKey() = Triple(poolId, gamblerId, matchId)

private fun LazyListScope.pendingBetPlaceholderList(count: Int) {
    repeat(count) {
        pendingBetPlaceholderItemRow()
    }
}

private fun LazyListScope.emptyContent() {
    viewportFillingItem {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(id = SharedR.drawable.ic_sentiment_sad),
                contentDescription = null,
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

private fun LazyListScope.pendingBetPlaceholderItemRow() {
    item(contentType = CARD_KEY) {
        PendingBetPlaceholderItem(
            modifier = Modifier
                .padding(horizontal = LocalBoxSpacing.current.large)
                .padding(bottom = PendingBetStyle.cardSpacing),
        )
    }
}

private const val FILTER_ROW_KEY = "filterRow"
private const val DAY_HEADER_KEY = "dayHeader"
private const val CARD_KEY = "card"
private const val PAGE_TRIGGER_KEY = "pageTrigger"
private val iconSize = 64.dp

@OptIn(ExperimentalTime::class)
@PreviewLightDark
@Composable
private fun PendingBetListPreview() {
    val items = MutableStateFlow(PagingData.from(poolGamblerBetPendingDummyModels())).collectAsLazyPagingItems()
    TycheTheme {
        Surface {
            PendingBetList(
                lazyPoolGamblerBets = items,
                listState = PendingBetListState.INITIAL,
                now = Clock.System.now(),
                onSelectFilter = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PendingBetPlaceholderListPreview() {
    TycheTheme {
        Surface {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                pendingBetPlaceholderList(count = 8)
            }
        }
    }
}
