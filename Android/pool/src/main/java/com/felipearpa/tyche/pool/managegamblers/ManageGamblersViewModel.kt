package com.felipearpa.tyche.pool.managegamblers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.felipearpa.tyche.data.pool.application.GetPool
import com.felipearpa.tyche.data.pool.application.GetPoolMembers
import com.felipearpa.tyche.ui.exception.orDefaultLocalized
import com.felipearpa.ui.state.LoadState
import com.felipearpa.ui.state.map
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 50
private const val PREFETCH_DISTANCE = 5

class ManageGamblersViewModel(
    private val poolId: String,
    private val getPoolMembers: GetPoolMembers,
    private val getPool: GetPool,
) : ViewModel(), RemovalRegistry {

    val pageSize = PAGE_SIZE

    val poolMembers = buildPager().flow
        .cachedIn(scope = viewModelScope)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = PagingData.empty(),
        )

    private val _removedGamblerIds = MutableStateFlow<Set<String>>(emptySet())
    val removedGamblerIds: StateFlow<Set<String>> = _removedGamblerIds.asStateFlow()

    private val _failedGamblerIds = MutableStateFlow<Set<String>>(emptySet())
    override val failedGamblerIds: StateFlow<Set<String>> = _failedGamblerIds.asStateFlow()

    private val fetchedCount = MutableStateFlow(FetchedCount(state = LoadState.Loading, reflectedRemovals = emptySet()))
    private var countJob: Job? = null

    /** Removals the server had already applied when the pending refresh started. */
    private var removalsBeforeRefresh: Set<String>? = null

    /**
     * The pool's gambler count less the removals this screen made since the count was requested.
     * It is [LoadState.Loading] only until the first answer; a refresh keeps the previous count
     * until the new one arrives.
     */
    val gamblerCount: StateFlow<LoadState<Int>> =
        combine(fetchedCount, _removedGamblerIds, ::shownCount)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = shownCount(fetchedCount.value, _removedGamblerIds.value),
            )

    init {
        loadGamblerCount()
    }

    private fun buildPager() =
        Pager(
            config = PagingConfig(
                pageSize = pageSize,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                PoolMemberPagingSource(
                    pagingQuery = { next ->
                        getPoolMembersPagingQuery(
                            next = next,
                            poolId = poolId,
                            getPoolMembers = getPoolMembers,
                        )
                    },
                )
            },
        )

    /**
     * Called with a pull-to-refresh, alongside the members' own refresh: returns every failed row to
     * idle and requests the count again. Removed gamblers stay hidden until [onMembersRefreshed]
     * reports that the refreshed members, which no longer contain them, are shown.
     */
    fun refresh() {
        _failedGamblerIds.value.forEach(::clearFailed)
        removalsBeforeRefresh = _removedGamblerIds.value
        loadGamblerCount()
    }

    /** Called once the members' first page has been reloaded and shown after [refresh]. */
    fun onMembersRefreshed() {
        val removals = removalsBeforeRefresh ?: return
        removalsBeforeRefresh = null
        _removedGamblerIds.update { ids -> ids - removals }
    }

    override fun markRemoved(gamblerId: String) {
        _failedGamblerIds.update { ids -> ids - gamblerId }
        _removedGamblerIds.update { ids -> ids + gamblerId }
    }

    override fun markFailed(gamblerId: String) {
        _failedGamblerIds.update { ids -> ids + gamblerId }
    }

    override fun clearFailed(gamblerId: String) {
        _failedGamblerIds.update { ids -> ids - gamblerId }
    }

    private fun loadGamblerCount() {
        // A newer request supersedes an unanswered one, so a slow answer cannot overwrite it.
        countJob?.cancel()
        val reflectedRemovals = _removedGamblerIds.value
        countJob = viewModelScope.launch {
            val state: LoadState<Int> = getPool.execute(poolId = poolId).fold(
                onSuccess = { pool ->
                    pool.gamblerCount?.let { count -> LoadState.Loaded(count) }
                        ?: LoadState.Failure(MissingGamblerCountException(poolId))
                },
                onFailure = { exception -> LoadState.Failure(exception.orDefaultLocalized()) },
            )
            fetchedCount.value = FetchedCount(state = state, reflectedRemovals = reflectedRemovals)
        }
    }

    private fun shownCount(fetched: FetchedCount, removedIds: Set<String>): LoadState<Int> =
        fetched.state.map { count -> count - (removedIds - fetched.reflectedRemovals).size }

    /** A count answer and the removals the server had already applied when it was requested. */
    private data class FetchedCount(
        val state: LoadState<Int>,
        val reflectedRemovals: Set<String>,
    )
}

/** The pool answered without a gambler count, so the screen shows no count rather than a guess. */
class MissingGamblerCountException(poolId: String) :
    IllegalStateException("Pool $poolId has no gambler count")
