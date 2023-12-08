package com.felipearpa.tyche.poolhome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.data.pool.application.GetPoolUseCase
import com.felipearpa.tyche.core.network.NetworkException
import com.felipearpa.tyche.pool.PoolModel
import com.felipearpa.tyche.pool.toPoolModel
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.network.toNetworkLocalizedException
import com.felipearpa.tyche.ui.state.LoadableViewState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PoolHomeViewModel @AssistedInject constructor(
    @Assisted("poolId") val poolId: String,
    @Assisted("gamblerId") val gamblerId: String,
    private val getPoolUseCase: GetPoolUseCase
) :
    ViewModel() {

    private val _state = MutableStateFlow<LoadableViewState<PoolModel>>(LoadableViewState.Initial)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.emit(LoadableViewState.Loading)

            val maybePool = getPoolUseCase.execute(poolId = poolId)
            maybePool.onSuccess { pool ->
                _state.emit(LoadableViewState.Success(pool.toPoolModel()))
            }.onFailure { exception ->
                _state.emit(LoadableViewState.Failure(exception.toLocalizedException()))
            }
        }
    }
}

private fun Throwable.toLocalizedException() =
    when (this) {
        is NetworkException -> this.toNetworkLocalizedException()
        else -> UnknownLocalizedException()
    }