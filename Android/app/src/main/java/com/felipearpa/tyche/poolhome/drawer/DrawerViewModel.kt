package com.felipearpa.tyche.poolhome.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.data.pool.application.DeletePool
import com.felipearpa.tyche.data.pool.application.GetPool
import com.felipearpa.tyche.data.pool.application.GetPoolGamblerScore
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.toPoolGamblerScoreModel
import com.felipearpa.tyche.session.AccountStorage
import com.felipearpa.tyche.session.authentication.application.LogOut
import com.felipearpa.tyche.ui.exception.orDefaultLocalized
import com.felipearpa.ui.state.LoadableViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DrawerViewModel(
    val poolId: String,
    val gamblerId: String,
    private val logOut: LogOut,
    private val getPoolGamblerScore: GetPoolGamblerScore,
    private val getPool: GetPool,
    private val deletePool: DeletePool,
    private val accountStorage: AccountStorage,
) : ViewModel() {
    private val _state =
        MutableStateFlow<LoadableViewState<PoolGamblerScoreModel>>(LoadableViewState.Initial)
    val state = _state.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _isOwner = MutableStateFlow(false)
    val isOwner: StateFlow<Boolean> = _isOwner.asStateFlow()

    private val _deleteState =
        MutableStateFlow<LoadableViewState<Unit>>(LoadableViewState.Initial)
    val deleteState = _deleteState.asStateFlow()

    init {
        _email.value = accountStorage.state.value?.email.orEmpty()
    }

    init {
        viewModelScope.launch {
            _state.emit(LoadableViewState.Loading)

            val poolResult =
                getPoolGamblerScore.execute(poolId = poolId, gamblerId = gamblerId)
            poolResult.onSuccess { poolGamblerScore ->
                _state.emit(LoadableViewState.Success(poolGamblerScore.toPoolGamblerScoreModel()))
            }.onFailure { exception ->
                _state.emit(LoadableViewState.Failure(exception.orDefaultLocalized()))
            }
        }
    }

    init {
        viewModelScope.launch {
            getPool.execute(poolId = poolId).onSuccess { pool ->
                _isOwner.value = pool.creatorGamblerId == gamblerId
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logOut.execute()
        }
    }

    fun deletePool(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _deleteState.emit(LoadableViewState.Loading)

            deletePool.execute(poolId = poolId, gamblerId = gamblerId)
                .onSuccess {
                    _deleteState.emit(LoadableViewState.Success(Unit))
                    onSuccess()
                }
                .onFailure { exception ->
                    _deleteState.emit(LoadableViewState.Failure(exception.orDefaultLocalized()))
                }
        }
    }

    fun resetDeleteState() {
        _deleteState.value = LoadableViewState.Initial
    }
}
