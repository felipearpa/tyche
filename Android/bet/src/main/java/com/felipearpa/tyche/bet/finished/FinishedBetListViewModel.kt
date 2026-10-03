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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    private val _pointsSummary = MutableStateFlow(HistoryPointsSummaryState.initial)

    /**
     * The authoritative total for this view model's pool and gambler. It is requested on entry,
     * on pull to refresh, and on summary retry; paging never requests or derives it. A view model
     * serves one pool and gambler, so another context gets a new view model and never sees this
     * total.
     */
    val pointsSummary: StateFlow<HistoryPointsSummaryState> = _pointsSummary.asStateFlow()

    private var pointsSummaryJob: Job? = null

    /** Identifies the latest summary request; a response from an older request is discarded. */
    private var pointsSummaryGeneration = 0

    init {
        loadPointsSummary()
    }

    fun search(searchText: String) {
        _searchText.value = searchText.trim()
    }

    /**
     * Requests the earned-points total on its own, as a summary retry does, so it never reloads
     * the rows. Confirmed points stay visible while it runs.
     */
    fun loadPointsSummary() {
        requestPointsSummary(HistoryPointsSummaryState.Request.LOADING)
    }

    /**
     * Requests the total for a pull to refresh, which also refreshes the rows. The pull indicator
     * stays while this request is pending.
     */
    fun refreshPointsSummary() {
        requestPointsSummary(HistoryPointsSummaryState.Request.PULL_REFRESHING)
    }

    private fun requestPointsSummary(request: HistoryPointsSummaryState.Request) {
        pointsSummaryJob?.cancel()
        val generation = ++pointsSummaryGeneration
        _pointsSummary.update { state -> state.requesting(request) }

        pointsSummaryJob = viewModelScope.launch {
            val result = getPoolGamblerScore.execute(poolId = poolId, gamblerId = gamblerId)
            // A superseded request can still answer when its call ignores cancellation.
            if (generation != pointsSummaryGeneration) return@launch
            _pointsSummary.update { state ->
                result.fold(
                    onSuccess = { score -> state.loaded(HistoryPoints.of(score.score)) },
                    onFailure = { state.failed() },
                )
            }
        }
    }
}

private const val PAGE_SIZE = 50
private const val PREFETCH_DISTANCE = 5
