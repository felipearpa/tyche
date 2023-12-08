package com.felipearpa.tyche.pool.gamblerscore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.felipearpa.data.pool.application.GetPoolGamblerScoresByPoolUseCase
import com.felipearpa.tyche.core.emptyString
import com.felipearpa.tyche.pool.PoolGamblerScorePagingSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

private const val PAGE_SIZE = 50
private const val PREFETCH_DISTANCE = 5

class GamblerScoreListViewModel @AssistedInject constructor(
    @Assisted("poolId") private val poolId: String,
    @Assisted("gamblerId") val gamblerId: String,
    private val getPoolGamblerScoresByPoolUseCase: GetPoolGamblerScoresByPoolUseCase
) : ViewModel() {

    val pageSize = PAGE_SIZE
    private var _searchText = MutableStateFlow(emptyString())

    @OptIn(ExperimentalCoroutinesApi::class)
    val poolGamblerScores = _searchText.flatMapLatest { newSearchText ->
        buildPager(searchText = newSearchText).flow.cachedIn(scope = viewModelScope)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = PagingData.empty()
    )

    private fun buildPager(searchText: String) =
        Pager(
            config = PagingConfig(
                pageSize = pageSize,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = true
            ),
            pagingSourceFactory = {
                PoolGamblerScorePagingSource(
                    pagingQuery = { next ->
                        getPoolGamblerScoresByPoolPagingQuery(
                            next = next,
                            poolId = poolId,
                            search = { searchText.ifEmpty { null } },
                            getPoolGamblerScoresByPoolUseCase = getPoolGamblerScoresByPoolUseCase
                        )
                    }
                )
            }
        )

    fun search(searchText: String) {
        _searchText.value = searchText.trim()
    }
}