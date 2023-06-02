package com.felipearpa.tyche.bet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.felipearpa.tyche.bet.application.BetUseCase
import com.felipearpa.tyche.bet.application.GetPoolGamblerBetsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow

private const val PAGE_SIZE = 50
private const val PREFETCH_DISTANCE = 5

class PoolGamblerBetListViewModel @AssistedInject constructor(
    @Assisted("poolId") private val poolId: String,
    @Assisted("gamblerId") public val gamblerId: String,
    private val getPoolGamblerBetsUseCase: GetPoolGamblerBetsUseCase,
    private val betUseCase: BetUseCase
) :
    ViewModel() {

    val pageSize = PAGE_SIZE

    private lateinit var poolGamblerBetPagingSource: PoolGamblerBetPagingSource

    private var _searchText: String? = null

    private var _poolGamblerBets = Pager(
        config = PagingConfig(
            pageSize = pageSize,
            prefetchDistance = PREFETCH_DISTANCE,
            enablePlaceholders = true
        ),
        pagingSourceFactory = {
            PoolGamblerBetPagingSource(
                pagingQuery = { next ->
                    getPoolGamblerBetsPagingQuery(
                        next = next,
                        poolId = poolId,
                        gamblerId = gamblerId,
                        search = { _searchText },
                        getPoolGamblerBetsUseCase = getPoolGamblerBetsUseCase
                    )
                }
            ).also { poolPagingSource ->
                this.poolGamblerBetPagingSource = poolPagingSource
            }
        }
    ).flow.cachedIn(scope = viewModelScope)
    val poolGamblerBets: Flow<PagingData<PoolGamblerBetModel>>
        get() = _poolGamblerBets

    fun search(searchText: String) {
        _searchText = searchText.trim().ifEmpty { null }
        poolGamblerBetPagingSource.invalidate()
    }
}