package com.felipearpa.tyche.bet.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.felipearpa.tyche.bet.pending.PendingBetPagingSource
import com.felipearpa.tyche.data.bet.application.GetPoolMatchGamblerBets
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

private const val PAGE_SIZE = 50
private const val PREFETCH_DISTANCE = 5

class MatchBetListViewModel(
    private val poolId: String,
    val matchId: String,
    private val getPoolMatchGamblerBets: GetPoolMatchGamblerBets,
) : ViewModel() {

    val pageSize = PAGE_SIZE

    val poolGamblerBets = buildPager().flow
        .cachedIn(scope = viewModelScope)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = PagingData.empty(),
        )

    private fun buildPager() =
        Pager(
            config = PagingConfig(
                pageSize = pageSize,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = true,
            ),
            pagingSourceFactory = {
                PendingBetPagingSource(
                    pagingQuery = { next ->
                        getPoolMatchBetsPagingQuery(
                            next = next,
                            poolId = poolId,
                            matchId = matchId,
                            getPoolMatchGamblerBets = getPoolMatchGamblerBets,
                        )
                    },
                )
            },
        )
}
