package com.felipearpa.tyche.poolhome.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.data.pool.application.GetPoolGamblerScoreUseCase
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.toPoolGamblerScoreModel
import com.felipearpa.tyche.session.authentication.application.LogOutUseCase
import com.felipearpa.tyche.ui.exception.orDefaultLocalized
import com.felipearpa.ui.state.LoadableViewState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DrawerViewModel @AssistedInject constructor(
    @Assisted("poolId") val poolId: String,
    @Assisted("gamblerId") val gamblerId: String,
    private val logoutUseCase: LogOutUseCase,
    private val getPoolGamblerScoreUseCase: GetPoolGamblerScoreUseCase
) : ViewModel() {
    private val _state =
        MutableStateFlow<LoadableViewState<PoolGamblerScoreModel>>(LoadableViewState.Initial)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.emit(LoadableViewState.Loading)

            val poolResult =
                getPoolGamblerScoreUseCase.execute(poolId = poolId, gamblerId = gamblerId)
            poolResult.onSuccess { poolGamblerScore ->
                _state.emit(LoadableViewState.Success(poolGamblerScore.toPoolGamblerScoreModel()))
            }.onFailure { exception ->
                _state.emit(LoadableViewState.Failure(exception.orDefaultLocalized()))
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase.execute()
        }
    }
}