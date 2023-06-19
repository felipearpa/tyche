package com.felipearpa.tyche.poolHome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.core.network.NetworkException
import com.felipearpa.tyche.pool.application.GetPoolUseCase
import com.felipearpa.tyche.pool.ui.PoolModel
import com.felipearpa.tyche.pool.ui.toModel
import com.felipearpa.tyche.ui.UnknownLocalizedException
import com.felipearpa.tyche.ui.ViewState
import com.felipearpa.tyche.ui.network.toNetworkLocalizedException
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
                _state.emit(ViewState.Failure(exception.toLocalizedException()))
            }
        }
    }
}

private fun Throwable.toLocalizedException() =
    when (this) {
        is NetworkException -> this.toNetworkLocalizedException()
        else -> UnknownLocalizedException()
    }