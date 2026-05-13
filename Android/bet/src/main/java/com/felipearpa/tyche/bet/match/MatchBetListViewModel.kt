package com.felipearpa.tyche.bet.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.pending.PendingBetPagingSource
import com.felipearpa.tyche.bet.toPoolGamblerBetModel
import com.felipearpa.tyche.data.bet.application.GetPoolGamblerBet
import com.felipearpa.tyche.data.bet.application.GetPoolMatchGamblerBets
import com.felipearpa.ui.state.LoadableViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MatchBetListViewModel(
    private val poolId: String,
    private val gamblerId: String,
    private val matchId: String,
    private val getPoolGamblerBet: GetPoolGamblerBet,
    private val getPoolMatchGamblerBets: GetPoolMatchGamblerBets,
) : ViewModel() {

    val pageSize = PAGE_SIZE

    private val _poolGamblerBetState =
        MutableStateFlow<LoadableViewState<PoolGamblerBetModel>>(LoadableViewState.Loading)
    val poolGamblerBetState: StateFlow<LoadableViewState<PoolGamblerBetModel>> = _poolGamblerBetState.asStateFlow()

    val poolGamblerBets = buildPager().flow
        .cachedIn(scope = viewModelScope)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = PagingData.empty(),
        )

    fun loadPoolGamblerBet() {
        _poolGamblerBetState.value = LoadableViewState.Loading
        viewModelScope.launch {
            getPoolGamblerBet.execute(
                poolId = poolId,
                gamblerId = gamblerId,
                matchId = matchId,
            ).onSuccess { bet ->
                _poolGamblerBetState.value = LoadableViewState.Success(bet.toPoolGamblerBetModel())
            }.onFailure { exception ->
                _poolGamblerBetState.value = LoadableViewState.Failure(exception)
            }
        }
    }

    private fun buildPager() =
        Pager(
            config = PagingConfig(
                pageSize = pageSize,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = false,
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

private const val PAGE_SIZE = 50
private const val PREFETCH_DISTANCE = 5
