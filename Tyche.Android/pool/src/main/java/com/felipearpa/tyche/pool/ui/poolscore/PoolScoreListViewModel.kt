package com.felipearpa.tyche.pool.ui.poolscore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.felipearpa.tyche.pool.application.GetPoolGamblerScoresByGamblerUseCase
import com.felipearpa.tyche.pool.ui.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.ui.PoolGamblerScorePagingSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow

private const val PAGE_SIZE = 50
private const val PREFETCH_DISTANCE = 5

class PoolScoreListViewModel @AssistedInject constructor(
    @Assisted private val gamblerId: String,
    private val getPoolGamblerScoresByGamblerUseCase: GetPoolGamblerScoresByGamblerUseCase
) :
    ViewModel() {

    val pageSize = PAGE_SIZE

    private lateinit var poolGamblerScorePagingSource: PoolGamblerScorePagingSource

    private var _searchText: String? = null

    private var _poolGamblerScores = Pager(
        config = PagingConfig(
            pageSize = pageSize,
            prefetchDistance = PREFETCH_DISTANCE,
            enablePlaceholders = true
        ),
        pagingSourceFactory = {
            PoolGamblerScorePagingSource(
                pagingQuery = { next ->
                    getPoolGamblerScoresByGamblerPagingQuery(
                        next = next,
                        gamblerId = gamblerId,
                        search = { _searchText },
                        getPoolGamblerScoresByGamblerUseCase = getPoolGamblerScoresByGamblerUseCase
                    )
                }
            ).also { poolPagingSource ->
                this.poolGamblerScorePagingSource = poolPagingSource
            }
        }
    ).flow.cachedIn(scope = viewModelScope)
    val poolGamblerScores: Flow<PagingData<PoolGamblerScoreModel>>
        get() = _poolGamblerScores

    fun search(searchText: String) {
        _searchText = searchText.trim().ifEmpty { null }
        poolGamblerScorePagingSource.invalidate()
    }
}