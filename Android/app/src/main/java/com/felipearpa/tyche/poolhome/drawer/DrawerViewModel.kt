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
import com.felipearpa.ui.state.LoadState
import com.felipearpa.ui.state.SaveState
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
        MutableStateFlow<LoadState<PoolGamblerScoreModel>>(LoadState.Idle)
    val state = _state.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _isOwner = MutableStateFlow(false)
    val isOwner: StateFlow<Boolean> = _isOwner.asStateFlow()

    private val _deleteState =
        MutableStateFlow<SaveState<Unit>>(SaveState.Idle)
    val deleteState = _deleteState.asStateFlow()

    init {
        val bundle = accountStorage.state.value
        _email.value = bundle?.email.orEmpty()
        _username.value = bundle?.username.orEmpty()
    }

    init {
        viewModelScope.launch {
            _state.emit(LoadState.Loading)

            val poolResult =
                getPoolGamblerScore.execute(poolId = poolId, gamblerId = gamblerId)
            poolResult.onSuccess { poolGamblerScore ->
                _state.emit(LoadState.Loaded(poolGamblerScore.toPoolGamblerScoreModel()))
            }.onFailure { exception ->
                _state.emit(LoadState.Failure(exception.orDefaultLocalized()))
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
            _deleteState.emit(SaveState.Saving(Unit))

            deletePool.execute(poolId = poolId, gamblerId = gamblerId)
                .onSuccess {
                    _deleteState.emit(SaveState.Saved(Unit))
                    onSuccess()
                }
                .onFailure { exception ->
                    _deleteState.emit(SaveState.Failure(Unit, exception.orDefaultLocalized()))
                }
        }
    }

    fun resetDeleteState() {
        _deleteState.value = SaveState.Idle
    }

    fun applyUsername(newUsername: String) {
        _username.value = newUsername
    }
}
