package com.felipearpa.tyche.bet.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.felipearpa.tyche.bet.finished.HistoryPointsSummaryLoader
import com.felipearpa.tyche.bet.finished.HistoryPointsSummaryState
import com.felipearpa.tyche.bet.pending.PendingBetPagingSource
import com.felipearpa.tyche.data.bet.application.GetGamblerBetsTimeline
import com.felipearpa.tyche.data.pool.application.GetPoolGamblerScore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Another gambler's Timeline in one pool: the existing feed of their bets, in server order, and
 * their authoritative earned-points total. The two load independently, so either can succeed
 * while the other is loading or failed.
 */
class BetTimelineListViewModel(
    private val poolId: String,
    val gamblerId: String,
    private val getGamblerBetsTimeline: GetGamblerBetsTimeline,
    getPoolGamblerScore: GetPoolGamblerScore,
) : ViewModel() {

    val pageSize = PAGE_SIZE

    val poolGamblerBets = buildPager().flow
        .cachedIn(scope = viewModelScope)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = PagingData.empty(),
        )

    private val pointsSummaryLoader = HistoryPointsSummaryLoader(
        scope = viewModelScope,
        poolId = poolId,
        gamblerId = gamblerId,
        getPoolGamblerScore = getPoolGamblerScore,
    )

    /**
     * The selected gambler's total in this pool, from the server; it is never summed from loaded
     * rows, and appending a page does not request it.
     */
    val pointsSummary: StateFlow<HistoryPointsSummaryState> = pointsSummaryLoader.state

    init {
        loadPointsSummary()
    }

    /** Requests only the total, as a summary retry does; the rows are not reloaded. */
    fun loadPointsSummary() {
        pointsSummaryLoader.load()
    }

    /** Requests the total for a pull to refresh, which also refreshes the rows. */
    fun refreshPointsSummary() {
        pointsSummaryLoader.refresh()
    }

    private fun buildPager() =
        Pager(
            config = PagingConfig(
                pageSize = pageSize,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                PendingBetPagingSource(
                    pagingQuery = { next ->
                        getBetsTimelinePagingQuery(
                            next = next,
                            poolId = poolId,
                            gamblerId = gamblerId,
                            getGamblerBetsTimeline = getGamblerBetsTimeline,
                        )
                    },
                )
            },
        )
}

private const val PAGE_SIZE = 50
private const val PREFETCH_DISTANCE = 5
