package com.felipearpa.tyche.bet.pending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.data.bet.application.GetPendingPoolGamblerBets
import com.felipearpa.tyche.data.bet.application.PlaceBet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

private const val PAGE_SIZE = 50
private const val PREFETCH_DISTANCE = 5

/**
 * The Bets tab: the paged pending feed, one [PendingBetRowHolder] per loaded match, and the filter
 * over them.
 *
 * Holders are created as pages arrive, before the list renders them, and kept for the screen's
 * lifetime in [rowHolders], so drafts and in-flight saves survive paging updates and filtering.
 * Their saves run on this view model's `viewModelScope`. Counts and filter membership in
 * [listState] cover the matches loaded by the current paging generation and are recomputed when a
 * page lands, the filter changes, or a holder reports an outcome.
 */
class PendingBetListViewModel(
    private val poolId: String,
    val gamblerId: String,
    private val getPendingPoolGamblerBets: GetPendingPoolGamblerBets,
    private val placeBet: PlaceBet,
) : ViewModel() {

    val pageSize = PAGE_SIZE
    private val searchText = MutableStateFlow(emptyString())

    /**
     * One holder per match ID, replaced as a whole when a holder is added. Nothing observes the
     * map itself: the screen reads holders by match and observes [listState] and each holder's
     * own flows, so a published map would only announce identities no one consumes.
     */
    private var rowHolders: Map<String, PendingBetRowHolder> = emptyMap()

    /** The paging generation whose pages [loadedMatchIds] holds; a new source starts a new one. */
    private var latestGeneration = 0

    /** The current generation's loaded matches in feed order. */
    private var loadedMatchIds: List<String> = emptyList()

    private val _listState = MutableStateFlow(PendingBetListState.INITIAL)
    val listState: StateFlow<PendingBetListState> = _listState.asStateFlow()

    private val rowOutcomes = object : PendingBetRowOutcomes {
        override fun saved(matchId: String) = refreshListState()
        override fun failed(matchId: String) = refreshListState()
        override fun failureCleared(matchId: String) = refreshListState()
        override fun locked(matchId: String) = refreshListState()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val poolGamblerBets: StateFlow<PagingData<PoolGamblerBetModel>> = searchText.flatMapLatest { newSearchText ->
        buildPager(searchText = newSearchText).flow.cachedIn(scope = viewModelScope)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = PagingData.empty(),
    )

    /** The holder of a loaded match, created on first use. */
    fun rowHolder(match: PoolGamblerBetModel): PendingBetRowHolder {
        rowHolders[match.matchId]?.let { holder -> return holder }
        val holder = PendingBetRowHolder(
            match = match,
            placeBet = placeBet,
            scope = viewModelScope,
            outcomes = rowOutcomes,
        )
        rowHolders = rowHolders + (match.matchId to holder)
        return holder
    }

    fun selectFilter(filter: PendingBetFilter) {
        if (filter == _listState.value.filter) return
        refreshListState(filter = filter)
    }

    fun search(searchText: String) {
        this.searchText.value = searchText.trim()
    }

    private fun buildPager(searchText: String) =
        Pager(
            config = PagingConfig(
                pageSize = pageSize,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                val generation = ++latestGeneration
                PendingBetPagingSource(
                    pagingQuery = { next ->
                        getPendingBetsPagingQuery(
                            next = next,
                            poolId = poolId,
                            gamblerId = gamblerId,
                            search = { searchText.ifEmpty { null } },
                            getPendingPoolGamblerBets = getPendingPoolGamblerBets,
                        ).also { result ->
                            val page = result.getOrNull() ?: return@also
                            withContext(Dispatchers.Main.immediate) {
                                pageLoaded(generation = generation, isFirstPage = next == null, matches = page.items)
                            }
                        }
                    },
                )
            },
        )

    /**
     * Binds a page's matches to their holders before the pager hands the page to the list. A page
     * from a superseded generation is ignored; a first page replaces the loaded matches, so a
     * refresh or a new search counts only what it returns.
     */
    private fun pageLoaded(generation: Int, isFirstPage: Boolean, matches: List<PoolGamblerBetModel>) {
        if (generation != latestGeneration) return
        matches.forEach { match -> rowHolder(match).bind(match) }
        val pageIds = matches.map { match -> match.matchId }
        loadedMatchIds = if (isFirstPage) pageIds else loadedMatchIds + (pageIds - loadedMatchIds.toSet())
        refreshListState()
    }

    private fun refreshListState(filter: PendingBetFilter = _listState.value.filter) {
        val rows = loadedMatchIds.mapNotNull { matchId -> rowHolders[matchId] }
        _listState.value = PendingBetListState(
            filter = filter,
            counts = PendingBetFilterCounts(
                all = rows.size,
                noBet = rows.count(PendingBetFilter.NO_BET::keeps),
                placed = rows.count(PendingBetFilter.PLACED::keeps),
            ),
            visibleMatchIds = rows.filter(filter::keeps).map { row -> row.matchId }.toSet(),
        )
    }
}

/** Which loaded matches the Bets tab shows. */
enum class PendingBetFilter {
    /** Every loaded match. */
    ALL,

    /** Open matches without a saved prediction, incomplete drafts included. */
    NO_BET,

    /** Matches with a saved prediction, locked ones included. */
    PLACED;

    /** A locked match without a prediction can no longer get one, so only [ALL] keeps it. */
    fun keeps(row: PendingBetRowHolder): Boolean = when (this) {
        ALL -> true
        NO_BET -> row.isEditable && row.saved == SavedBet.None
        PLACED -> row.saved is SavedBet.Placed
    }
}

/** How many loaded matches each filter shows. */
data class PendingBetFilterCounts(val all: Int, val noBet: Int, val placed: Int)

/**
 * The filter, its counts, and which loaded matches it keeps.
 *
 * The list keeps one slot per paging index whatever the filter, so the pager still sees every
 * index read and keeps loading pages; [rowsFor] says which slots show their card and which open a
 * day.
 */
data class PendingBetListState(
    val filter: PendingBetFilter,
    val counts: PendingBetFilterCounts,
    val visibleMatchIds: Set<String>,
) {
    /** Matches are loaded, but the filter keeps none of them. */
    val isFilteredEmpty: Boolean get() = counts.all > 0 && visibleMatchIds.isEmpty()

    fun isVisible(match: PoolGamblerBetModel): Boolean =
        filter == PendingBetFilter.ALL || match.matchId in visibleMatchIds

    /**
     * One row per paging item, in order: whether the filter shows it and, for the first shown
     * match of each kickoff day, that day for its header.
     */
    fun rowsFor(items: List<PoolGamblerBetModel>): List<PendingBetListRow> {
        var lastDay: LocalDate? = null
        return items.map { match ->
            val isVisible = isVisible(match)
            val day = match.matchDateTime.date
            val opensDay = isVisible && day != lastDay
            if (opensDay) lastDay = day
            PendingBetListRow(match = match, isVisible = isVisible, dayHeader = if (opensDay) day else null)
        }
    }

    companion object {
        val INITIAL = PendingBetListState(
            filter = PendingBetFilter.ALL,
            counts = PendingBetFilterCounts(all = 0, noBet = 0, placed = 0),
            visibleMatchIds = emptySet(),
        )
    }
}

/** A paging item as the filtered list presents it. */
data class PendingBetListRow(
    val match: PoolGamblerBetModel,
    val isVisible: Boolean,
    /** The kickoff day this row's header names, or null when the row opens no day. */
    val dayHeader: LocalDate?,
)
