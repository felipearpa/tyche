package com.felipearpa.tyche.pool.poolscore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.core.JoinPoolUrlTemplateProvider
import com.felipearpa.tyche.data.pool.application.GetPoolGamblerScoresByGamblerUseCase
import com.felipearpa.tyche.pool.PoolGamblerScorePagingSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

private const val PAGE_SIZE = 50
private const val PREFETCH_DISTANCE = 5

class PoolScoreListViewModel @AssistedInject constructor(
    @Assisted val gamblerId: String,
    private val getPoolGamblerScoresByGamblerUseCase: GetPoolGamblerScoresByGamblerUseCase,
    private val joinPoolUrlTemplate: JoinPoolUrlTemplateProvider,
) :
    ViewModel() {

    val pageSize = PAGE_SIZE
    private var _searchText = MutableStateFlow(emptyString())

    @OptIn(ExperimentalCoroutinesApi::class)
    val poolGamblerScores = _searchText.flatMapLatest { newSearchText ->
        buildPager(searchText = newSearchText).flow.cachedIn(viewModelScope)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = PagingData.empty(),
    )

    private val _refreshEvent: MutableSharedFlow<Unit> =
        MutableSharedFlow(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    val refreshEvents: SharedFlow<Unit> = _refreshEvent.asSharedFlow()

    private var pagingSource: PoolGamblerScorePagingSource? = null

    private fun buildPager(searchText: String) =
        Pager(
            config = PagingConfig(
                pageSize = pageSize,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = true,
            ),
            pagingSourceFactory = {
                PoolGamblerScorePagingSource(
                    pagingQuery = { next ->
                        getPoolGamblerScoresByGamblerPagingQuery(
                            next = next,
                            gamblerId = gamblerId,
                            search = { searchText.ifEmpty { null } },
                            getPoolGamblerScoresByGamblerUseCase = getPoolGamblerScoresByGamblerUseCase,
                        )
                    },
                ).also { pagingSource = it }
            },
        )

    fun refresh() {
        pagingSource?.invalidate()
    }

    fun search(searchText: String) {
        _searchText.value = searchText.trim()
    }

    fun createUrlForJoining(poolId: String): String {
        return String.format(joinPoolUrlTemplate(), poolId)
    }
}
