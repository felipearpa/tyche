package com.felipearpa.tyche.bet.finished

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.bet.pending.PendingBetPagingSource
import com.felipearpa.tyche.data.bet.application.GetFinishedPoolGamblerBetsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

private const val PAGE_SIZE = 50
private const val PREFETCH_DISTANCE = 5

class FinishedBetListViewModel @AssistedInject constructor(
    @Assisted("poolId") private val poolId: String,
    @Assisted("gamblerId") val gamblerId: String,
    private val getFinishedPoolGamblerBetsUseCase: GetFinishedPoolGamblerBetsUseCase,
) :
    ViewModel() {

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
                enablePlaceholders = true,
            ),
            pagingSourceFactory = {
                PendingBetPagingSource(
                    pagingQuery = { next ->
                        getFinishedBetsPagingQuery(
                            next = next,
                            poolId = poolId,
                            gamblerId = gamblerId,
                            search = { searchText.ifEmpty { null } },
                            getFinishedPoolGamblerBetsUseCase = getFinishedPoolGamblerBetsUseCase,
                        )
                    },
                )
            },
        )

    fun search(searchText: String) {
        _searchText.value = searchText.trim()
    }
}
