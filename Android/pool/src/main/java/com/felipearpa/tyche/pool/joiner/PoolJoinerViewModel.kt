package com.felipearpa.tyche.pool.joiner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.data.pool.application.GetPool
import com.felipearpa.tyche.data.pool.application.JoinPool
import com.felipearpa.tyche.data.pool.domain.JoinPoolInput
import com.felipearpa.ui.state.LoadableViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PoolJoinerViewModel(
    private val getPool: GetPool,
    private val joinPoolAction: JoinPool,
) : ViewModel() {
    private val _poolState =
        MutableStateFlow<LoadableViewState<PoolModel>>(LoadableViewState.Initial)
    val poolState = _poolState.asStateFlow()

    private val _joinPoolState =
        MutableStateFlow<LoadableViewState<Unit>>(LoadableViewState.Initial)
    val joinPoolState = _joinPoolState.asStateFlow()

    fun loadPool(poolId: String) {
        viewModelScope.launch {
            _poolState.emit(LoadableViewState.Loading)
            val result = getPool.execute(poolId = poolId)
            result.onSuccess { pool ->
                _poolState.emit(LoadableViewState.Success(pool.toPoolModel()))
            }.onFailure {
                _poolState.emit(LoadableViewState.Failure(it))
            }
        }
    }

    fun joinPool(poolId: String, gamblerId: String) {
        viewModelScope.launch {
            _joinPoolState.emit(LoadableViewState.Loading)
            val result =
                joinPoolAction.execute(JoinPoolInput(poolId = poolId, gamblerId = gamblerId))
            result.onSuccess {
                _joinPoolState.emit(LoadableViewState.Success(Unit))
            }.onFailure {
                _joinPoolState.emit(LoadableViewState.Failure(it))
            }
        }
    }
}
