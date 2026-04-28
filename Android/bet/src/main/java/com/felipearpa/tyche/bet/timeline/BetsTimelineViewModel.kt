package com.felipearpa.tyche.bet.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.felipearpa.tyche.bet.pending.PendingBetPagingSource
import com.felipearpa.tyche.data.bet.application.GetGamblerBetsTimeline
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

private const val PAGE_SIZE = 50
private const val PREFETCH_DISTANCE = 5

class BetsTimelineViewModel(
    private val poolId: String,
    val gamblerId: String,
    private val getGamblerBetsTimeline: GetGamblerBetsTimeline,
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
