package com.felipearpa.tyche.bet.finished

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.bet.pending.PendingBetPagingSource
import com.felipearpa.tyche.data.bet.application.GetFinishedPoolGamblerBets
import com.felipearpa.tyche.data.pool.application.GetPoolGamblerScore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class FinishedBetListViewModel(
    private val poolId: String,
    val gamblerId: String,
    private val getFinishedPoolGamblerBets: GetFinishedPoolGamblerBets,
    private val getPoolGamblerScore: GetPoolGamblerScore,
) : ViewModel() {

    val pageSize = PAGE_SIZE
    private var _searchText = MutableStateFlow(emptyString())

    @OptIn(ExperimentalCoroutinesApi::class)
    val poolGamblerBets = _searchText.flatMapLatest { newSearchText ->
        buildPager(searchText = newSearchText).flow.cachedIn(scope = viewModelScope)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = PagingData.empty(),
    )

    private fun buildPager(searchText: String) =
        Pager(
            config = PagingConfig(
                pageSize = pageSize,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                PendingBetPagingSource(
                    pagingQuery = { next ->
                        getFinishedBetsPagingQuery(
                            next = next,
                            poolId = poolId,
                            gamblerId = gamblerId,
                            search = { searchText.ifEmpty { null } },
                            getFinishedPoolGamblerBets = getFinishedPoolGamblerBets,
                        )
                    },
                )
            },
        )

    private val pointsSummaryLoader = HistoryPointsSummaryLoader(
        scope = viewModelScope,
        poolId = poolId,
        gamblerId = gamblerId,
        getPoolGamblerScore = getPoolGamblerScore,
    )

    /** The authoritative total for this view model's pool and gambler; see [HistoryPointsSummaryLoader]. */
    val pointsSummary: StateFlow<HistoryPointsSummaryState> = pointsSummaryLoader.state

    init {
        loadPointsSummary()
    }

    fun search(searchText: String) {
        _searchText.value = searchText.trim()
    }

    /** Requests only the total, as a summary retry does; the rows are not reloaded. */
    fun loadPointsSummary() {
        pointsSummaryLoader.load()
    }

    /** Requests the total for a pull to refresh, which also refreshes the rows. */
    fun refreshPointsSummary() {
        pointsSummaryLoader.refresh()
    }
}

private const val PAGE_SIZE = 50
private const val PREFETCH_DISTANCE = 5
