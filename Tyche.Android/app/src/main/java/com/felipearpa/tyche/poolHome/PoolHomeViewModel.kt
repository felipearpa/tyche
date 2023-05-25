package com.felipearpa.tyche.poolHome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.core.network.NetworkException
import com.felipearpa.pool.application.GetPoolUseCase
import com.felipearpa.pool.ui.PoolModel
import com.felipearpa.pool.ui.toModel
import com.felipearpa.ui.UnknownException
import com.felipearpa.ui.ViewState
import com.felipearpa.ui.network.toNetworkAppException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PoolHomeViewModel @AssistedInject constructor(
    @Assisted("poolId") val poolId: String,
    @Assisted("gamblerId") val gamblerId: String,
    private val getPoolUseCase: GetPoolUseCase
) :
    ViewModel() {

    private val _state = MutableStateFlow<ViewState<PoolModel>>(ViewState.Initial)
    val state: StateFlow<ViewState<PoolModel>>
        get() = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.emit(ViewState.Loading)

            val maybePool = getPoolUseCase.execute(poolId = poolId)
            maybePool.onSuccess { pool ->
                _state.emit(ViewState.Success(pool.toModel()))
            }.onFailure { exception ->
                _state.emit(ViewState.Failure(exception.toAppException()))
            }
        }
    }
}

private fun Throwable.toAppException() =
    when (this) {
        is NetworkException -> this.toNetworkAppException()
        else -> UnknownException()
    }