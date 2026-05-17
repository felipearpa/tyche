package com.felipearpa.tyche.pool.joiner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.data.pool.application.GetPool
import com.felipearpa.tyche.data.pool.application.JoinPool
import com.felipearpa.tyche.data.pool.domain.JoinPoolInput
import com.felipearpa.ui.state.LoadState
import com.felipearpa.ui.state.SaveState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PoolJoinerViewModel(
    private val getPool: GetPool,
    private val joinPoolAction: JoinPool,
) : ViewModel() {
    private val _poolState =
        MutableStateFlow<LoadState<PoolModel>>(LoadState.Idle)
    val poolState = _poolState.asStateFlow()

    private val _joinPoolState =
        MutableStateFlow<SaveState<Unit>>(SaveState.Idle)
    val joinPoolState = _joinPoolState.asStateFlow()

    fun loadPool(poolId: String) {
        viewModelScope.launch {
            _poolState.emit(LoadState.Loading)
            val result = getPool.execute(poolId = poolId)
            result.onSuccess { pool ->
                _poolState.emit(LoadState.Loaded(pool.toPoolModel()))
            }.onFailure {
                _poolState.emit(LoadState.Failure(it))
            }
        }
    }

    fun joinPool(poolId: String, gamblerId: String) {
        viewModelScope.launch {
            _joinPoolState.emit(SaveState.Saving(Unit))
            val result =
                joinPoolAction.execute(JoinPoolInput(poolId = poolId, gamblerId = gamblerId))
            result.onSuccess {
                _joinPoolState.emit(SaveState.Saved(Unit))
            }.onFailure {
                _joinPoolState.emit(SaveState.Failure(Unit, it.asJoinPoolLocalizedException()))
            }
        }
    }
}
